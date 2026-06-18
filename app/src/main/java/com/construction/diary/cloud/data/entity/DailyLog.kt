package com.construction.diary.cloud.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_logs")
data class DailyLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long = System.currentTimeMillis(),
    val content: String = "",        // 施工内容描述
    val weather: String = "",        // 天气
    val workers: String = "",        // 今日到场工人（逗号分隔）
    val progress: Int = 0,           // 总体进度百分比 0-100
    val phase: String = "",          // 当前施工阶段
    val imagePaths: String = "",     // 图片路径，逗号分隔
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
