package com.example.data.engine

import android.util.Log
import com.example.data.db.OptiHauntDatabase
import com.example.data.model.CreativityLevel
import com.example.data.model.GeneratedImageEntity
import com.example.data.model.InfluenceMarkEntity
import com.example.data.model.InstructionVersionEntity
import com.example.data.model.ProjectSessionEntity
import com.example.data.model.PromptEntity
import com.example.data.model.Resolution
import com.example.data.model.ThemeSteerEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import kotlin.random.Random

class PipelineScheduler(
    private val db: OptiHauntDatabase,
    private val scope: CoroutineScope
) {
    private val promptEngine = PromptBrainstormEngine()
    private val imageEngine = ImageGenerationEngine()

    private val _statusMessage = MutableStateFlow("Ready")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private val _editingPromptId = MutableStateFlow<String?>(null)
    val editingPromptId: StateFlow<String?> = _editingPromptId.asStateFlow()

    private val _inflightTasksCount = MutableStateFlow(0)
    val inflightTasksCount: StateFlow<Int> = _inflightTasksCount.asStateFlow()

    private val mutex = Mutex()
    private var schedulerJob: Job? = null
    private var isWritingPrompts = false

    fun start(sessionId: String) {
        schedulerJob?.cancel()
        schedulerJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                tick(sessionId)
                delay(1000)
            }
        }
    }

    fun stop() {
        schedulerJob?.cancel()
        schedulerJob = null
    }

    fun setEditingPrompt(promptId: String?) {
        _editingPromptId.value = promptId
    }

    private suspend fun tick(sessionId: String) {
        val session = db.sessionDao().getSession(sessionId) ?: return
        if (session.currentInstruction.isBlank()) {
            _statusMessage.value = "Type what you want to see first"
            return
        }

        // Full pause check
        if (session.isPaused) {
            _statusMessage.value = "⏸ All paused"
            return
        }

        // Editing pause check
        if (_editingPromptId.value != null) {
            _statusMessage.value = "✍️ Paused while you edit"
            return
        }

        // 1. Maintain prompt stock in upcoming queue
        if (!session.pausedPrompts && !isWritingPrompts) {
            val allPrompts = db.promptDao().getPrompts(sessionId)
            val upcomingPrompts = allPrompts.filter { it.used == 0 && !it.stale && it.version == session.currentVersion }

            if (upcomingPrompts.size < 5) {
                scope.launch(Dispatchers.IO) {
                    writePromptBatch(session)
                }
            }
        }

        // 2. Dispatch image jobs
        if (!session.pausedImages && _inflightTasksCount.value < 2) {
            dispatchNextImage(session)
        }

        updateStatus(session)
    }

    private suspend fun writePromptBatch(session: ProjectSessionEntity) {
        if (isWritingPrompts) return
        isWritingPrompts = true
        _statusMessage.value = "⏳ Brainstorming new prompts…"

        try {
            val activeSteers = db.steerDao().getActiveSteers(session.id)
            val steerToConsume = activeSteers.firstOrNull()

            val likedPrompts = if (session.allowMix) {
                db.promptDao().getPrompts(session.id).filter { it.votes > 0 }
            } else {
                db.promptDao().getPrompts(session.id).filter { it.votes > 0 && it.version == session.currentVersion }
            }

            // Check Promote Upvoted: ~3 in 5 batches when enabled and liked prompts exist
            val promotedPrompt = if (session.promoteUpvoted && likedPrompts.isNotEmpty() && Random.nextFloat() < 0.6f) {
                likedPrompts.maxByOrNull { it.score }
            } else null

            val effectiveInstruction = promotedPrompt?.text ?: session.currentInstruction

            val newPromptTexts = promptEngine.brainstormBatch(
                instruction = session.currentInstruction,
                effectiveInstruction = effectiveInstruction,
                creativity = CreativityLevel.fromLevel(session.creativity),
                steerText = steerToConsume?.text,
                batchRule = session.batchRule.ifBlank { null },
                likedExamples = likedPrompts,
                batchCount = 5
            )

            if (newPromptTexts.isNotEmpty()) {
                val now = System.currentTimeMillis()
                val origin = when {
                    promotedPrompt != null -> "promoted"
                    session.creativity == 2 -> "creative"
                    else -> "generated"
                }

                val entities = newPromptTexts.map { text ->
                    PromptEntity(
                        id = UUID.randomUUID().toString(),
                        sessionId = session.id,
                        text = text,
                        version = session.currentVersion,
                        origin = origin,
                        readyAt = now + 10_000L, // 10s cooldown
                        influenced = steerToConsume != null,
                        influenceText = steerToConsume?.text,
                        parentPromptId = promotedPrompt?.id,
                        parentVersion = session.currentVersion,
                        batchRule = session.batchRule.ifBlank { null }
                    )
                }

                db.promptDao().insertAll(entities)

                // Consume steer only upon successful batch creation
                if (steerToConsume != null) {
                    db.steerDao().update(steerToConsume.copy(isConsumed = true))
                }
            }
        } catch (e: Exception) {
            Log.e("PipelineScheduler", "Failed to write prompt batch", e)
        } finally {
            isWritingPrompts = false
        }
    }

    private suspend fun dispatchNextImage(session: ProjectSessionEntity) = mutex.withLock {
        val now = System.currentTimeMillis()
        val allPrompts = db.promptDao().getPrompts(session.id)

        // Eligible prompts:
        // Must be ready (cooldown finished <= now)
        // Must not be currently edited
        // Must match version if freshOnly is enabled
        var available = allPrompts.filter { prompt ->
            now >= prompt.readyAt && prompt.id != _editingPromptId.value && prompt.inFlightClaim == 0
        }

        if (session.freshOnly) {
            available = available.filter { it.used == 0 && it.version == session.currentVersion && !it.stale }
        }

        if (available.isEmpty()) return@withLock

        // Prioritize manual prompts for their first use!
        val manualFirstUse = available.filter { it.origin == "manual" && it.used == 0 }
        if (manualFirstUse.isNotEmpty()) {
            available = manualFirstUse
        } else if (session.prioritizeSteered) {
            val steeredFirst = available.filter { it.influenced && it.used == 0 }
            if (steeredFirst.isNotEmpty()) {
                available = steeredFirst
            }
        }

        // Selection: weighted between high scored and untested exploration
        val scored = available.filter { it.used > 0 && it.score > 0f }
        val untested = available.filter { it.used == 0 }

        val pick = when {
            // Exploitation: re-use well-liked prompts 60% of the time if available
            scored.isNotEmpty() && Random.nextFloat() < 0.6f -> {
                scored.maxByOrNull { it.score } ?: scored.random()
            }
            untested.isNotEmpty() -> {
                untested.random()
            }
            else -> available.random()
        }

        // Claim prompt
        db.promptDao().update(pick.copy(inFlightClaim = 1))
        _inflightTasksCount.value += 1

        val resolution = Resolution.fromId(session.resolution)
        val imageId = UUID.randomUUID().toString()

        scope.launch(Dispatchers.IO) {
            try {
                val result = imageEngine.generateImage(
                    promptText = pick.text,
                    prefix = session.promptPrefix,
                    suffix = session.promptSuffix,
                    resolution = resolution
                )

                // Update prompt use and retire influenced flag
                val updatedUsed = pick.used + 1
                val newScore = if (updatedUsed > 0) pick.votes.toFloat() / updatedUsed.toFloat() else 0f
                db.promptDao().update(
                    pick.copy(
                        used = updatedUsed,
                        score = newScore,
                        influenced = false, // retired upon first image production
                        inFlightClaim = 0
                    )
                )

                // Save generated image
                val imageEntity = GeneratedImageEntity(
                    id = imageId,
                    sessionId = session.id,
                    promptId = pick.id,
                    imageUrl = result.imageUrl,
                    seed = result.seed,
                    resolution = resolution.id,
                    resolvedText = result.resolvedText,
                    state = "done",
                    startedAt = now,
                    completedAt = System.currentTimeMillis(),
                    influenced = pick.influenced,
                    influenceText = pick.influenceText,
                    versionNumber = session.currentVersion
                )
                db.imageDao().insert(imageEntity)
            } catch (e: Exception) {
                Log.e("PipelineScheduler", "Image generation failed for prompt: ${pick.id}", e)
                // Release claim on failure
                db.promptDao().update(pick.copy(inFlightClaim = 0))
            } finally {
                _inflightTasksCount.value = maxOf(0, _inflightTasksCount.value - 1)
            }
        }
    }

    private suspend fun updateStatus(session: ProjectSessionEntity) {
        val now = System.currentTimeMillis()
        val all = db.promptDao().getPrompts(session.id)
        val cooling = all.filter { it.used == 0 && now < it.readyAt }
        val ready = all.filter { it.used == 0 && now >= it.readyAt }

        _statusMessage.value = when {
            session.isPaused -> "⏸ Paused"
            session.pausedImages -> "✍️ Writing prompts (images paused)"
            session.pausedPrompts -> "🖼️ Drawing images (prompts paused)"
            _inflightTasksCount.value > 0 -> "⚡ Drawing (${_inflightTasksCount.value} in flight)"
            cooling.isNotEmpty() && ready.isEmpty() -> "⏳ Prompts cooling (${cooling.size})"
            ready.isNotEmpty() -> "Ready (${ready.size} upcoming)"
            else -> "Generating prompts…"
        }
    }

    suspend fun toggleUpvote(imageId: String) {
        val image = db.imageDao().getImage(imageId) ?: return
        val newUpvoted = !image.isUpvoted
        db.imageDao().update(image.copy(isUpvoted = newUpvoted))

        val prompt = db.promptDao().getPrompt(image.promptId) ?: return
        val newVotes = maxOf(0, prompt.votes + if (newUpvoted) 1 else -1)
        val newScore = if (prompt.used > 0) newVotes.toFloat() / prompt.used.toFloat() else 0f
        db.promptDao().update(prompt.copy(votes = newVotes, score = newScore))
    }

    suspend fun toggleSaveImage(imageId: String) {
        val image = db.imageDao().getImage(imageId) ?: return
        db.imageDao().update(image.copy(isSaved = !image.isSaved))
    }

    suspend fun toggleMarkInfluence(sessionId: String, taskId: String, promptId: String, text: String, isAnti: Boolean) {
        val existing = db.influenceDao().getMarkForPrompt(sessionId, promptId)
        if (existing != null && existing.isAnti == isAnti) {
            // Remove mark
            db.influenceDao().delete(existing.id)
        } else if (existing != null) {
            // Switch positive <-> anti
            db.influenceDao().delete(existing.id)
            db.influenceDao().insert(
                InfluenceMarkEntity(
                    sessionId = sessionId,
                    taskId = taskId,
                    promptId = promptId,
                    text = text,
                    isAnti = isAnti
                )
            )
        } else {
            // Add mark
            db.influenceDao().insert(
                InfluenceMarkEntity(
                    sessionId = sessionId,
                    taskId = taskId,
                    promptId = promptId,
                    text = text,
                    isAnti = isAnti
                )
            )
        }
    }

    suspend fun applyInstructionChange(sessionId: String, newInstruction: String, whyNote: String? = null) {
        val session = db.sessionDao().getSession(sessionId) ?: return
        val nextVersion = session.currentVersion + 1

        // Commit instruction version
        db.versionDao().insert(
            InstructionVersionEntity(
                sessionId = sessionId,
                versionNumber = nextVersion,
                text = newInstruction.trim(),
                whyNote = whyNote?.trim()?.ifBlank { null }
            )
        )

        // Commit pending marks
        db.influenceDao().commitPendingMarks(sessionId, nextVersion)

        // Mark older prompts stale except manual and active steered prompts
        db.promptDao().markStaleExceptManualAndSteered(sessionId, nextVersion)

        // Update session
        db.sessionDao().update(
            session.copy(
                currentInstruction = newInstruction.trim(),
                currentVersion = nextVersion,
                pendingDraft = null,
                pendingWhy = null,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun addManualPrompt(sessionId: String, text: String) {
        val lines = text.lines()
            .map { it.trim().removePrefix("- ").removePrefix("* ") }
            .filter { it.isNotBlank() }

        val session = db.sessionDao().getSession(sessionId) ?: return
        val now = System.currentTimeMillis()

        val entities = lines.map { promptText ->
            PromptEntity(
                sessionId = sessionId,
                text = promptText,
                version = session.currentVersion,
                origin = "manual",
                readyAt = now + 10_000L // 10s cooldown
            )
        }
        db.promptDao().insertAll(entities)
    }

    suspend fun addThemeSteer(sessionId: String, text: String) {
        val clean = text.trim()
        if (clean.isBlank()) return
        val active = db.steerDao().getActiveSteers(sessionId)
        if (active.any { it.text.equals(clean, ignoreCase = true) }) return // Duplicate prevention

        db.steerDao().insert(
            ThemeSteerEntity(
                sessionId = sessionId,
                text = clean,
                orderIndex = active.size
            )
        )
    }
}
