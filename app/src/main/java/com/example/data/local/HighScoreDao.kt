package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.HighScore
import kotlinx.coroutines.flow.Flow

@Dao
interface HighScoreDao {
    @Query("SELECT * FROM high_scores WHERE id = 1 LIMIT 1")
    fun getHighScoreFlow(): Flow<HighScore?>

    @Query("SELECT * FROM high_scores WHERE id = 1 LIMIT 1")
    suspend fun getHighScoreSync(): HighScore?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHighScore(highScore: HighScore)
}
