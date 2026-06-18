package com.construction.diary.cloud.data.dao

import androidx.room.*
import com.construction.diary.cloud.data.entity.Appliance
import kotlinx.coroutines.flow.Flow

@Dao
interface ApplianceDao {
    @Query("SELECT * FROM appliances ORDER BY category, name")
    fun getAll(): Flow<List<Appliance>>

    @Query("SELECT * FROM appliances ORDER BY category, name")
    suspend fun getAllList(): List<Appliance>

    @Query("SELECT * FROM appliances WHERE name LIKE '%' || :query || '%' OR brand LIKE '%' || :query || '%' OR model LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%' OR serialNumber LIKE '%' || :query || '%'")
    suspend fun search(query: String): List<Appliance>

    @Insert
    suspend fun insert(appliance: Appliance): Long

    @Update
    suspend fun update(appliance: Appliance)

    @Delete
    suspend fun delete(appliance: Appliance)

    @Query("DELETE FROM appliances")
    suspend fun deleteAll()
}
