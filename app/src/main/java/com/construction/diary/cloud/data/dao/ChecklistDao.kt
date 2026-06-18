package com.construction.diary.cloud.data.dao

import androidx.room.*
import com.construction.diary.cloud.data.entity.ChecklistItem
import kotlinx.coroutines.flow.Flow

@Dao
interface ChecklistDao {
    @Query("SELECT * FROM checklist_items ORDER BY phase, sortOrder")
    fun getAll(): Flow<List<ChecklistItem>>

    @Query("SELECT * FROM checklist_items WHERE phase = :phase ORDER BY sortOrder")
    fun getByPhase(phase: String): Flow<List<ChecklistItem>>

    @Query("SELECT * FROM checklist_items ORDER BY phase, sortOrder")
    suspend fun getAllList(): List<ChecklistItem>

    @Query("SELECT COUNT(*) FROM checklist_items WHERE phase = :phase AND isChecked = 1")
    suspend fun getCheckedCount(phase: String): Int

    @Query("SELECT COUNT(*) FROM checklist_items WHERE phase = :phase")
    suspend fun getTotalCount(phase: String): Int

    @Insert
    suspend fun insert(item: ChecklistItem): Long

    @Update
    suspend fun update(item: ChecklistItem)

    @Delete
    suspend fun delete(item: ChecklistItem)

    @Query("DELETE FROM checklist_items")
    suspend fun deleteAll()
}
