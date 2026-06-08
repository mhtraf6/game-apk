package com.example.ui

import android.app.Application
import android.media.AudioManager
import android.media.ToneGenerator
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ui.screens.calculateLevelDetails
import com.example.data.local.AppDatabase
import com.example.data.model.HighScore
import com.example.data.model.Question
import com.example.data.model.UserStats
import com.example.data.model.LeaderboardEntry
import com.example.data.model.Friend
import com.example.data.repository.QuizRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class GameState {
    HOME, LOADING, PLAYING, RESULTS, LEADERBOARD, SPIN_WHEEL, SHOP, FRIENDS
}

data class QuizUiState(
    val gameState: GameState = GameState.HOME,
    val categories: List<String> = listOf("ALL", "General", "Science", "History", "Sports", "Geography", "Islamic"),
    val levels: List<String> = listOf("ALL", "Easy", "Medium", "Hard"),
    val selectedCategory: String = "ALL",
    val selectedLevel: String = "ALL",
    val questions: List<Question> = emptyList(),
    val currentQuestionIndex: Int = 0,
    val score: Int = 0,
    val selectedAnswer: String? = null,
    val highlightCorrectAnswer: String? = null,
    val isDbInitializing: Boolean = true,
    val highScore: Int = 0,
    val isNewHighScore: Boolean = false,
    val isSoundEnabled: Boolean = true,
    val isMusicEnabled: Boolean = true,
    val showLevelUpDialog: Boolean = false,
    val levelUpTitleState: String = "",
    val levelUpRoleState: String = "",
    
    // Stats and Progression
    val userStats: UserStats? = null,
    val leaderboard: List<LeaderboardEntry> = emptyList(),
    val totalCoinsEarnedThisSession: Int = 0,
    
    // Timer System
    val timeLeftSeconds: Float = 15f,
    val maxTimeSeconds: Float = 15f,
    
    // Spin Wheel system
    val isSpinning: Boolean = false,
    val wheelRotation: Float = 0f,
    val adSpinsWatchedCount: Int = 0, // max 2
    val extraSpinsAvailable: Int = 0,
    val showSpinRewardDialog: Boolean = false,
    val spinRewardTitle: String = "",
    val spinRewardCoins: Int = 0,
    val spinRewardSkips: Int = 0,
    
    // Friends state
    val friendsList: List<Friend> = emptyList(),
    val isHelpFeedbackActive: Boolean = false,
    val helpFeedbackMessage: String = "",
    
    // Interactive Help utilities on active Question state
    val removedWrongAnswers: List<String> = emptyList(), // Choices currently hidden ("A", "B", etc.)
    val hintTextForCurrentQuestion: String? = null,
    val hasUsedFiftyFiftyOnCurrentQuestion: Boolean = false,
    val hasUsedHintOnCurrentQuestion: Boolean = false,
    
    // Video Ads integration
    val isWatchingAd: Boolean = false,
    val adSecondsRemaining: Int = 0,
    val showAdCompleteNotification: Boolean = false
)

class QuizViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: QuizRepository
    private val _uiState = MutableStateFlow(QuizUiState())
    val uiState: StateFlow<QuizUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private val prefs = application.getSharedPreferences("challenge_prefs", android.content.Context.MODE_PRIVATE)

    init {
        val soundPref = prefs.getBoolean("sound_enabled", true)
        val musicPref = prefs.getBoolean("music_enabled", true)
        _uiState.update { 
            it.copy(
                isSoundEnabled = soundPref,
                isMusicEnabled = musicPref
            )
        }
        SynthPlayer.setMuted(!soundPref)
        SynthPlayer.setMusicEnabled(musicPref)
        AppVibrator.setMuted(!soundPref)

        val database = AppDatabase.getDatabase(application)
        repository = QuizRepository(
            database.questionDao(),
            database.highScoreDao(),
            database.userStatsDao(),
            database.leaderboardDao(),
            database.friendDao()
        )

        // 1. Initial configuration, seed data if empty
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isDbInitializing = true) }
            repository.prepopulateQuestionsIfNeeded()
            _uiState.update { it.copy(isDbInitializing = false) }
        }

        // 2. Observe Stats reactively and trigger level-up popup
        viewModelScope.launch {
            repository.userStatsFlow.collect { stats ->
                val prevStats = _uiState.value.userStats
                _uiState.update { it.copy(userStats = stats) }
                
                if (prevStats != null && stats != null) {
                    val prevLevel = calculateLevelDetails(prevStats.totalCorrectAnswers)
                    val newLevel = calculateLevelDetails(stats.totalCorrectAnswers)
                    if (newLevel.levelTitle != prevLevel.levelTitle) {
                        if (_uiState.value.isSoundEnabled) {
                            SynthPlayer.playLevelUpSound()
                        }
                        _uiState.update {
                            it.copy(
                                showLevelUpDialog = true,
                                levelUpTitleState = newLevel.levelTitle,
                                levelUpRoleState = newLevel.roleName
                            )
                        }
                    }
                }
            }
        }

        // 3. Observe Leaderboards reactively
        viewModelScope.launch {
            repository.leaderboardScoresFlow.collect { topScores ->
                _uiState.update { it.copy(leaderboard = topScores) }
            }
        }

        // 4. Observe High scores
        viewModelScope.launch {
            repository.highScoreFlow.collect { hs ->
                _uiState.update { it.copy(highScore = hs?.score ?: 0) }
            }
        }

        // 5. Observe Friends reactively
        viewModelScope.launch {
            repository.friendsFlow.collect { list ->
                _uiState.update { it.copy(friendsList = list) }
            }
        }
    }

    fun selectCategory(category: String) {
        _uiState.update { it.copy(selectedCategory = category) }
    }

    fun selectLevel(level: String) {
        _uiState.update { it.copy(selectedLevel = level) }
    }

    fun toggleSound() {
        val nextVal = !_uiState.value.isSoundEnabled
        _uiState.update { it.copy(isSoundEnabled = nextVal) }
        prefs.edit().putBoolean("sound_enabled", nextVal).apply()
        SynthPlayer.setMuted(!nextVal)
        AppVibrator.setMuted(!nextVal)
    }

    fun toggleMusic() {
        val nextVal = !_uiState.value.isMusicEnabled
        _uiState.update { it.copy(isMusicEnabled = nextVal) }
        prefs.edit().putBoolean("music_enabled", nextVal).apply()
        SynthPlayer.setMusicEnabled(nextVal)
        if (nextVal && _uiState.value.gameState == GameState.PLAYING) {
            SynthPlayer.startMusicLoop()
        } else {
            SynthPlayer.stopMusicLoop()
        }
    }

    fun dismissLevelUpDialog() {
        _uiState.update { it.copy(showLevelUpDialog = false) }
    }

    fun navigateToLeaderboard() {
        _uiState.update { it.copy(gameState = GameState.LEADERBOARD) }
    }

    fun navigateToSpinWheel() {
        _uiState.update { it.copy(gameState = GameState.SPIN_WHEEL) }
    }

    fun navigateToShop() {
        _uiState.update { it.copy(gameState = GameState.SHOP) }
    }

    fun navigateToFriends() {
        _uiState.update { it.copy(gameState = GameState.FRIENDS) }
    }

    fun startGame() {
        viewModelScope.launch {
            _uiState.update { it.copy(gameState = GameState.LOADING) }
            val state = _uiState.value
            
            // Load 15 questions for the game session
            val loadedQuestions = repository.loadQuizQuestions(
                category = state.selectedCategory,
                level = state.selectedLevel,
                limit = 15
            )

            if (loadedQuestions.isNotEmpty()) {
                _uiState.update {
                    it.copy(
                        questions = loadedQuestions,
                        currentQuestionIndex = 0,
                        score = 0,
                        totalCoinsEarnedThisSession = 0,
                        selectedAnswer = null,
                        highlightCorrectAnswer = null,
                        isNewHighScore = false,
                        gameState = GameState.PLAYING,
                        removedWrongAnswers = emptyList(),
                        hintTextForCurrentQuestion = null,
                        hasUsedFiftyFiftyOnCurrentQuestion = false,
                        hasUsedHintOnCurrentQuestion = false
                    )
                }
            } else {
                // Fallback
                val fallbackQuestions = repository.loadQuizQuestions("ALL", "ALL", 15)
                _uiState.update {
                    it.copy(
                        questions = fallbackQuestions,
                        currentQuestionIndex = 0,
                        score = 0,
                        totalCoinsEarnedThisSession = 0,
                        selectedAnswer = null,
                        highlightCorrectAnswer = null,
                        isNewHighScore = false,
                        gameState = GameState.PLAYING,
                        removedWrongAnswers = emptyList(),
                        hintTextForCurrentQuestion = null,
                        hasUsedFiftyFiftyOnCurrentQuestion = false,
                        hasUsedHintOnCurrentQuestion = false
                    )
                }
            }
            // Trigger countdown timer
            startQuestionTimer()
            if (_uiState.value.isMusicEnabled) {
                SynthPlayer.startMusicLoop()
            }
        }
    }

    private fun startQuestionTimer() {
        timerJob?.cancel()
        _uiState.update { it.copy(timeLeftSeconds = 15.0f, maxTimeSeconds = 15.0f) }
        
        timerJob = viewModelScope.launch {
            val tickRateMs = 100L
            while (_uiState.value.timeLeftSeconds > 0f && 
                   _uiState.value.gameState == GameState.PLAYING && 
                   _uiState.value.selectedAnswer == null) {
                delay(tickRateMs)
                _uiState.update {
                    val nextTime = (it.timeLeftSeconds - 0.1f).coerceAtLeast(0f)
                    it.copy(timeLeftSeconds = nextTime)
                }
            }
            // Trigger automatic feedback / wrong answer if timer expires on active play state
            if (_uiState.value.timeLeftSeconds <= 0f && 
                _uiState.value.selectedAnswer == null && 
                _uiState.value.gameState == GameState.PLAYING) {
                _uiState.update { it.copy(timeLeftSeconds = 0f) }
                handleTimeRunOut()
            }
        }
    }

    private fun handleTimeRunOut() {
        val state = _uiState.value
        val currentQuestion = state.questions.getOrNull(state.currentQuestionIndex) ?: return
        
        if (state.isSoundEnabled) {
            SynthPlayer.playWrongSound()
            AppVibrator.vibrateWrong()
        }

        viewModelScope.launch {
            // Update streak
            repository.updateStreak(false)
        }

        _uiState.update {
            it.copy(
                selectedAnswer = "TIMEOUT",
                highlightCorrectAnswer = currentQuestion.correctAnswer
            )
        }

        viewModelScope.launch {
            delay(1200) // Give short feedback showing correct alternative
            moveToNextQuestionOrFinish()
        }
    }

    fun selectAnswer(answerLetter: String) {
        if (_uiState.value.selectedAnswer != null) return

        val state = _uiState.value
        val currentQuestion = state.questions.getOrNull(state.currentQuestionIndex) ?: return
        val isCorrect = currentQuestion.correctAnswer == answerLetter

        if (state.isSoundEnabled) {
            if (isCorrect) {
                SynthPlayer.playCorrectSound()
                AppVibrator.vibrateCorrect()
            } else {
                SynthPlayer.playWrongSound()
                AppVibrator.vibrateWrong()
            }
        }

        viewModelScope.launch {
            var earnedCoins = 0
            if (isCorrect) {
                // Returns actual coins earned (base + streak bonus)
                earnedCoins = repository.incrementCorrectAnswers(incrementStreak = true)
            } else {
                repository.updateStreak(false)
            }

            val newScore = if (isCorrect) state.score + 1 else state.score

            _uiState.update {
                it.copy(
                    selectedAnswer = answerLetter,
                    highlightCorrectAnswer = currentQuestion.correctAnswer,
                    score = newScore,
                    totalCoinsEarnedThisSession = it.totalCoinsEarnedThisSession + earnedCoins
                )
            }

            delay(1000)
            moveToNextQuestionOrFinish()
        }
    }

    fun skipCurrentQuestion() {
        if (_uiState.value.selectedAnswer != null || _uiState.value.gameState != GameState.PLAYING) return
        
        viewModelScope.launch {
            val skipSuccessful = repository.useSkipUnit()
            if (skipSuccessful) {
                // Skip plays successful tone
                if (_uiState.value.isSoundEnabled) {
                    SynthPlayer.playButtonClickSound()
                }
                
                val currentQuestion = _uiState.value.questions.getOrNull(_uiState.value.currentQuestionIndex)
                _uiState.update {
                    it.copy(
                        selectedAnswer = "SKIPPED",
                        highlightCorrectAnswer = currentQuestion?.correctAnswer
                    )
                }
                delay(800)
                moveToNextQuestionOrFinish()
            } else {
                // Not enough coins/skips notification
                _uiState.update {
                    it.copy(
                        showSpinRewardDialog = true,
                        spinRewardTitle = "تنبيه",
                        spinRewardCoins = 0,
                        spinRewardSkips = -1 // customized flag indicating failed skip due to cost
                    )
                }
            }
        }
    }

    private suspend fun moveToNextQuestionOrFinish() {
        val state = _uiState.value
        val nextIdx = state.currentQuestionIndex + 1
        
        if (nextIdx < state.questions.size) {
            _uiState.update {
                it.copy(
                    currentQuestionIndex = nextIdx,
                    selectedAnswer = null,
                    highlightCorrectAnswer = null,
                    removedWrongAnswers = emptyList(),
                    hintTextForCurrentQuestion = null,
                    hasUsedFiftyFiftyOnCurrentQuestion = false,
                    hasUsedHintOnCurrentQuestion = false
                )
            }
            startQuestionTimer()
        } else {
            timerJob?.cancel()
            // Record game session end statistics
            val isNewRecord = repository.recordGameEnded(
                score = state.score,
                category = state.selectedCategory,
                level = state.selectedLevel
            )
            
            _uiState.update {
                it.copy(
                    isNewHighScore = isNewRecord,
                    gameState = GameState.RESULTS
                )
            }
            SynthPlayer.stopMusicLoop()
        }
    }

    // Daily Spin Wheel
    fun isDailySpinClaimable(): Boolean {
        val lastSpin = _uiState.value.userStats?.lastSpinTimeMillis ?: 0L
        val current = System.currentTimeMillis()
        val oneDayMillis = 24 * 60 * 60 * 1000L
        return (current - lastSpin) >= oneDayMillis
    }

    fun getDailySpinRemainingTimeString(): String {
        val lastSpin = _uiState.value.userStats?.lastSpinTimeMillis ?: 0L
        val current = System.currentTimeMillis()
        val oneDayMillis = 24 * 60 * 60 * 1000L
        val diff = oneDayMillis - (current - lastSpin)
        if (diff <= 0) return "دولاب الحظ متاح الآن!"
        
        val hours = diff / (60 * 60 * 1000L)
        val minutes = (diff % (60 * 60 * 1000L)) / (60 * 1000L)
        return "متاح بعد $hours ساعة و $minutes دقيقة"
    }

    fun spinWheel() {
        if (_uiState.value.isSpinning) return
        
        val isFree = isDailySpinClaimable()
        val hasAdSpins = _uiState.value.extraSpinsAvailable > 0
        
        if (!isFree && !hasAdSpins) return // Not available

        _uiState.update { it.copy(isSpinning = true) }

        viewModelScope.launch {
            // Spin sound rhythm ticks
            if (_uiState.value.isSoundEnabled) {
                launch {
                    for (i in 1..10) {
                        SynthPlayer.playButtonClickSound()
                        delay((100 + i * 35).toLong())
                    }
                }
            }

            // Decide target sector (8 slices)
            val randomSlice = (0..7).random()
            // Slice mapping:
            // 0 -> 50 Coins, 1 -> 1 Free Skip, 2 -> 10 Coins, 3 -> 100 Coins,
            // 4 -> 2 Free Skips, 5 -> 20 Coins, 6 -> 200 Coins, 7 -> 500 Coins (Super Jackpot)
            val currentRotationDegrees = _uiState.value.wheelRotation
            val addedRotation = (360 * 6) + (randomSlice * 45) + 22.5f
            val targetRotation = currentRotationDegrees + addedRotation

            // Quick rotation simulated interpolation
            _uiState.update { it.copy(wheelRotation = targetRotation) }
            delay(1500) // Match UI animation duration

            // Determine reward
            val (wonCoins, wonSkips, text) = when (randomSlice) {
                0 -> Triple(50, 0, "50 قطعة ذهبية")
                1 -> Triple(0, 1, "تخطي مجاني واحد")
                2 -> Triple(10, 0, "10 قطع ذهبية")
                3 -> Triple(100, 0, "100 قطعة ذهبية")
                4 -> Triple(0, 2, "2 تخطي مجاني")
                5 -> Triple(20, 0, "20 قطعة ذهبية")
                6 -> Triple(200, 0, "200 قطعة ذهبية")
                else -> Triple(500, 0, "الجائزة الكبرى: 500 قطعة ذهبية!")
            }

            // Claim reward in database
            repository.claimSpinReward(coins = wonCoins, skips = wonSkips)

            // Adjust available spins
            if (!isFree) {
                _uiState.update { it.copy(extraSpinsAvailable = (it.extraSpinsAvailable - 1).coerceAtLeast(0)) }
            }

            if (_uiState.value.isSoundEnabled) {
                SynthPlayer.playCorrectSound()
            }

            _uiState.update {
                it.copy(
                    isSpinning = false,
                    showSpinRewardDialog = true,
                    spinRewardTitle = "ألف مبروك! 🎉",
                    spinRewardCoins = wonCoins,
                    spinRewardSkips = wonSkips
                )
            }
        }
    }

    // Video Ads - Simulation
    fun triggerWatchAdForSpin() {
        if (_uiState.value.adSpinsWatchedCount >= 2) return // Max 2
        
        _uiState.update {
            it.copy(
                isWatchingAd = true,
                adSecondsRemaining = 5
            )
        }

        viewModelScope.launch {
            while (_uiState.value.adSecondsRemaining > 0) {
                delay(1000)
                _uiState.update { it.copy(adSecondsRemaining = it.adSecondsRemaining - 1) }
            }
            // Finished!
            _uiState.update {
                it.copy(
                    isWatchingAd = false,
                    adSpinsWatchedCount = it.adSpinsWatchedCount + 1,
                    extraSpinsAvailable = it.extraSpinsAvailable + 1,
                    showAdCompleteNotification = true
                )
            }
            
            if (_uiState.value.isSoundEnabled) {
                SynthPlayer.playCorrectSound()
            }
            
            // Auto close notifications
            delay(2500)
            _uiState.update { it.copy(showAdCompleteNotification = false) }
        }
    }

    fun dismissSpinRewardDialog() {
        _uiState.update { it.copy(showSpinRewardDialog = false) }
    }

    fun resetToHome() {
        timerJob?.cancel()
        SynthPlayer.stopMusicLoop()
        _uiState.update {
            it.copy(
                gameState = GameState.HOME,
                selectedAnswer = null,
                highlightCorrectAnswer = null,
                removedWrongAnswers = emptyList(),
                hintTextForCurrentQuestion = null,
                hasUsedFiftyFiftyOnCurrentQuestion = false,
                hasUsedHintOnCurrentQuestion = false
            )
        }
    }

    // --- SUPPORT HELP SYSTEM ---
    fun useFiftyFiftyHelp() {
        if (_uiState.value.gameState != GameState.PLAYING || _uiState.value.selectedAnswer != null) return
        if (_uiState.value.hasUsedFiftyFiftyOnCurrentQuestion) return
        
        viewModelScope.launch {
            val state = _uiState.value
            val currentQuestion = state.questions.getOrNull(state.currentQuestionIndex) ?: return@launch
            
            // Check cost: 50 coins
            val success = repository.deductCoins(50)
            if (success) {
                // Eliminate two wrong answers
                val correct = currentQuestion.correctAnswer // "A", "B", "C", "D"
                val allOptions = listOf("A", "B", "C", "D")
                val wrongOptions = allOptions.filter { it != correct }
                val eliminated = wrongOptions.shuffled().take(2)
                
                _uiState.update {
                    it.copy(
                        removedWrongAnswers = eliminated,
                        hasUsedFiftyFiftyOnCurrentQuestion = true
                    )
                }
                
                if (state.isSoundEnabled) {
                    SynthPlayer.playButtonClickSound()
                }
            } else {
                // Trigger ad or alert
                triggerWatchAdForFiftyFifty()
            }
        }
    }

    fun triggerWatchAdForFiftyFifty() {
        val state = _uiState.value
        if (state.userStats?.isAdsRemoved == true) {
            grantFiftyFiftyDirectly()
            return
        }
        
        _uiState.update {
            it.copy(
                isWatchingAd = true,
                adSecondsRemaining = 5
            )
        }

        viewModelScope.launch {
            while (_uiState.value.adSecondsRemaining > 0) {
                delay(1000)
                _uiState.update { it.copy(adSecondsRemaining = it.adSecondsRemaining - 1) }
            }
            _uiState.update { it.copy(isWatchingAd = false) }
            grantFiftyFiftyDirectly()
            
            if (_uiState.value.isSoundEnabled) {
                SynthPlayer.playCorrectSound()
            }
        }
    }

    private fun grantFiftyFiftyDirectly() {
        val state = _uiState.value
        val currentQuestion = state.questions.getOrNull(state.currentQuestionIndex) ?: return
        val correct = currentQuestion.correctAnswer
        val allOptions = listOf("A", "B", "C", "D")
        val wrongOptions = allOptions.filter { it != correct }
        val eliminated = wrongOptions.shuffled().take(2)
        _uiState.update {
            it.copy(
                removedWrongAnswers = eliminated,
                hasUsedFiftyFiftyOnCurrentQuestion = true,
                showAdCompleteNotification = true
            )
        }
        viewModelScope.launch {
            delay(2000)
            _uiState.update { it.copy(showAdCompleteNotification = false) }
        }
    }

    fun useHintHelp() {
        if (_uiState.value.gameState != GameState.PLAYING || _uiState.value.selectedAnswer != null) return
        if (_uiState.value.hasUsedHintOnCurrentQuestion) return
        
        viewModelScope.launch {
            val state = _uiState.value
            val currentQuestion = state.questions.getOrNull(state.currentQuestionIndex) ?: return@launch
            
            val success = repository.useHintUnit()
            if (success) {
                grantHintDirectly(currentQuestion)
            } else {
                triggerWatchAdForHint()
            }
        }
    }

    private fun triggerWatchAdForHint() {
        val state = _uiState.value
        if (state.userStats?.isAdsRemoved == true) {
            val currentQuestion = state.questions.getOrNull(state.currentQuestionIndex) ?: return
            grantHintDirectly(currentQuestion)
            return
        }
        _uiState.update {
            it.copy(
                isWatchingAd = true,
                adSecondsRemaining = 5
            )
        }

        viewModelScope.launch {
            while (_uiState.value.adSecondsRemaining > 0) {
                delay(1000)
                _uiState.update { it.copy(adSecondsRemaining = it.adSecondsRemaining - 1) }
            }
            _uiState.update { it.copy(isWatchingAd = false) }
            val currentQuestion = _uiState.value.questions.getOrNull(_uiState.value.currentQuestionIndex) ?: return@launch
            grantHintDirectly(currentQuestion)
        }
    }

    private fun grantHintDirectly(currentQuestion: Question) {
        val correct = currentQuestion.correctAnswer
        val correctText = when (correct) {
            "A" -> currentQuestion.optionA
            "B" -> currentQuestion.optionB
            "C" -> currentQuestion.optionC
            else -> currentQuestion.optionD
        }
        val hintText = "تنبيه العبقري: الإجابة تبدأ بـ '${correctText.trim().firstOrNull() ?: '?'}' وتتكون من ${correctText.trim().length} أحرف!"
        _uiState.update {
            it.copy(
                hintTextForCurrentQuestion = hintText,
                hasUsedHintOnCurrentQuestion = true,
                showAdCompleteNotification = true
            )
        }
        viewModelScope.launch {
            delay(2000)
            _uiState.update { it.copy(showAdCompleteNotification = false) }
        }
    }

    fun useShowCorrectHelp() {
        if (_uiState.value.gameState != GameState.PLAYING || _uiState.value.selectedAnswer != null) return
        
        viewModelScope.launch {
            val state = _uiState.value
            val currentQuestion = state.questions.getOrNull(state.currentQuestionIndex) ?: return@launch
            
            val success = repository.deductCoins(100)
            if (success) {
                selectAnswer(currentQuestion.correctAnswer)
            } else {
                _uiState.update {
                    it.copy(
                        showSpinRewardDialog = true,
                        spinRewardTitle = "عذراً!",
                        spinRewardCoins = 0,
                        spinRewardSkips = -2
                    )
                }
            }
        }
    }

    // --- ADS SYSTEM INTERACTIVE REWARDS ---
    fun triggerWatchAdForCoins() {
        val state = _uiState.value
        if (state.userStats?.isAdsRemoved == true) {
            viewModelScope.launch {
                repository.addCoins(100)
                _uiState.update {
                    it.copy(
                        showAdCompleteNotification = true,
                        spinRewardTitle = "مكافأة مميزة! 🎉",
                        spinRewardCoins = 100,
                        showSpinRewardDialog = true
                    )
                }
            }
            return
        }
        
        _uiState.update {
            it.copy(
                isWatchingAd = true,
                adSecondsRemaining = 5
            )
        }

        viewModelScope.launch {
            while (_uiState.value.adSecondsRemaining > 0) {
                delay(1000)
                _uiState.update { it.copy(adSecondsRemaining = it.adSecondsRemaining - 1) }
            }
            _uiState.update { it.copy(isWatchingAd = false) }
            repository.addCoins(100)
            _uiState.update {
                it.copy(
                    showAdCompleteNotification = true,
                    spinRewardTitle = "عملة مجانية! 🎉",
                    spinRewardCoins = 100,
                    showSpinRewardDialog = true
                )
            }
            delay(2000)
            _uiState.update { it.copy(showAdCompleteNotification = false) }
        }
    }

    // --- SHOP SYSTEM ACTIONS ---
    fun buyRemoveAds(useCoins: Boolean) {
        viewModelScope.launch {
            val result = repository.purchaseRemoveAds(useCoins)
            if (result) {
                if (useCoins) {
                    _uiState.update {
                        it.copy(
                            showSpinRewardDialog = true,
                            spinRewardTitle = "تم تفعيل إزالة الإعلانات! 🚫",
                            spinRewardCoins = 0,
                            spinRewardSkips = 0
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            showSpinRewardDialog = true,
                            spinRewardTitle = "عملية شراء ناجحة! $2.99 💸",
                            spinRewardCoins = 0,
                            spinRewardSkips = 0
                        )
                    }
                }
            } else {
                _uiState.update {
                    it.copy(
                        showSpinRewardDialog = true,
                        spinRewardTitle = "عملات غير كافية لفك الإعلانات (5000 🪙)",
                        spinRewardCoins = 0,
                        spinRewardSkips = -3
                    )
                }
            }
        }
    }

    fun buyCoinsPack(coinsAmount: Int, costDollars: Double) {
        viewModelScope.launch {
            repository.addCoins(coinsAmount)
            _uiState.update {
                it.copy(
                    showSpinRewardDialog = true,
                    spinRewardTitle = "تم تفعيل باقة الذهب العباقرة! 🪙",
                    spinRewardCoins = coinsAmount,
                    spinRewardSkips = 0
                )
            }
        }
    }

    fun buyHintsPack(hintsAmount: Int, costCoins: Int) {
        viewModelScope.launch {
            val success = repository.purchaseHints(hintsAmount, costCoins)
            if (success) {
                _uiState.update {
                    it.copy(
                        showSpinRewardDialog = true,
                        spinRewardTitle = "تم شراء حزمة تلميحات العباقرة! 💡",
                        spinRewardCoins = 0,
                        spinRewardSkips = hintsAmount
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        showSpinRewardDialog = true,
                        spinRewardTitle = "عملات غير كافية لشراء التلميحات",
                        spinRewardCoins = 0,
                        spinRewardSkips = -3
                    )
                }
            }
        }
    }

    // --- FRIEND SYSTEM ACTIONS ---
    fun addFriend(uid: String, name: String) {
        viewModelScope.launch {
            val success = repository.addFriend(uid, name)
            if (success) {
                _uiState.update {
                    it.copy(
                        isHelpFeedbackActive = true,
                        helpFeedbackMessage = "تم إضافة الصديق $name بنجاح! 🤝"
                    )
                }
                delay(2000)
                _uiState.update { it.copy(isHelpFeedbackActive = false) }
            } else {
                _uiState.update {
                    it.copy(
                        isHelpFeedbackActive = true,
                        helpFeedbackMessage = "فشلت الإضافة! المعرّف مكرر أو غير صالح."
                    )
                }
                delay(2000)
                _uiState.update { it.copy(isHelpFeedbackActive = false) }
            }
        }
    }

    fun sendHelpRequestToFriend(friendId: String, friendName: String) {
        _uiState.update {
            it.copy(
                isHelpFeedbackActive = true,
                helpFeedbackMessage = "جاري إرسال طلب مساعدة إلى $friendName..."
            )
        }
        viewModelScope.launch {
            delay(1500)
            val coinReward = 50
            repository.addCoins(coinReward)
            _uiState.update {
                it.copy(
                    helpFeedbackMessage = "استجاب $friendName لطلبك وأرسل لك 50 🪙 كهدية! 💖"
                )
            }
            delay(2500)
            _uiState.update { it.copy(isHelpFeedbackActive = false) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        try {
            SynthPlayer.stopMusicLoop()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
