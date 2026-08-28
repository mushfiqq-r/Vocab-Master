package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.ReviewDao
import com.example.data.dao.StatsDao
import com.example.data.dao.WordDao
import com.example.data.model.ReviewStateEntity
import com.example.data.model.UserStatsEntity
import com.example.data.model.WordEntity

@Database(
    entities = [
        WordEntity::class,
        ReviewStateEntity::class,
        UserStatsEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun wordDao(): WordDao
    abstract fun reviewDao(): ReviewDao
    abstract fun statsDao(): StatsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "vocabmaster_database.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
