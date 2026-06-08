package com.example.data.repository

import com.example.data.local.HighScoreDao
import com.example.data.local.QuestionDao
import com.example.data.local.UserStatsDao
import com.example.data.local.LeaderboardDao
import com.example.data.local.FriendDao
import com.example.data.local.QuestionDbInitializer
import com.example.data.model.HighScore
import com.example.data.model.Question
import com.example.data.model.UserStats
import com.example.data.model.LeaderboardEntry
import com.example.data.model.Friend
import kotlinx.coroutines.flow.Flow

class QuizRepository(
    private val questionDao: QuestionDao,
    private val highScoreDao: HighScoreDao,
    private val userStatsDao: UserStatsDao,
    private val leaderboardDao: LeaderboardDao,
    private val friendDao: FriendDao
) {
    // Collect stats, leaderboards, and friends reactively
    val highScoreFlow: Flow<HighScore?> = highScoreDao.getHighScoreFlow()
    val userStatsFlow: Flow<UserStats?> = userStatsDao.getUserStatsFlow()
    val leaderboardScoresFlow: Flow<List<LeaderboardEntry>> = leaderboardDao.getTopScores()
    val friendsFlow: Flow<List<Friend>> = friendDao.getAllFriendsFlow()

    suspend fun prepopulateQuestionsIfNeeded(): Int {
        val count = questionDao.getQuestionCount()
        if (count == 0) {
            val seeded = QuestionDbInitializer.getSeededQuestions()
            questionDao.insertQuestions(seeded)
        }
        
        // Also ensure user_stats is initialized
        val stats = userStatsDao.getUserStatsSync()
        if (stats == null) {
            userStatsDao.insertUserStats(UserStats())
        }

        // Initialize default mock friends list
        val friendsCount = friendDao.getFriendsCount()
        if (friendsCount == 0) {
            val defaultFriends = listOf(
                Friend(friendUserId = "ABQ-6204", name = "خالد العتيبي", status = "نشط", score = 840),
                Friend(friendUserId = "ABQ-5561", name = "رانية الحربي", status = "غائب", score = 420),
                Friend(friendUserId = "ABQ-7783", name = "ياسين السعدوني", status = "نشط", score = 1250)
            )
            friendDao.insertFriends(defaultFriends)
        }
        
        return count
    }

    suspend fun loadQuizQuestions(category: String, level: String, limit: Int = 15): List<Question> {
        return when {
            category == "ALL" && level == "ALL" -> {
                questionDao.getRandomQuestions(limit)
            }
            category == "ALL" -> {
                questionDao.getRandomQuestionsByLevel(level, limit)
            }
            level == "ALL" -> {
                questionDao.getRandomQuestionsByCategory(category, limit)
            }
            else -> {
                questionDao.getRandomQuestionsByCategoryAndLevel(category, level, limit)
            }
        }
    }

    suspend fun getCurrentUserStats(): UserStats {
        var stats = userStatsDao.getUserStatsSync()
        if (stats == null) {
            stats = UserStats()
            userStatsDao.insertUserStats(stats)
        }
        return stats
    }

    suspend fun addCoins(amount: Int) {
        val stats = getCurrentUserStats()
        userStatsDao.insertUserStats(stats.copy(coins = stats.coins + amount))
    }

    suspend fun updateStreak(correct: Boolean) {
        val stats = getCurrentUserStats()
        val newStreak = if (correct) stats.currentStreak + 1 else 0
        userStatsDao.insertUserStats(stats.copy(currentStreak = newStreak))
    }

    suspend fun deductCoins(amount: Int): Boolean {
        val stats = getCurrentUserStats()
        if (stats.coins >= amount) {
            userStatsDao.insertUserStats(stats.copy(coins = stats.coins - amount))
            return true
        }
        return false
    }

    suspend fun useSkipUnit(): Boolean {
        val stats = getCurrentUserStats()
        if (stats.totalSkipsAvailable > 0) {
            userStatsDao.insertUserStats(stats.copy(totalSkipsAvailable = stats.totalSkipsAvailable - 1))
            return true
        } else if (stats.coins >= 30) {
            // Spend 30 coins
            userStatsDao.insertUserStats(stats.copy(coins = stats.coins - 30))
            return true
        }
        return false
    }

    suspend fun incrementCorrectAnswers(incrementStreak: Boolean = true): Int {
        val stats = getCurrentUserStats()
        val newStreak = if (incrementStreak) stats.currentStreak + 1 else stats.currentStreak
        // Check bonus coins for streak (e.g. 3, 5, 10, 15)
        var bonus = 0
        if (newStreak > 0 && newStreak % 3 == 0) {
            bonus = 15 // +15 coins bonus for streak of size multiple of 3!
        }
        val earned = 10 + bonus
        userStatsDao.insertUserStats(stats.copy(
            coins = stats.coins + earned,
            totalCorrectAnswers = stats.totalCorrectAnswers + 1,
            currentStreak = newStreak
        ))
        return earned
    }

    suspend fun recordGameEnded(score: Int, category: String, level: String): Boolean {
        // 1. Save entry to local leaderboard history
        leaderboardDao.insertLeaderboardEntry(
            LeaderboardEntry(score = score, category = category, level = level)
        )

        // 2. See if update highscore
        val currentHighScore = highScoreDao.getHighScoreSync()
        var newBest = false
        if (currentHighScore == null || score > currentHighScore.score) {
            highScoreDao.insertHighScore(HighScore(score = score))
            newBest = true
        }

        // 3. Update UserStats total high score if needed
        val stats = getCurrentUserStats()
        val finalMaxScore = if (score > stats.score) score else stats.score
        userStatsDao.insertUserStats(stats.copy(
            score = finalMaxScore,
            currentStreak = 0 // Reset active game streak when game closes or ends
        ))

        return newBest
    }

    suspend fun claimSpinReward(coins: Int, skips: Int): UserStats {
        val stats = getCurrentUserStats()
        val updated = stats.copy(
            coins = stats.coins + coins,
            totalSkipsAvailable = stats.totalSkipsAvailable + skips,
            lastSpinTimeMillis = System.currentTimeMillis()
        )
        userStatsDao.insertUserStats(updated)
        return updated
    }

    suspend fun purchaseRemoveAds(useCoins: Boolean): Boolean {
        val stats = getCurrentUserStats()
        if (stats.isAdsRemoved) return true
        if (useCoins) {
            if (stats.coins >= 5000) {
                userStatsDao.insertUserStats(stats.copy(
                    coins = stats.coins - 5000,
                    isAdsRemoved = true
                ))
                return true
            }
            return false
        } else {
            // Direct mock purchase
            userStatsDao.insertUserStats(stats.copy(isAdsRemoved = true))
            return true
        }
    }

    suspend fun addHintsCount(amount: Int) {
        val stats = getCurrentUserStats()
        userStatsDao.insertUserStats(stats.copy(hintsCount = stats.hintsCount + amount))
    }

    suspend fun purchaseHints(amount: Int, cost: Int): Boolean {
        val stats = getCurrentUserStats()
        if (stats.coins >= cost) {
            userStatsDao.insertUserStats(stats.copy(
                coins = stats.coins - cost,
                hintsCount = stats.hintsCount + amount
            ))
            return true
        }
        return false
    }

    suspend fun addFriend(uid: String, name: String): Boolean {
        val count = friendDao.getAllFriendsSync().count { it.friendUserId == uid }
        if (count > 0 || uid.isBlank() || name.isBlank()) return false
        val newFriend = Friend(friendUserId = uid, name = name, status = "نشط", score = (200..1200).random())
        friendDao.insertFriend(newFriend)
        return true
    }

    suspend fun useHintUnit(): Boolean {
        val stats = getCurrentUserStats()
        if (stats.hintsCount > 0) {
            userStatsDao.insertUserStats(stats.copy(hintsCount = stats.hintsCount - 1))
            return true
        } else if (stats.coins >= 30) {
            userStatsDao.insertUserStats(stats.copy(coins = stats.coins - 30))
            return true
        }
        return false
    }
}
