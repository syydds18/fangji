package com.construction.diary.cloud.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "suppliers")
data class Supplier(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String = "",
    val contact: String = "",
    val phone: String = "",
    val address: String = "",
    val category: String = "",
    val qrCodePath: String = "",     // 收款码图片路径
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
