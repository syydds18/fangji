package com.construction.diary.cloud.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "milestones")
data class Milestone(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String = "",          // 事件标题
    val date: Long = System.currentTimeMillis(),
    val description: String = "",
    val imagePaths: String = "",     // 图片路径，逗号分隔
    val createdAt: Long = System.currentTimeMillis()
)
