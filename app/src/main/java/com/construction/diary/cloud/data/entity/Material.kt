package com.construction.diary.cloud.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "materials")
data class Material(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String = "",           // 材料名称
    val spec: String = "",           // 规格
    val quantity: String = "",       // 数量
    val budgetAmount: Double = 0.0,  // 预算金额
    val actualAmount: Double = 0.0,  // 实际金额
    val buyer: String = "",
    val phase: String = "",
    val status: Int = 0,             // 0=待购 1=已购 2=已到货
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
