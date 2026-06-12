package com.example.subskill.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_settings")
data class AppSettings(
    @PrimaryKey
    val packageName: String,
    val serviceName: String? = null,
    val monthlyFee: Int? = null,
    val isCandidate: Boolean = false
)