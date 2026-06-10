package com.example.overlay.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [UsageRecord::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun usageDao(): UsageDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Returns a singleton instance of the database.
         *
         * The `allowMainThreadQueries()` call is added **only for debugging** purposes.
         * In production you should perform all DB operations off the main thread (as we already do
         * via coroutines in `AppLaunchMonitor`). This flag prevents the database from being
         * considered “closed” when accessed from the UI thread during inspection.
         */
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "overlay_usage.db"
                )
                    // Enable main‑thread queries for debugging / inspection.
                    .allowMainThreadQueries()
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
