package com.example.data.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        UserEntity::class,
        HabitEntity::class,
        HabitCompletionEntity::class,
        StreakEntity::class,
        GoalEntity::class,
        XpTransactionEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class HabitFlowDatabase : RoomDatabase() {
    abstract fun habitFlowDao(): HabitFlowDao

    companion object {
        @Volatile
        private var INSTANCE: HabitFlowDatabase? = null

        fun getDatabase(context: Context): HabitFlowDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    HabitFlowDatabase::class.java,
                    "habitflow_local_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
