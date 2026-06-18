package com.construction.diary.cloud.data.dao

import androidx.room.*
import com.construction.diary.cloud.data.entity.Supplier
import kotlinx.coroutines.flow.Flow

@Dao
interface SupplierDao {
    @Query("SELECT * FROM suppliers ORDER BY category, name")
    fun getAll(): Flow<List<Supplier>>

    @Query("SELECT * FROM suppliers ORDER BY category, name")
    suspend fun getAllList(): List<Supplier>

    @Query("SELECT * FROM suppliers WHERE name LIKE '%' || :query || '%' OR contact LIKE '%' || :query || '%' OR phone LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%'")
    suspend fun search(query: String): List<Supplier>

    @Insert
    suspend fun insert(supplier: Supplier): Long

    @Update
    suspend fun update(supplier: Supplier)

    @Delete
    suspend fun delete(supplier: Supplier)

    @Query("DELETE FROM suppliers")
    suspend fun deleteAll()
}
