package com.kingplay.desktop.userdata

/** "3 h 20 min", "45 min", "2 d 5 h" or "under a minute". */
fun formatListening(ms: Long): String {
    val minutes = ms / MS_PER_MINUTE
    val hours = minutes / MINUTES_PER_HOUR
    return when {
        minutes < 1 -> "under a minute"
        hours < 1 -> "$minutes min"
        hours < HOURS_PER_DAY -> "$hours h ${minutes % MINUTES_PER_HOUR} min"
        else -> "${hours / HOURS_PER_DAY} d ${hours % HOURS_PER_DAY} h"
    }
}

/** "Good morning, James" for the hour of the day (0-23). */
fun greeting(hour: Int, nickname: String): String {
    val part = when {
        hour < NOON -> "Good morning"
        hour < EVENING -> "Good afternoon"
        else -> "Good evening"
    }
    return "$part, $nickname"
}

private const val MS_PER_MINUTE = 60_000L
private const val MINUTES_PER_HOUR = 60L
private const val HOURS_PER_DAY = 24L
private const val NOON = 12
private const val EVENING = 17
