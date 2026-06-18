package com.construction.diary.cloud.data.dao

import androidx.room.*
import com.construction.diary.cloud.data.entity.DailyLog
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyLogDao {
    @Query("SELECT * FROM daily_logs ORDER BY date DESC")
    fun getAll(): Flow<List<DailyLog>>

    @Query("SELECT * FROM daily_logs ORDER BY date DESC")
    suspend fun getAllList(): List<DailyLog>

    @Query("SELECT * FROM daily_logs WHERE content LIKE '%' || :query || '%' OR weather LIKE '%' || :query || '%' OR workers LIKE '%' || :query || '%'")
    suspend fun search(query: String): List<DailyLog>

    @Query("SELECT * FROM daily_logs ORDER BY date DESC LIMIT 1")
    suspend fun getLatest(): DailyLog?

    @Insert
    suspend fun insert(log: DailyLog): Long

    @Update
    suspend fun update(log: DailyLog)

    @Delete
    suspend fun delete(log: DailyLog)

    @Query("DELETE FROM daily_logs")
    suspend fun deleteAll()
}
