package com.construction.diary.cloud.data.dao

import androidx.room.*
import com.construction.diary.cloud.data.entity.StandalonePhoto

@Dao
interface StandalonePhotoDao {
    @Query("SELECT * FROM standalone_photos ORDER BY takenAt DESC")
    suspend fun getAllList(): List<StandalonePhoto>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(photo: StandalonePhoto): Long

    @Update
    suspend fun update(photo: StandalonePhoto)

    @Delete
    suspend fun delete(photo: StandalonePhoto)
}
