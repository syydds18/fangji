package com.construction.diary.cloud.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "house_rooms")
data class HouseRoom(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String = "",           // 房间名称
    val area: String = "",           // 面积
    val status: Int = 0,             // 0=未开始 1=施工中 2=已完成
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
