package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.data.model.LeaderboardEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface LeaderboardDao {
    @Query("SELECT * FROM leaderboard ORDER BY score DESC, timestamp DESC LIMIT 10")
    fun getTopScores(): Flow<List<LeaderboardEntry>>

    @Insert
    suspend fun insertLeaderboardEntry(entry: LeaderboardEntry)
}
