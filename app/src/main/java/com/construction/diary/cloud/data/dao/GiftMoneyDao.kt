package com.construction.diary.cloud.data.dao

import androidx.room.*
import com.construction.diary.cloud.data.entity.GiftMoney
import kotlinx.coroutines.flow.Flow

@Dao
interface GiftMoneyDao {
    @Query("SELECT * FROM gift_money ORDER BY date DESC")
    fun getAll(): Flow<List<GiftMoney>>

    @Query("SELECT * FROM gift_money ORDER BY date DESC")
    suspend fun getAllList(): List<GiftMoney>

    @Query("SELECT SUM(amount) FROM gift_money")
    suspend fun getTotalAmount(): Double?

    @Query("SELECT SUM(amount) FROM gift_money WHERE occasion = :occasion")
    suspend fun getTotalByOccasion(occasion: String): Double?

    @Insert
    suspend fun insert(gift: GiftMoney): Long

    @Update
    suspend fun update(gift: GiftMoney)

    @Delete
    suspend fun delete(gift: GiftMoney)

    @Query("DELETE FROM gift_money")
    suspend fun deleteAll()
}
