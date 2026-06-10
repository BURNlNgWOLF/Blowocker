package com.example.overlay.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity representing the total usage time for a package.
 * The primary key is the package name, ensuring a single row per app.
 */
@Entity(tableName = "usage_records")
data class UsageRecord(
    @PrimaryKey val packageName: String,
    // total time spent in seconds
    val timeSeconds: Long
)
