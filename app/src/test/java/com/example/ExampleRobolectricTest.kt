package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.OptiHauntDatabase
import com.example.data.engine.CurlyChoiceEvaluator
import com.example.data.model.GeneratedImageEntity
import com.example.data.model.InfluenceMarkEntity
import com.example.data.model.InstructionVersionEntity
import com.example.data.model.ProjectSessionEntity
import com.example.data.model.PromptEntity
import com.example.data.model.ThemeSteerEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var database: OptiHauntDatabase

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, OptiHauntDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `read string from context matches OptiHaunt`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("OptiHaunt", appName)
    }

    @Test
    fun `curly choice evaluator rolls alternatives and validates balanced braces`() {
        val input = "A shot at {golden hour|blue hour}"
        val rolled = CurlyChoiceEvaluator.rollCurlyChoices(input)
        assertTrue(rolled == "A shot at golden hour" || rolled == "A shot at blue hour")

        assertTrue(CurlyChoiceEvaluator.isValidCurlyBlocks("{a|b}"))
        assertTrue(CurlyChoiceEvaluator.isValidCurlyBlocks("Plain text"))
        assertFalse(CurlyChoiceEvaluator.isValidCurlyBlocks("{unclosed block"))
        assertFalse(CurlyChoiceEvaluator.isValidCurlyBlocks("{nested {inner}|outer}"))
    }

    @Test
    fun `scoring formula computes votes divided by used count`() = runBlocking {
        val sessionId = UUID.randomUUID().toString()
        val session = ProjectSessionEntity(id = sessionId, name = "Test Session")
        database.sessionDao().insertOrUpdate(session)

        val prompt = PromptEntity(
            id = "prompt-1",
            sessionId = sessionId,
            text = "cyberpunk alleyway",
            used = 4,
            votes = 3,
            score = 3f / 4f
        )
        database.promptDao().insert(prompt)

        val retrieved = database.promptDao().getPrompt("prompt-1")
        assertNotNull(retrieved)
        assertEquals(0.75f, retrieved!!.score, 0.001f)
    }

    @Test
    fun `positive and anti influence marks remain strictly separate`() = runBlocking {
        val sessionId = UUID.randomUUID().toString()
        val session = ProjectSessionEntity(id = sessionId, name = "Marks Test")
        database.sessionDao().insertOrUpdate(session)

        val posMark = InfluenceMarkEntity(
            sessionId = sessionId,
            promptId = "p1",
            text = "vivid warm lighting",
            isAnti = false
        )
        val antiMark = InfluenceMarkEntity(
            sessionId = sessionId,
            promptId = "p2",
            text = "distorted anatomy",
            isAnti = true
        )

        database.influenceDao().insert(posMark)
        database.influenceDao().insert(antiMark)

        val pending = database.influenceDao().getPendingMarks(sessionId)
        assertEquals(2, pending.size)
        assertEquals(1, pending.count { !it.isAnti })
        assertEquals(1, pending.count { it.isAnti })

        // Committing marks updates committedVersion
        database.influenceDao().commitPendingMarks(sessionId, 2)
        val afterCommit = database.influenceDao().getPendingMarks(sessionId)
        assertEquals(0, afterCommit.size)
    }

    @Test
    fun `applying instruction bumps version and preserves lineage`() = runBlocking {
        val sessionId = UUID.randomUUID().toString()
        val session = ProjectSessionEntity(id = sessionId, currentVersion = 1, currentInstruction = "cats")
        database.sessionDao().insertOrUpdate(session)

        database.versionDao().insert(
            InstructionVersionEntity(sessionId = sessionId, versionNumber = 1, text = "cats")
        )

        // Add version 2
        database.versionDao().insert(
            InstructionVersionEntity(sessionId = sessionId, versionNumber = 2, text = "cyberpunk cats", whyNote = "added neon")
        )

        val versions = database.versionDao().getVersions(sessionId)
        assertEquals(2, versions.size)
        assertEquals("cats", versions[0].text)
        assertEquals("cyberpunk cats", versions[1].text)
        assertEquals("added neon", versions[1].whyNote)
    }

    @Test
    fun `steer entity active and consumed lifecycle`() = runBlocking {
        val sessionId = UUID.randomUUID().toString()
        val session = ProjectSessionEntity(id = sessionId)
        database.sessionDao().insertOrUpdate(session)

        val steer = ThemeSteerEntity(sessionId = sessionId, text = "watercolor", isConsumed = false)
        database.steerDao().insert(steer)

        val active = database.steerDao().getActiveSteers(sessionId)
        assertEquals(1, active.size)
        assertEquals("watercolor", active[0].text)

        database.steerDao().update(active[0].copy(isConsumed = true))
        val afterConsumed = database.steerDao().getActiveSteers(sessionId)
        assertEquals(0, afterConsumed.size)
    }

    @Test
    fun `copper hunter oled theme uses true oled black background, deep copper and hunter green with gold fleck`() {
        val oledScheme = com.example.ui.theme.CopperHunterOledColorScheme
        // Main bg is true OLED Black
        assertEquals(com.example.ui.theme.OledBlack, oledScheme.background)
        // Primary is deep copper
        assertEquals(com.example.ui.theme.DeepCopperLight, oledScheme.primary)
        // Secondary is bright silver chrome
        assertEquals(com.example.ui.theme.BrightChrome, oledScheme.secondary)
        // Tertiary is gold fleck
        assertEquals(com.example.ui.theme.GoldFleck, oledScheme.tertiary)
        // Tertiary container is dark hunter green
        assertEquals(com.example.ui.theme.DarkHunterGreen, oledScheme.tertiaryContainer)
    }

    @Test
    fun `app theme selection and fallback`() {
        assertEquals(com.example.ui.theme.AppTheme.COPPER_HUNTER_OLED, com.example.ui.theme.AppTheme.fromId("copper_hunter_oled"))
        assertEquals(com.example.ui.theme.AppTheme.SPECTRAL_VIOLET, com.example.ui.theme.AppTheme.fromId("spectral_violet"))
        assertEquals(com.example.ui.theme.AppTheme.CYBER_NEON, com.example.ui.theme.AppTheme.fromId("cyber_neon"))
        assertEquals(com.example.ui.theme.AppTheme.STUDIO_LIGHT, com.example.ui.theme.AppTheme.fromId("studio_light"))
        // Unknown id defaults to COPPER_HUNTER_OLED
        assertEquals(com.example.ui.theme.AppTheme.COPPER_HUNTER_OLED, com.example.ui.theme.AppTheme.fromId("non_existent"))
    }
}
