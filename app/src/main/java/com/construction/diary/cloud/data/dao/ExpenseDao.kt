package com.construction.diary.cloud.data.dao

import androidx.room.*
import com.construction.diary.cloud.data.entity.Expense
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY date DESC")
    fun getAll(): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE phase = :phase ORDER BY date DESC")
    fun getByPhase(phase: String): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE roomId = :roomId ORDER BY date DESC")
    fun getByRoom(roomId: Long): Flow<List<Expense>>

    @Query("SELECT * FROM expenses ORDER BY date DESC")
    suspend fun getAllList(): List<Expense>

    @Query("SELECT * FROM expenses WHERE itemName LIKE '%' || :query || '%' OR brand LIKE '%' || :query || '%' OR buyer LIKE '%' || :query || '%' OR note LIKE '%' || :query || '%'")
    suspend fun search(query: String): List<Expense>

    @Query("SELECT SUM(amount) FROM expenses")
    suspend fun getTotalAmount(): Double?

    @Query("SELECT SUM(amount) FROM expenses WHERE phase = :phase")
    suspend fun getTotalByPhase(phase: String): Double?

    @Query("SELECT SUM(amount) FROM expenses WHERE roomId = :roomId")
    suspend fun getTotalByRoom(roomId: Long): Double?

    @Query("SELECT DISTINCT phase FROM expenses WHERE phase != '' ORDER BY phase")
    suspend fun getAllPhases(): List<String>

    @Query("SELECT phase, SUM(amount) as total FROM expenses WHERE phase != '' GROUP BY phase ORDER BY total DESC")
    suspend fun getPhaseSums(): List<PhaseSum>

    @Query("SELECT buyer, SUM(amount) as total FROM expenses WHERE buyer != '' GROUP BY buyer ORDER BY total DESC LIMIT 10")
    suspend fun getTopBuyers(): List<BuyerSum>

    @Query("SELECT * FROM expenses ORDER BY date DESC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<Expense>

    @Insert
    suspend fun insert(expense: Expense): Long

    @Update
    suspend fun update(expense: Expense)

    @Delete
    suspend fun delete(expense: Expense)

    @Query("DELETE FROM expenses")
    suspend fun deleteAll()
}

data class PhaseSum(val phase: String, val total: Double)
data class BuyerSum(val buyer: String, val total: Double)
