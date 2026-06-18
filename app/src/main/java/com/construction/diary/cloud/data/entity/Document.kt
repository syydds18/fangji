package com.construction.diary.cloud.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "documents")
data class Document(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String = "",
    val category: String = "",
    val storageLocation: String = "",
    val relatedParty: String = "",
    val issueDate: Long = 0,
    val expireDate: Long = 0,
    val note: String = "",
    val imagePaths: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
