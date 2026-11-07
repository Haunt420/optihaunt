package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

enum class Resolution(val id: String, val label: String, val icon: String, val aspectRatio: Float, val width: Int, val height: Int) {
    SQUARE("square", "Square", "⬜", 1f, 768, 768),
    TURBO("turbo", "Turbo", "⚡", 1f, 512, 512),
    PORTRAIT("portrait", "Portrait", "🖼️", 0.667f, 512, 768),
    LANDSCAPE("landscape", "Landscape", "🌄", 1.5f, 768, 512);

    companion object {
        fun fromId(id: String): Resolution = entries.find { it.id == id } ?: SQUARE
    }
}

enum class CreativityLevel(val level: Int, val title: String, val description: String) {
    FAITHFUL(0, "Faithful", "Stays on your request: free within it, but on-topic"),
    BALANCED(1, "Balanced", "Fresh angles and varied mood, lighting & style"),
    CREATIVE(2, "Creative", "Reinterprets and remixes freely, including bold departures");

    companion object {
        fun fromLevel(level: Int): CreativityLevel = entries.find { it.level == level } ?: BALANCED
    }
}

enum class AiHelpMode(val id: String, val title: String, val hint: String, val rule: String) {
    KEEP("keep", "Keep structure", "Refines details while keeping your current structure",
        "Keep the overall structure, headings, and format of the current instruction intact while incorporating the feedback."),
    FREE("free", "Freely restructure", "Reorganizes into a fresh layout or composition",
        "Freely reorganize, rephrase, or restructure the instruction for the best artistic results."),
    TIGHTEN("tighten", "Tighten", "Condenses wording and trims excess",
        "Make the instruction crisper, more concise, and remove redundant fluff."),
    EXPAND("expand", "Expand", "Enriches with vivid sensory details",
        "Add vivid, rich, evocative sensory details and nuanced artistic descriptions.");

    companion object {
        fun fromId(id: String): AiHelpMode = entries.find { it.id == id } ?: KEEP
    }
}

@Entity(tableName = "sessions")
data class ProjectSessionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String = "Untitled Session",
    val currentInstruction: String = "",
    val currentVersion: Int = 1,
    val pendingDraft: String? = null,
    val pendingWhy: String? = null,
    val isPaused: Boolean = false,
    val pausedPrompts: Boolean = false,
    val pausedImages: Boolean = false,
    val creativity: Int = 1,
    val allowMix: Boolean = true,
    val promoteUpvoted: Boolean = false,
    val freshOnly: Boolean = false,
    val prioritizeSteered: Boolean = true,
    val resolution: String = Resolution.SQUARE.id,
    val promptPrefix: String = "",
    val promptSuffix: String = "",
    val batchRule: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "instruction_versions",
    foreignKeys = [
        ForeignKey(
            entity = ProjectSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sessionId"), Index(value = ["sessionId", "versionNumber"], unique = true)]
)
data class InstructionVersionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val sessionId: String,
    val versionNumber: Int,
    val text: String,
    val whyNote: String? = null,
    val appliedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "prompts",
    foreignKeys = [
        ForeignKey(
            entity = ProjectSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sessionId"), Index("version"), Index("readyAt")]
)
data class PromptEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val sessionId: String,
    val text: String,
    val used: Int = 0,
    val votes: Int = 0,
    val score: Float = 0f,
    val createdAt: Long = System.currentTimeMillis(),
    val readyAt: Long = System.currentTimeMillis(), // 10s cooldown target
    val version: Int = 1,
    val stale: Boolean = false,
    val origin: String = "generated", // "generated", "manual", "creative", "promoted", "mixed", "steered"
    val influenced: Boolean = false, // Active steer flag: true until first image
    val influenceText: String? = null,
    val influenceInherited: String? = null,
    val parentPromptId: String? = null,
    val parentVersion: Int? = null,
    val drewFromIds: String? = null, // CSV
    val batchRule: String? = null,
    val inFlightClaim: Int = 0
)

@Entity(
    tableName = "generated_images",
    foreignKeys = [
        ForeignKey(
            entity = ProjectSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sessionId"), Index("promptId"), Index("isUpvoted"), Index("isSaved")]
)
data class GeneratedImageEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val sessionId: String,
    val promptId: String,
    val imageUrl: String,
    val seed: Long = (100000L..999999999L).random(),
    val resolution: String = Resolution.SQUARE.id,
    val resolvedText: String = "",
    val state: String = "done", // "running", "done", "error"
    val errorMessage: String? = null,
    val startedAt: Long = System.currentTimeMillis(),
    val completedAt: Long = System.currentTimeMillis(),
    val isUpvoted: Boolean = false,
    val isSaved: Boolean = false,
    val influenced: Boolean = false,
    val influenceText: String? = null,
    val versionNumber: Int = 1
)

@Entity(
    tableName = "influence_marks",
    foreignKeys = [
        ForeignKey(
            entity = ProjectSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sessionId"), Index("isCommitted")]
)
data class InfluenceMarkEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val sessionId: String,
    val taskId: String? = null,
    val promptId: String? = null,
    val text: String,
    val isAnti: Boolean = false, // false = 🔖 positive, true = 🚫 anti
    val isCommitted: Boolean = false,
    val committedVersion: Int? = null,
    val markedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "theme_steers",
    foreignKeys = [
        ForeignKey(
            entity = ProjectSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sessionId"), Index("isConsumed")]
)
data class ThemeSteerEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val sessionId: String,
    val text: String,
    val isConsumed: Boolean = false,
    val orderIndex: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
