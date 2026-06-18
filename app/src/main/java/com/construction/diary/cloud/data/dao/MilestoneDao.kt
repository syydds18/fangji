package com.construction.diary.cloud.data.dao

import androidx.room.*
import com.construction.diary.cloud.data.entity.Milestone
import kotlinx.coroutines.flow.Flow

@Dao
interface MilestoneDao {
    @Query("SELECT * FROM milestones ORDER BY date DESC")
    fun getAll(): Flow<List<Milestone>>

    @Query("SELECT * FROM milestones ORDER BY date DESC")
    suspend fun getAllList(): List<Milestone>

    @Insert
    suspend fun insert(milestone: Milestone): Long

    @Update
    suspend fun update(milestone: Milestone)

    @Delete
    suspend fun delete(milestone: Milestone)

    @Query("DELETE FROM milestones")
    suspend fun deleteAll()
}
