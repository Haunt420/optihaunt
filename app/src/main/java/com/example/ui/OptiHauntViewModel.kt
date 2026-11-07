package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.OptiHauntDatabase
import com.example.data.engine.AiHelpEngine
import com.example.data.engine.PipelineScheduler
import com.example.data.model.AiHelpMode
import com.example.data.model.CreativityLevel
import com.example.data.model.GeneratedImageEntity
import com.example.data.model.InfluenceMarkEntity
import com.example.data.model.InstructionVersionEntity
import com.example.data.model.ProjectSessionEntity
import com.example.data.model.PromptEntity
import com.example.data.model.Resolution
import com.example.data.model.ThemeSteerEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class OptiHauntViewModel(application: Application) : AndroidViewModel(application) {

    private val db = OptiHauntDatabase.getDatabase(application)
    val scheduler = PipelineScheduler(db, viewModelScope)
    private val aiHelpEngine = AiHelpEngine()

    private val _currentSessionId = MutableStateFlow<String?>(null)
    val currentSessionId: StateFlow<String?> = _currentSessionId.asStateFlow()

    val currentSession: StateFlow<ProjectSessionEntity?> = _currentSessionId.flatMapLatest { id ->
        if (id != null) db.sessionDao().getSessionFlow(id) else flowOf(null)
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    val allSessions: StateFlow<List<ProjectSessionEntity>> = db.sessionDao().getAllSessions()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val feedImages: StateFlow<List<GeneratedImageEntity>> = _currentSessionId.flatMapLatest { id ->
        if (id != null) db.imageDao().getImagesFlow(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val allPrompts: StateFlow<List<PromptEntity>> = _currentSessionId.flatMapLatest { id ->
        if (id != null) db.promptDao().getPromptsFlow(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val activeSteers: StateFlow<List<ThemeSteerEntity>> = _currentSessionId.flatMapLatest { id ->
        if (id != null) db.steerDao().getActiveSteersFlow(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val pendingMarks: StateFlow<List<InfluenceMarkEntity>> = _currentSessionId.flatMapLatest { id ->
        if (id != null) db.influenceDao().getPendingMarksFlow(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val instructionVersions: StateFlow<List<InstructionVersionEntity>> = _currentSessionId.flatMapLatest { id ->
        if (id != null) db.versionDao().getVersionsFlow(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val savedImages: StateFlow<List<GeneratedImageEntity>> = db.imageDao().getAllSavedImagesFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val cachedImages: StateFlow<List<GeneratedImageEntity>> = db.imageDao().getCachedImagesFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val pickedImages: StateFlow<List<GeneratedImageEntity>> = _currentSessionId.flatMapLatest { id ->
        if (id != null) db.imageDao().getPickedImagesFlow(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val statusMessage = scheduler.statusMessage
    val editingPromptId = scheduler.editingPromptId

    // AI Help Suggestion State
    private val _aiSuggestionText = MutableStateFlow("")
    val aiSuggestionText: StateFlow<String> = _aiSuggestionText.asStateFlow()

    private val _aiSuggestionState = MutableStateFlow("idle") // idle, writing, done, error
    val aiSuggestionState: StateFlow<String> = _aiSuggestionState.asStateFlow()

    // UI Theme Selection
    private val prefs = application.getSharedPreferences("optihaunt_theme_prefs", android.content.Context.MODE_PRIVATE)
    private val _currentTheme = MutableStateFlow(
        com.example.ui.theme.AppTheme.fromId(
            prefs.getString("selected_theme", com.example.ui.theme.AppTheme.COPPER_HUNTER_OLED.id)
                ?: com.example.ui.theme.AppTheme.COPPER_HUNTER_OLED.id
        )
    )
    val currentTheme: StateFlow<com.example.ui.theme.AppTheme> = _currentTheme.asStateFlow()

    fun setTheme(theme: com.example.ui.theme.AppTheme) {
        _currentTheme.value = theme
        prefs.edit().putString("selected_theme", theme.id).apply()
    }

    init {
        initInitialSession()
    }

    private fun initInitialSession() {
        viewModelScope.launch(Dispatchers.IO) {
            val latest = db.sessionDao().getLatestSession()
            if (latest != null) {
                _currentSessionId.value = latest.id
                scheduler.start(latest.id)
            } else {
                val newSession = ProjectSessionEntity(
                    id = UUID.randomUUID().toString(),
                    name = "Session 1",
                    currentInstruction = "",
                    currentVersion = 1
                )
                db.sessionDao().insertOrUpdate(newSession)
                _currentSessionId.value = newSession.id
                scheduler.start(newSession.id)
            }
        }
    }

    fun startInstruction(text: String) {
        val sId = _currentSessionId.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val session = db.sessionDao().getSession(sId) ?: return@launch
            val updated = session.copy(
                currentInstruction = text.trim(),
                pendingDraft = null,
                isPaused = false,
                updatedAt = System.currentTimeMillis()
            )
            db.sessionDao().update(updated)

            // Insert initial version record if not exists
            if (db.versionDao().getVersions(sId).isEmpty()) {
                db.versionDao().insert(
                    InstructionVersionEntity(
                        sessionId = sId,
                        versionNumber = 1,
                        text = text.trim()
                    )
                )
            }
            scheduler.start(sId)
        }
    }

    fun updatePendingDraft(draftText: String) {
        val sId = _currentSessionId.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val session = db.sessionDao().getSession(sId) ?: return@launch
            db.sessionDao().update(session.copy(pendingDraft = draftText))
        }
    }

    fun updatePendingWhy(noteText: String) {
        val sId = _currentSessionId.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val session = db.sessionDao().getSession(sId) ?: return@launch
            db.sessionDao().update(session.copy(pendingWhy = noteText))
        }
    }

    fun applyInstructionChange(newText: String, whyNote: String? = null) {
        val sId = _currentSessionId.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            scheduler.applyInstructionChange(sId, newText, whyNote)
            _aiSuggestionText.value = ""
            _aiSuggestionState.value = "idle"
        }
    }

    fun discardPendingDraft() {
        val sId = _currentSessionId.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val session = db.sessionDao().getSession(sId) ?: return@launch
            db.sessionDao().update(session.copy(pendingDraft = null, pendingWhy = null))
        }
    }

    fun toggleMasterPause() {
        val sId = _currentSessionId.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val session = db.sessionDao().getSession(sId) ?: return@launch
            db.sessionDao().update(session.copy(isPaused = !session.isPaused))
        }
    }

    fun togglePausePrompts() {
        val sId = _currentSessionId.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val session = db.sessionDao().getSession(sId) ?: return@launch
            db.sessionDao().update(session.copy(pausedPrompts = !session.pausedPrompts))
        }
    }

    fun togglePauseImages() {
        val sId = _currentSessionId.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val session = db.sessionDao().getSession(sId) ?: return@launch
            db.sessionDao().update(session.copy(pausedImages = !session.pausedImages))
        }
    }

    fun setCreativity(level: CreativityLevel) {
        val sId = _currentSessionId.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val session = db.sessionDao().getSession(sId) ?: return@launch
            db.sessionDao().update(session.copy(creativity = level.level))
        }
    }

    fun toggleMixPrompts() {
        val sId = _currentSessionId.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val session = db.sessionDao().getSession(sId) ?: return@launch
            db.sessionDao().update(session.copy(allowMix = !session.allowMix))
        }
    }

    fun togglePromoteUpvoted() {
        val sId = _currentSessionId.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val session = db.sessionDao().getSession(sId) ?: return@launch
            db.sessionDao().update(session.copy(promoteUpvoted = !session.promoteUpvoted))
        }
    }

    fun toggleFreshOnly() {
        val sId = _currentSessionId.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val session = db.sessionDao().getSession(sId) ?: return@launch
            db.sessionDao().update(session.copy(freshOnly = !session.freshOnly))
        }
    }

    fun togglePrioritizeSteered() {
        val sId = _currentSessionId.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val session = db.sessionDao().getSession(sId) ?: return@launch
            db.sessionDao().update(session.copy(prioritizeSteered = !session.prioritizeSteered))
        }
    }

    fun setResolution(res: Resolution) {
        val sId = _currentSessionId.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val session = db.sessionDao().getSession(sId) ?: return@launch
            db.sessionDao().update(session.copy(resolution = res.id))
        }
    }

    fun updateRulesAndFixes(batchRule: String, prefix: String, suffix: String) {
        val sId = _currentSessionId.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val session = db.sessionDao().getSession(sId) ?: return@launch
            db.sessionDao().update(
                session.copy(
                    batchRule = batchRule,
                    promptPrefix = prefix,
                    promptSuffix = suffix
                )
            )
        }
    }

    fun addManualPrompt(text: String) {
        val sId = _currentSessionId.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            scheduler.addManualPrompt(sId, text)
        }
    }

    fun addThemeSteer(text: String) {
        val sId = _currentSessionId.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            scheduler.addThemeSteer(sId, text)
        }
    }

    fun removeThemeSteer(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            db.steerDao().delete(id)
        }
    }

    fun clearSteerHistory() {
        val sId = _currentSessionId.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            db.steerDao().clearSteerHistory(sId)
        }
    }

    fun clearUpcomingQueue() {
        val sId = _currentSessionId.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            db.promptDao().clearUnusedQueue(sId)
        }
    }

    fun clearNonSteeredQueue() {
        val sId = _currentSessionId.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            db.promptDao().clearNonSteeredQueue(sId)
        }
    }

    fun updatePromptText(promptId: String, newText: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val prompt = db.promptDao().getPrompt(promptId) ?: return@launch
            db.promptDao().update(prompt.copy(text = newText.trim()))
            scheduler.setEditingPrompt(null)
        }
    }

    fun deletePrompt(promptId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            db.promptDao().delete(promptId)
            if (scheduler.editingPromptId.value == promptId) {
                scheduler.setEditingPrompt(null)
            }
        }
    }

    fun drawPromptNext(promptText: String) {
        val sId = _currentSessionId.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            scheduler.addManualPrompt(sId, promptText)
        }
    }

    fun toggleUpvote(imageId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            scheduler.toggleUpvote(imageId)
        }
    }

    fun toggleSaveImage(imageId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            scheduler.toggleSaveImage(imageId)
        }
    }

    fun toggleMarkInfluence(image: GeneratedImageEntity, isAnti: Boolean) {
        val sId = _currentSessionId.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            scheduler.toggleMarkInfluence(
                sessionId = sId,
                taskId = image.id,
                promptId = image.promptId,
                text = image.resolvedText,
                isAnti = isAnti
            )
        }
    }

    fun requestAiHelp(mode: AiHelpMode) {
        val sId = _currentSessionId.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            _aiSuggestionState.value = "writing"
            try {
                val session = db.sessionDao().getSession(sId) ?: return@launch
                val marks = db.influenceDao().getPendingMarks(sId)
                val suggestion = aiHelpEngine.suggestInstruction(
                    currentInstruction = session.currentInstruction,
                    pendingDraft = session.pendingDraft,
                    userNote = session.pendingWhy,
                    marks = marks,
                    mode = mode
                )
                _aiSuggestionText.value = suggestion
                _aiSuggestionState.value = "done"
            } catch (e: Exception) {
                _aiSuggestionState.value = "error"
            }
        }
    }

    fun applyAiSuggestion() {
        val suggestion = _aiSuggestionText.value
        if (suggestion.isNotBlank()) {
            val note = currentSession.value?.pendingWhy
            applyInstructionChange(suggestion, note)
        }
    }

    fun useAiSuggestionAsDraft() {
        val suggestion = _aiSuggestionText.value
        if (suggestion.isNotBlank()) {
            updatePendingDraft(suggestion)
        }
    }

    fun switchSession(sessionId: String) {
        scheduler.stop()
        _currentSessionId.value = sessionId
        scheduler.start(sessionId)
    }

    fun createNewSession(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val newSession = ProjectSessionEntity(
                id = UUID.randomUUID().toString(),
                name = name.ifBlank { "Session ${System.currentTimeMillis() % 1000}" },
                currentInstruction = "",
                currentVersion = 1
            )
            db.sessionDao().insertOrUpdate(newSession)
            switchSession(newSession.id)
        }
    }

    fun spinOffTrimmedSession(name: String, keepVersions: Int) {
        val sId = _currentSessionId.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val current = db.sessionDao().getSession(sId) ?: return@launch
            val versions = db.versionDao().getVersions(sId)
            val versionsToKeep = versions.takeLast(maxOf(1, keepVersions))
            val minVersion = versionsToKeep.firstOrNull()?.versionNumber ?: 1

            val newId = UUID.randomUUID().toString()
            val newSession = current.copy(
                id = newId,
                name = name.ifBlank { "${current.name} (Trimmed)" },
                currentVersion = versionsToKeep.size,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            db.sessionDao().insertOrUpdate(newSession)

            // Copy versions
            versionsToKeep.forEachIndexed { index, v ->
                db.versionDao().insert(
                    v.copy(
                        id = UUID.randomUUID().toString(),
                        sessionId = newId,
                        versionNumber = index + 1
                    )
                )
            }

            // Copy prompts
            val prompts = db.promptDao().getPrompts(sId).filter { it.version >= minVersion }
            val newPrompts = prompts.map { p ->
                p.copy(
                    id = UUID.randomUUID().toString(),
                    sessionId = newId,
                    version = maxOf(1, p.version - minVersion + 1)
                )
            }
            db.promptDao().insertAll(newPrompts)

            // Switch to new trimmed session
            switchSession(newId)
        }
    }
}
