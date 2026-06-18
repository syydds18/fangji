package com.construction.diary.cloud.util

import java.text.SimpleDateFormat
import java.util.*

object DateUtils {
    private val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val dateTimeFmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    private val monthFmt = SimpleDateFormat("yyyy年MM月", Locale.getDefault())

    fun formatDate(timestamp: Long): String = dateFmt.format(Date(timestamp))
    fun formatDateTime(timestamp: Long): String = dateTimeFmt.format(Date(timestamp))
    fun formatMonth(timestamp: Long): String = monthFmt.format(Date(timestamp))

    fun todayStart(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
