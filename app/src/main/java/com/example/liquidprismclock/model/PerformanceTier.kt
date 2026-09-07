package com.example.liquidprismclock.model

import android.app.ActivityManager
import android.content.Context

/**
 * Coarse hardware capability tier used to scale particle counts, background
 * blob counts and effect detail. The goal is graceful degradation on weak
 * GPUs rather than forcing heavy effects everywhere.
 */
enum class PerformanceTier {
    LOW, MEDIUM, HIGH;

    /** Max particle pool size. */
    val particlePool: Int get() = when (this) {
        LOW -> 24
        MEDIUM -> 48
        HIGH -> 80
    }

    /** Ambient glass-dust particles kept on screen. */
    val ambientParticles: Int get() = when (this) {
        LOW -> 6
        MEDIUM -> 14
        HIGH -> 22
    }

    /** Background gradient blobs. */
    val blobCount: Int get() = when (this) {
        LOW -> 2
        MEDIUM -> 3
        HIGH -> 4
    }

    /** Whether the subtle noise/grain overlay is drawn. */
    val drawGrain: Boolean get() = this != LOW

    companion object {
        fun detect(context: Context): PerformanceTier {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val memClass = am?.memoryClass ?: 48
            val cores = Runtime.getRuntime().availableProcessors()
            return when {
                cores >= 6 && memClass >= 128 -> HIGH
                cores >= 4 && memClass >= 80 -> MEDIUM
                else -> LOW
            }
        }
    }
}
