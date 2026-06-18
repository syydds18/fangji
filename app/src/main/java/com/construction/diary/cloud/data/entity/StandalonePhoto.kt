package com.construction.diary.cloud.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "standalone_photos")
data class StandalonePhoto(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fileName: String = "",           // 图片文件名
    val caption: String = "",            // 备注
    val takenAt: Long = System.currentTimeMillis(),  // 拍摄时间
    val createdAt: Long = System.currentTimeMillis()
)
