package com.construction.diary.cloud.data.dao

import androidx.room.*
import com.construction.diary.cloud.data.entity.ProjectConfig

@Dao
interface ProjectConfigDao {
    @Query("SELECT value FROM project_config WHERE `key` = :key")
    suspend fun getValue(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun set(config: ProjectConfig)

    @Query("SELECT * FROM project_config")
    suspend fun getAll(): List<ProjectConfig>

    @Query("DELETE FROM project_config")
    suspend fun deleteAll()
}
