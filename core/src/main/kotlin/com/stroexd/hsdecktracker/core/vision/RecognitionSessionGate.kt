package com.stroexd.hsdecktracker.core.vision

/** A delayed stop from a replaced producer must not stop its successor. */
class RecognitionSessionGate {
    private var generation = 0L
    @Synchronized fun start(): Long = ++generation
    @Synchronized fun isCurrent(session: Long): Boolean = session == generation
    @Synchronized fun stop(session: Long): Boolean {
        if (session != generation) return false
        generation++
        return true
    }
}
