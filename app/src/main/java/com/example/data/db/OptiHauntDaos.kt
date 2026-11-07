package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.GeneratedImageEntity
import com.example.data.model.InfluenceMarkEntity
import com.example.data.model.InstructionVersionEntity
import com.example.data.model.ProjectSessionEntity
import com.example.data.model.PromptEntity
import com.example.data.model.ThemeSteerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions ORDER BY updatedAt DESC")
    fun getAllSessions(): Flow<List<ProjectSessionEntity>>

    @Query("SELECT * FROM sessions WHERE id = :sessionId LIMIT 1")
    fun getSessionFlow(sessionId: String): Flow<ProjectSessionEntity?>

    @Query("SELECT * FROM sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getSession(sessionId: String): ProjectSessionEntity?

    @Query("SELECT * FROM sessions ORDER BY updatedAt DESC LIMIT 1")
    suspend fun getLatestSession(): ProjectSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(session: ProjectSessionEntity)

    @Update
    suspend fun update(session: ProjectSessionEntity)

    @Query("DELETE FROM sessions WHERE id = :sessionId")
    suspend fun deleteSession(sessionId: String)
}

@Dao
interface InstructionVersionDao {
    @Query("SELECT * FROM instruction_versions WHERE sessionId = :sessionId ORDER BY versionNumber ASC")
    fun getVersionsFlow(sessionId: String): Flow<List<InstructionVersionEntity>>

    @Query("SELECT * FROM instruction_versions WHERE sessionId = :sessionId ORDER BY versionNumber ASC")
    suspend fun getVersions(sessionId: String): List<InstructionVersionEntity>

    @Query("SELECT * FROM instruction_versions WHERE sessionId = :sessionId AND versionNumber = :ver LIMIT 1")
    suspend fun getVersion(sessionId: String, ver: Int): InstructionVersionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(version: InstructionVersionEntity)
}

@Dao
interface PromptDao {
    @Query("SELECT * FROM prompts WHERE sessionId = :sessionId ORDER BY createdAt ASC")
    fun getPromptsFlow(sessionId: String): Flow<List<PromptEntity>>

    @Query("SELECT * FROM prompts WHERE sessionId = :sessionId ORDER BY createdAt ASC")
    suspend fun getPrompts(sessionId: String): List<PromptEntity>

    @Query("SELECT * FROM prompts WHERE id = :id LIMIT 1")
    suspend fun getPrompt(id: String): PromptEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(prompts: List<PromptEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(prompt: PromptEntity)

    @Update
    suspend fun update(prompt: PromptEntity)

    @Query("DELETE FROM prompts WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM prompts WHERE sessionId = :sessionId AND used = 0 AND inFlightClaim = 0")
    suspend fun clearUnusedQueue(sessionId: String)

    @Query("DELETE FROM prompts WHERE sessionId = :sessionId AND used = 0 AND inFlightClaim = 0 AND influenced = 0 AND origin != 'manual'")
    suspend fun clearNonSteeredQueue(sessionId: String)

    @Query("UPDATE prompts SET stale = 1 WHERE sessionId = :sessionId AND version < :newVersion AND origin != 'manual' AND influenced = 0")
    suspend fun markStaleExceptManualAndSteered(sessionId: String, newVersion: Int)
}

@Dao
interface ImageDao {
    @Query("SELECT * FROM generated_images WHERE sessionId = :sessionId ORDER BY startedAt DESC")
    fun getImagesFlow(sessionId: String): Flow<List<GeneratedImageEntity>>

    @Query("SELECT * FROM generated_images WHERE sessionId = :sessionId ORDER BY startedAt DESC")
    suspend fun getImages(sessionId: String): List<GeneratedImageEntity>

    @Query("SELECT * FROM generated_images WHERE isSaved = 1 ORDER BY completedAt DESC")
    fun getAllSavedImagesFlow(): Flow<List<GeneratedImageEntity>>

    @Query("SELECT * FROM generated_images ORDER BY completedAt DESC LIMIT 200")
    fun getCachedImagesFlow(): Flow<List<GeneratedImageEntity>>

    @Query("SELECT * FROM generated_images WHERE sessionId = :sessionId AND (isUpvoted = 1 OR influenced = 1) ORDER BY completedAt DESC")
    fun getPickedImagesFlow(sessionId: String): Flow<List<GeneratedImageEntity>>

    @Query("SELECT * FROM generated_images WHERE id = :id LIMIT 1")
    suspend fun getImage(id: String): GeneratedImageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(image: GeneratedImageEntity)

    @Update
    suspend fun update(image: GeneratedImageEntity)

    @Query("DELETE FROM generated_images WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface InfluenceDao {
    @Query("SELECT * FROM influence_marks WHERE sessionId = :sessionId AND isCommitted = 0 ORDER BY markedAt ASC")
    fun getPendingMarksFlow(sessionId: String): Flow<List<InfluenceMarkEntity>>

    @Query("SELECT * FROM influence_marks WHERE sessionId = :sessionId AND isCommitted = 0 ORDER BY markedAt ASC")
    suspend fun getPendingMarks(sessionId: String): List<InfluenceMarkEntity>

    @Query("SELECT * FROM influence_marks WHERE sessionId = :sessionId ORDER BY markedAt ASC")
    suspend fun getAllMarks(sessionId: String): List<InfluenceMarkEntity>

    @Query("SELECT * FROM influence_marks WHERE sessionId = :sessionId AND promptId = :promptId LIMIT 1")
    suspend fun getMarkForPrompt(sessionId: String, promptId: String): InfluenceMarkEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(mark: InfluenceMarkEntity)

    @Query("DELETE FROM influence_marks WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM influence_marks WHERE sessionId = :sessionId AND promptId = :promptId")
    suspend fun deleteByPrompt(sessionId: String, promptId: String)

    @Query("UPDATE influence_marks SET isCommitted = 1, committedVersion = :version WHERE sessionId = :sessionId AND isCommitted = 0")
    suspend fun commitPendingMarks(sessionId: String, version: Int)
}

@Dao
interface SteerDao {
    @Query("SELECT * FROM theme_steers WHERE sessionId = :sessionId AND isConsumed = 0 ORDER BY orderIndex ASC")
    fun getActiveSteersFlow(sessionId: String): Flow<List<ThemeSteerEntity>>

    @Query("SELECT * FROM theme_steers WHERE sessionId = :sessionId AND isConsumed = 0 ORDER BY orderIndex ASC")
    suspend fun getActiveSteers(sessionId: String): List<ThemeSteerEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(steer: ThemeSteerEntity)

    @Update
    suspend fun update(steer: ThemeSteerEntity)

    @Query("DELETE FROM theme_steers WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM theme_steers WHERE sessionId = :sessionId")
    suspend fun clearSteerHistory(sessionId: String)
}
