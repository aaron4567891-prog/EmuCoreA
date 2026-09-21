package com.sbro.emucorea.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FramePacerTest {
    @Test fun capsFrameStartsAtConfiguredRate() {
        for (rate in listOf(50.0, 59.94, 60.0, 180.0)) {
            val pacer = FramePacer()
            var now = 0L
            repeat(600) {
                val wait = pacer.remainingNanos(now, rate)
                now += wait
                pacer.frameStarted(now)
                assertTrue(pacer.remainingNanos(now, rate) >= 1_000_000_000.0 / rate)
                now += 3_000_000L
            }
            assertTrue(now >= 599 * 1_000_000_000.0 / rate)
        }
    }

    @Test fun missedDeadlineDoesNotAllowCatchUpBurst() {
        val pacer = FramePacer()
        pacer.frameStarted(0)
        assertEquals(0L, pacer.remainingNanos(100_000_000, 60.0))
        pacer.frameStarted(100_000_000)
        assertEquals(13_666_667L, pacer.remainingNanos(103_000_000, 60.0))
    }

    @Test fun changingRateUsesNewIntervalImmediately() {
        val pacer = FramePacer()
        pacer.frameStarted(0)
        assertEquals(17_000_000L, pacer.remainingNanos(3_000_000, 50.0))
        assertEquals(13_666_667L, pacer.remainingNanos(3_000_000, 60.0))
        pacer.reset()
        assertEquals(0L, pacer.remainingNanos(3_000_000, 60.0))
    }

    @Test fun schedulerOversleepDoesNotAccumulate() {
        val pacer = FramePacer()
        var now = 0L
        repeat(600) {
            now += pacer.remainingNanos(now, 60.0)
            now += 100_000L
            pacer.frameStarted(now)
            now += 3_000_000L
        }
        val expected = 599 * 16_666_667L + 3_000_000L
        assertTrue(now - expected in 0L..200_000L)
    }

    @Test fun shortStallStillLeavesTimeBeforeNextFrame() {
        val pacer = FramePacer()
        pacer.remainingNanos(0, 60.0)
        pacer.frameStarted(0)
        pacer.frameStarted(32_000_000)
        assertTrue(pacer.remainingNanos(32_000_000, 60.0) >= 8_333_333L)
    }

    @Test fun contentRateFollowsEmulatedFrameStep() {
        // One vblank per frame: 60 fps content, duplicated or not.
        assertEquals(59.94, frameRateFromEmulatedStep(16_683, 60.0), 0.01)
        // Two vblanks per frame: a 30 fps game must not be paced at 60.
        assertEquals(29.97, frameRateFromEmulatedStep(33_367, 60.0), 0.01)
    }

    @Test fun stateJumpsKeepThePreviousContentRate() {
        assertEquals(59.94, frameRateFromEmulatedStep(0, 59.94), 0.0)
        assertEquals(59.94, frameRateFromEmulatedStep(-33_367, 59.94), 0.0)
        assertEquals(59.94, frameRateFromEmulatedStep(MAX_EMULATED_FRAME_STEP_US + 1, 59.94), 0.0)
    }

    @Test fun periodicWorkDoesNotPermanentlySlowEmulation() {
        val pacer = FramePacer()
        var now = 0L
        repeat(600) { frame ->
            now += pacer.remainingNanos(now, 60.0)
            pacer.frameStarted(now)
            now += if (frame % 120 == 60) 45_000_000L else 3_000_000L
        }
        assertEquals(599 * 16_666_667L + 3_000_000L, now)
    }
}
