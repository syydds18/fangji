package com.construction.diary.cloud.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "project_config")
data class ProjectConfig(
    @PrimaryKey val key: String = "",
    val value: String = ""
)
