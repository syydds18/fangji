package com.construction.diary.cloud.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "funding_sources")
data class FundingSource(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val contributorName: String = "",  // 出资人姓名
    val relationship: String = "",     // 关系（爸爸/妈妈/自己等）
    val amount: Double = 0.0,          // 金额
    val date: Long = System.currentTimeMillis(),  // 出资日期
    val note: String = "",             // 备注（现金/转账等）
    val createdAt: Long = System.currentTimeMillis()
)
