package com.selfcontrol.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Date and time utility functions
 */
object DateUtil {
    
    private const val PATTERN_FULL_DATE_TIME = "yyyy-MM-dd HH:mm:ss"
    private const val PATTERN_DATE_ONLY = "yyyy-MM-dd"
    private const val PATTERN_TIME_ONLY = "HH:mm:ss"
    private const val PATTERN_DISPLAY = "MMM dd, yyyy HH:mm"
    
    /**
     * Format timestamp to readable date-time string
     */
    fun formatTimestamp(timestamp: Long, pattern: String = PATTERN_DISPLAY): String {
        val date = Date(timestamp)
        val formatter = SimpleDateFormat(pattern, Locale.getDefault())
        return formatter.format(date)
    }
    
    /**
     * Get current timestamp in milliseconds
     */
    fun now(): Long = System.currentTimeMillis()
    
    /**
     * Get relative time span (e.g., "2 hours ago", "just now")
     */
    fun getRelativeTimeSpan(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        
        return when {
            diff < TimeUnit.MINUTES.toMillis(1) -> "Just now"
            diff < TimeUnit.HOURS.toMillis(1) -> {
                val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
                "$minutes minute${if (minutes > 1) "s" else ""} ago"
            }
            diff < TimeUnit.DAYS.toMillis(1) -> {
                val hours = TimeUnit.MILLISECONDS.toHours(diff)
                "$hours hour${if (hours > 1) "s" else ""} ago"
            }
            diff < TimeUnit.DAYS.toMillis(7) -> {
                val days = TimeUnit.MILLISECONDS.toDays(diff)
                "$days day${if (days > 1) "s" else ""} ago"
            }
            else -> formatTimestamp(timestamp, PATTERN_DATE_ONLY)
        }
    }
    
    /**
     * Check if timestamp is today
     */
    fun isToday(timestamp: Long): Boolean {
        val today = formatTimestamp(now(), PATTERN_DATE_ONLY)
        val target = formatTimestamp(timestamp, PATTERN_DATE_ONLY)
        return today == target
    }
    
    /**
     * Get start of day timestamp
     */
    fun getStartOfDay(timestamp: Long = now()): Long {
        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
    
    /**
     * Get end of day timestamp
     */
    fun getEndOfDay(timestamp: Long = now()): Long {
        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 23)
        calendar.set(java.util.Calendar.MINUTE, 59)
        calendar.set(java.util.Calendar.SECOND, 59)
        calendar.set(java.util.Calendar.MILLISECOND, 999)
        return calendar.timeInMillis
    }
    
    /**
     * Add hours to timestamp
     */
    fun addHours(timestamp: Long, hours: Int): Long {
        return timestamp + TimeUnit.HOURS.toMillis(hours.toLong())
    }
    
    /**
     * Add days to timestamp
     */
    fun addDays(timestamp: Long, days: Int): Long {
        return timestamp + TimeUnit.DAYS.toMillis(days.toLong())
    }
}
