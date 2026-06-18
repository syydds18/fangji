package com.construction.diary.cloud.data.dao

import androidx.room.*
import com.construction.diary.cloud.data.entity.Maintenance
import kotlinx.coroutines.flow.Flow

@Dao
interface MaintenanceDao {
    @Query("SELECT * FROM maintenance ORDER BY status, repairDate DESC")
    fun getAll(): Flow<List<Maintenance>>

    @Query("SELECT * FROM maintenance ORDER BY status, repairDate DESC")
    suspend fun getAllList(): List<Maintenance>

    @Query("SELECT * FROM maintenance WHERE title LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%' OR location LIKE '%' || :query || '%' OR workerName LIKE '%' || :query || '%' OR status LIKE '%' || :query || '%'")
    suspend fun search(query: String): List<Maintenance>

    @Insert
    suspend fun insert(maintenance: Maintenance): Long

    @Update
    suspend fun update(maintenance: Maintenance)

    @Delete
    suspend fun delete(maintenance: Maintenance)

    @Query("DELETE FROM maintenance")
    suspend fun deleteAll()
}
