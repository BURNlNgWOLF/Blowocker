package com.example.overlay.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface UsageDao {
    @Query("SELECT * FROM usage_records")
    fun getAll(): List<UsageRecord>

    @Query("SELECT * FROM usage_records WHERE packageName = :pkg LIMIT 1")
    fun getByPackage(pkg: String): UsageRecord?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(record: UsageRecord)

    @Update
    fun update(record: UsageRecord)

    suspend fun upsert(packageName: String, addedSeconds: Long) {
        val existing = getByPackage(packageName)
        if (existing == null) {
            insert(UsageRecord(packageName, addedSeconds))
        } else {
            update(existing.copy(timeSeconds = existing.timeSeconds + addedSeconds))
        }
    }
}
