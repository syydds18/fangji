package com.construction.diary.cloud.data.dao

import androidx.room.*
import com.construction.diary.cloud.data.entity.Material
import kotlinx.coroutines.flow.Flow

@Dao
interface MaterialDao {
    @Query("SELECT * FROM materials ORDER BY status ASC, createdAt DESC")
    fun getAll(): Flow<List<Material>>

    @Query("SELECT * FROM materials WHERE status = :status ORDER BY createdAt DESC")
    fun getByStatus(status: Int): Flow<List<Material>>

    @Query("SELECT * FROM materials ORDER BY createdAt DESC")
    suspend fun getAllList(): List<Material>

    @Query("SELECT SUM(budgetAmount) FROM materials")
    suspend fun getTotalBudget(): Double?

    @Query("SELECT SUM(actualAmount) FROM materials")
    suspend fun getTotalActual(): Double?

    @Insert
    suspend fun insert(material: Material): Long

    @Update
    suspend fun update(material: Material)

    @Delete
    suspend fun delete(material: Material)

    @Query("DELETE FROM materials")
    suspend fun deleteAll()
}
