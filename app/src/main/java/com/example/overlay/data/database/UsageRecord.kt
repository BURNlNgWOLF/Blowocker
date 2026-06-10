package com.example.overlay.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

// Primary key is package name
@Entity(tableName = "usage_records")
data class UsageRecord(
    @PrimaryKey val packageName: String,
    // total time spent in seconds
    val timeSeconds: Long
)
