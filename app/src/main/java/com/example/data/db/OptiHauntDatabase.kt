package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.GeneratedImageEntity
import com.example.data.model.InfluenceMarkEntity
import com.example.data.model.InstructionVersionEntity
import com.example.data.model.ProjectSessionEntity
import com.example.data.model.PromptEntity
import com.example.data.model.ThemeSteerEntity

@Database(
    entities = [
        ProjectSessionEntity::class,
        InstructionVersionEntity::class,
        PromptEntity::class,
        GeneratedImageEntity::class,
        InfluenceMarkEntity::class,
        ThemeSteerEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class OptiHauntDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
    abstract fun versionDao(): InstructionVersionDao
    abstract fun promptDao(): PromptDao
    abstract fun imageDao(): ImageDao
    abstract fun influenceDao(): InfluenceDao
    abstract fun steerDao(): SteerDao

    companion object {
        @Volatile
        private var INSTANCE: OptiHauntDatabase? = null

        fun getDatabase(context: Context): OptiHauntDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    OptiHauntDatabase::class.java,
                    "optihaunt_database"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
