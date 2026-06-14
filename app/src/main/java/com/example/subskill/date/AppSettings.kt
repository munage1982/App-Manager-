package com.example.subskill.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_settings")
data class AppSettings(
    @PrimaryKey
    val packageName: String,
    val serviceName: String? = null,
    val monthlyFee: Double? = null,
    val isCandidate: Boolean = false,
    val isManualSubscription: Boolean = false
)