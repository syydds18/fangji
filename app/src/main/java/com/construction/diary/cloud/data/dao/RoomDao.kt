package com.construction.diary.cloud.data.dao

import androidx.room.*
import com.construction.diary.cloud.data.entity.HouseRoom
import kotlinx.coroutines.flow.Flow

@Dao
interface RoomDao {
    @Query("SELECT * FROM house_rooms ORDER BY status ASC, name")
    fun getAll(): Flow<List<HouseRoom>>

    @Query("SELECT * FROM house_rooms ORDER BY name")
    suspend fun getAllList(): List<HouseRoom>

    @Query("SELECT * FROM house_rooms WHERE id = :id")
    suspend fun getById(id: Long): HouseRoom?

    @Insert
    suspend fun insert(room: HouseRoom): Long

    @Update
    suspend fun update(room: HouseRoom)

    @Delete
    suspend fun delete(room: HouseRoom)

    @Query("DELETE FROM house_rooms")
    suspend fun deleteAll()
}
