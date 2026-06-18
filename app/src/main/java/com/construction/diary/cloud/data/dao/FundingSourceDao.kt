package com.construction.diary.cloud.data.dao

import androidx.room.*
import com.construction.diary.cloud.data.entity.FundingSource
import kotlinx.coroutines.flow.Flow

@Dao
interface FundingSourceDao {
    @Query("SELECT * FROM funding_sources ORDER BY date DESC")
    fun getAll(): Flow<List<FundingSource>>

    @Query("SELECT * FROM funding_sources ORDER BY date DESC")
    suspend fun getAllList(): List<FundingSource>

    @Query("SELECT SUM(amount) FROM funding_sources")
    suspend fun getTotalAmount(): Double?

    @Insert
    suspend fun insert(fundingSource: FundingSource): Long

    @Update
    suspend fun update(fundingSource: FundingSource)

    @Delete
    suspend fun delete(fundingSource: FundingSource)

    @Query("DELETE FROM funding_sources")
    suspend fun deleteAll()
}
