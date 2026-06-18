package com.construction.diary.cloud.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "appliances")
data class Appliance(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String = "",
    val brand: String = "",
    val model: String = "",
    val category: String = "",
    val purchaseChannel: String = "",
    val purchaseDate: Long = 0,
    val price: Double = 0.0,
    val warrantyYears: Int = 0,
    val warrantyExpireDate: Long = 0,
    val installDate: Long = 0,
    val afterSalePhone: String = "",
    val serialNumber: String = "",
    val note: String = "",
    val imagePaths: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
