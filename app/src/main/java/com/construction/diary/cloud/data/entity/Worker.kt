package com.construction.diary.cloud.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workers")
data class Worker(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String = "",
    val phone: String = "",
    val role: String = "",           // 工种（泥工/木工/水电/油漆等）
    val dailyWage: Double = 0.0,     // 日薪
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
