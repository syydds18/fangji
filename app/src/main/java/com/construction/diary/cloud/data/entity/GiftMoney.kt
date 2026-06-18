package com.construction.diary.cloud.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "gift_money")
data class GiftMoney(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val giverName: String = "",      // 给钱人姓名
    val relationship: String = "",   // 关系
    val amount: Double = 0.0,        // 金额
    val occasion: String = "",       // 场合（上梁/乔迁/其他）
    val date: Long = System.currentTimeMillis(),
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
