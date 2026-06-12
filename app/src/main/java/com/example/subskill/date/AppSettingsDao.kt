package com.example.subskill.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface AppSettingsDao {

    @Query("SELECT * FROM app_settings")
    suspend fun getAll(): List<AppSettings>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(appSettings: AppSettings)

    @Update
    suspend fun update(appSettings: AppSettings)

    @Query("DELETE FROM app_settings WHERE packageName = :packageName")
    suspend fun delete(packageName: String)
}