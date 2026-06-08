package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlin.random.Random

@Entity(tableName = "user_stats")
data class UserStats(
    @PrimaryKey val id: Int = 1,
    val coins: Int = 150, // Starts with 150 free coins
    val score: Int = 0,
    val totalCorrectAnswers: Int = 0,
    val lastSpinTimeMillis: Long = 0L,
    val totalSkipsAvailable: Int = 3,
    val currentStreak: Int = 0,
    val userUniqueId: String = "ABQ-${Random.nextInt(1000, 9999)}",
    val isAdsRemoved: Boolean = false,
    val hintsCount: Int = 3
)

