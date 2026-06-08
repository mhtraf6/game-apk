package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.HighScore
import com.example.data.model.Question
import com.example.data.model.UserStats
import com.example.data.model.LeaderboardEntry
import com.example.data.model.Friend

@Database(entities = [Question::class, HighScore::class, UserStats::class, LeaderboardEntry::class, Friend::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun questionDao(): QuestionDao
    abstract fun highScoreDao(): HighScoreDao
    abstract fun userStatsDao(): UserStatsDao
    abstract fun leaderboardDao(): LeaderboardDao
    abstract fun friendDao(): FriendDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "abaqarah_quiz_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
