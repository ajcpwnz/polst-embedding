package com.polst.sdk.offline.replay

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import java.io.File

@Database(entities = [ReplayEntity::class], version = 1, exportSchema = false)
internal abstract class ReplayDatabase : RoomDatabase() {
    internal abstract fun replayDao(): ReplayDao

    internal companion object {
        @Volatile
        private var INSTANCE: ReplayDatabase? = null

        internal fun getInstance(context: Context): ReplayDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: build(context).also { INSTANCE = it }
            }

        private fun build(context: Context): ReplayDatabase {
            val dbFile = File(context.filesDir, "polst/replay.db").apply {
                parentFile?.mkdirs()
            }
            return Room.databaseBuilder(
                context.applicationContext,
                ReplayDatabase::class.java,
                dbFile.absolutePath,
            )
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}
