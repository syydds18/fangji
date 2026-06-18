package com.construction.diary.cloud.data.dao

import androidx.room.*
import com.construction.diary.cloud.data.entity.Document
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {
    @Query("SELECT * FROM documents ORDER BY category, name")
    fun getAll(): Flow<List<Document>>

    @Query("SELECT * FROM documents ORDER BY category, name")
    suspend fun getAllList(): List<Document>

    @Query("SELECT * FROM documents WHERE name LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%' OR storageLocation LIKE '%' || :query || '%' OR relatedParty LIKE '%' || :query || '%'")
    suspend fun search(query: String): List<Document>

    @Insert
    suspend fun insert(document: Document): Long

    @Update
    suspend fun update(document: Document)

    @Delete
    suspend fun delete(document: Document)

    @Query("DELETE FROM documents")
    suspend fun deleteAll()
}
