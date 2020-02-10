package com.guesswho.movetracker.database


import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [LocationHistory::class], version = 1, exportSchema = false)
abstract class LocationHistoryDatabase : RoomDatabase() {

    abstract fun locationHistoryDoa(): LocationHistoryDoa

    companion object {
        @Volatile private var instance: LocationHistoryDatabase? = null
        private val LOCK = Any()

        operator fun invoke(context: Context)= instance ?: synchronized(LOCK){
            instance ?: buildDatabase(context).also { instance = it}
        }

        private fun buildDatabase(context: Context) = Room.databaseBuilder(context,
            LocationHistoryDatabase::class.java, "todo-list.db")
            .build()
    }
}
