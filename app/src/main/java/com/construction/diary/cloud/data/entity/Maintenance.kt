package com.construction.diary.cloud.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "maintenance")
data class Maintenance(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String = "",
    val category: String = "",
    val location: String = "",
    val description: String = "",
    val repairDate: Long = System.currentTimeMillis(),
    val cost: Double = 0.0,
    val workerName: String = "",
    val workerPhone: String = "",
    val status: String = "待维修",
    val cause: String = "",
    val note: String = "",
    val imagePaths: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
