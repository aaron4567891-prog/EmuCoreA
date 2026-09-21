package com.sbro.emucorea.core

import kotlin.math.ceil

/** Largest emulated step a single frame may advance without it being a state jump. */
internal const val MAX_EMULATED_FRAME_STEP_US = 1_000_000L

/**
 * Content frame rate implied by the emulated time one frontend frame advanced.
 *
 * The libretro AV info always reports the 59.94 Hz vblank clock, so a game that
 * flips every other vblank (30 fps) would be paced at 60 - twice its speed -
 * unless the loop paces from the step instead. Zero or oversized steps (boot,
 * savestate load, rewind) keep [fallback].
 */
internal fun frameRateFromEmulatedStep(stepUs: Long, fallback: Double): Double {
    if (stepUs <= 0L || stepUs > MAX_EMULATED_FRAME_STEP_US) return fallback
    return 1_000_000.0 / stepUs
}

/** Keeps frame deadlines steady across scheduler jitter and drops long-stall debt. */
internal class FramePacer {
    private var lastStartNanos: Long? = null
    private var deadlineNanos: Long? = null
    private var periodNanos = 0L

    fun remainingNanos(nowNanos: Long, framesPerSecond: Double): Long {
        if (!framesPerSecond.isFinite() || framesPerSecond <= 0.0) return 0L
        val period = ceil(1_000_000_000.0 / framesPerSecond).toLong()
        if (period != periodNanos) {
            periodNanos = period
            deadlineNanos = lastStartNanos?.plus(period)
        }
        val earliest = lastStartNanos?.plus(periodNanos / 2) ?: nowNanos
        return (maxOf(deadlineNanos ?: nowNanos, earliest) - nowNanos).coerceAtLeast(0L)
    }

    fun frameStarted(nowNanos: Long) {
        lastStartNanos = nowNanos
        val deadline = deadlineNanos
        deadlineNanos = if (deadline == null || nowNanos - deadline >= periodNanos * 4) {
            nowNanos + periodNanos
        } else {
            deadline + periodNanos
        }
    }

    fun reset() {
        lastStartNanos = null
        deadlineNanos = null
        periodNanos = 0L
    }
}
