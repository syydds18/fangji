package com.construction.diary.cloud.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "checklist_items")
data class ChecklistItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val phase: String = "",          // 施工阶段
    val title: String = "",          // 检查项名称
    val description: String = "",    // 检查标准/说明
    val isChecked: Boolean = false,  // 是否已验收通过
    val checkedDate: Long = 0,       // 验收日期
    val note: String = "",           // 备注
    val sortOrder: Int = 0,          // 排序
    val createdAt: Long = System.currentTimeMillis()
)
