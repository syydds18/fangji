package com.construction.diary.cloud.data.dao

import androidx.room.*
import com.construction.diary.cloud.data.entity.Worker
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkerDao {
    @Query("SELECT * FROM workers ORDER BY role, name")
    fun getAll(): Flow<List<Worker>>

    @Query("SELECT * FROM workers ORDER BY role, name")
    suspend fun getAllList(): List<Worker>

    @Insert
    suspend fun insert(worker: Worker): Long

    @Update
    suspend fun update(worker: Worker)

    @Delete
    suspend fun delete(worker: Worker)

    @Query("DELETE FROM workers")
    suspend fun deleteAll()
}
