package com.construction.diary.cloud.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemName: String = "",       // 项目名称
    val brand: String = "",          // 品牌/型号
    val amount: Double = 0.0,        // 金额
    val buyer: String = "",          // 购买人
    val phase: String = "",          // 施工阶段
    val category: String = "",       // 子分类
    val roomId: Long = 0,            // 关联房间ID（0=公共区域）
    val supplierId: Long = 0,        // 关联供应商ID
    val date: Long = System.currentTimeMillis(),
    val note: String = "",
    val imagePaths: String = "",     // 图片路径，逗号分隔
    val createdAt: Long = System.currentTimeMillis()
)
