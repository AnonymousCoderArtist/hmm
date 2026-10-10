package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.cv.PoseBiometrics
import com.example.data.local.AppDatabase
import com.example.data.model.OverlayConfig
import com.example.data.model.PoseMode
import com.example.data.model.PushUpRep
import com.example.data.model.Sensitivity
import com.example.data.repository.ProjectRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: ProjectRepository

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        repository = ProjectRepository(db.projectDao())
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("PushUp Reel", appName)
    }

    @Test
    fun `biometrics state machine detects pushup reps`() {
        val biometrics = PoseBiometrics(poseMode = PoseMode.AUTO, sensitivity = Sensitivity.BALANCED)
        val samples = mutableListOf<PoseBiometrics.FrameMotionSample>()

        // Simulate 3 full pushup cycles (top -> down -> up)
        var timeMs = 0L
        for (rep in 1..3) {
            // Top lockout
            for (i in 0..4) {
                samples.add(PoseBiometrics.FrameMotionSample(timeMs, 0.25f, 2f, 0.5f))
                timeMs += 100L
            }
            // Going down
            for (i in 0..5) {
                samples.add(PoseBiometrics.FrameMotionSample(timeMs, 0.25f + (i * 0.10f), 15f, 0.6f))
                timeMs += 100L
            }
            // Bottom hold
            samples.add(PoseBiometrics.FrameMotionSample(timeMs, 0.75f, 5f, 0.7f))
            timeMs += 100L
            // Pushing up
            for (i in 0..5) {
                samples.add(PoseBiometrics.FrameMotionSample(timeMs, 0.75f - (i * 0.10f), 15f, 0.5f))
                timeMs += 100L
            }
            // Lockout
            samples.add(PoseBiometrics.FrameMotionSample(timeMs, 0.25f, 2f, 0.5f))
            timeMs += 100L
        }

        val detectedReps = biometrics.processTrajectory(samples)
        assertTrue("Expected at least 1 detected rep", detectedReps.isNotEmpty())
    }

    @Test
    fun `room database auto-save and retrieve project`() = runBlocking {
        val config = OverlayConfig(
            dayNumber = 29,
            targetPushups = 29,
            quoteText = "Posting everyday until i forget her",
            badgeXPercent = 0.75f,
            badgeYPercent = 0.15f
        )
        val reps = listOf(
            PushUpRep(id = 1, timestampMs = 1200L, depthPercent = 95f),
            PushUpRep(id = 2, timestampMs = 2400L, depthPercent = 92f)
        )

        val id = repository.saveProject(
            id = 0,
            config = config,
            reps = reps,
            videoUri = null,
            videoFilePath = "/tmp/sample.mp4"
        )
        assertTrue(id > 0)

        val draft = repository.getCurrentDraft()
        assertNotNull(draft)
        assertEquals(29, draft?.dayNumber)
        assertEquals("Posting everyday until i forget her", draft?.quoteText)
        assertEquals(2, draft?.detectedRepsCount)

        val all = repository.allProjects.first()
        assertEquals(1, all.size)
        assertEquals("Day 29 Push-up Reel", all[0].title)
    }
}
