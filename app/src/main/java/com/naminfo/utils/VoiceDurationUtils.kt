package com.naminfo.utils

import java.util.Locale

object VoiceDurationUtils {
    fun format(durationMs: Int): String {
        val totalSeconds = durationMs.coerceAtLeast(0) / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60

        return String.format(Locale.ROOT, "%02d:%02d", minutes, seconds)
    }
}
