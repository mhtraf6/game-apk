package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.Question
import com.example.data.model.UserStats
import com.example.data.model.LeaderboardEntry
import com.example.ui.GameState
import com.example.ui.QuizUiState
import com.example.ui.QuizViewModel
import com.example.ui.SynthPlayer
import kotlinx.coroutines.launch
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import com.example.ui.AppVibrator
import com.example.ui.theme.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class ConfettiParticle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var color: Color,
    var size: Float,
    var alpha: Float = 1.0f,
    var rotation: Float = 0f,
    var rotationSpeed: Float = 0f
)

@Composable
fun QuizConfetti(
    trigger: Boolean,
    modifier: Modifier = Modifier
) {
    var particles by remember { mutableStateOf(emptyList<ConfettiParticle>()) }
    val random = remember { java.util.Random() }
    
    LaunchedEffect(trigger) {
        if (trigger) {
            val colors = listOf(
                Color(0xFFFFD700), // Gold
                Color(0xFF00ADB5), // Turquoise
                Color(0xFF2ECC71), // Green
                Color(0xFFF06292), // Pink
                Color(0xFF9B59B6), // Purple
                Color(0xFF3498DB)  // Blue
            )
            val newParticles = List(70) {
                val angle = random.nextFloat() * 2f * PI.toFloat()
                val speed = 8f + random.nextFloat() * 16f
                ConfettiParticle(
                    x = 540f,
                    y = 700f,
                    vx = cos(angle) * speed,
                    vy = sin(angle) * speed - 8f,
                    color = colors[random.nextInt(colors.size)],
                    size = 12f + random.nextFloat() * 20f,
                    rotation = random.nextFloat() * 360f,
                    rotationSpeed = -6f + random.nextFloat() * 12f
                )
            }
            particles = newParticles
        }
    }

    if (particles.isNotEmpty()) {
        LaunchedEffect(Unit) {
            while (true) {
                withFrameMillis { _ ->
                    particles = particles.map { p ->
                        p.x += p.vx
                        p.y += p.vy
                        p.vy += 0.35f
                        p.vx *= 0.98f
                        p.alpha = (p.alpha - 0.012f).coerceIn(0f, 1f)
                        p.rotation += p.rotationSpeed
                        p
                    }.filter { it.alpha > 0f }
                }
                if (particles.isEmpty()) break
            }
        }

        Canvas(modifier = modifier.fillMaxSize()) {
            particles.forEach { p ->
                drawContext.canvas.save()
                drawContext.canvas.translate(p.x, p.y)
                drawContext.canvas.rotate(p.rotation)
                drawRect(
                    color = p.color.copy(alpha = p.alpha),
                    size = androidx.compose.ui.geometry.Size(p.size, p.size / 2f),
                    topLeft = androidx.compose.ui.geometry.Offset(-p.size / 2f, -p.size / 4f)
                )
                drawContext.canvas.restore()
            }
        }
    }
}

class FlyingCoin(
    var x: Float,
    var y: Float,
    val targetX: Float,
    val targetY: Float,
    var progress: Float = 0f,
    val speed: Float,
    var delayFrames: Int,
    var rotation: Float = 0f,
    val rSpeed: Float
)

@Composable
fun FlyingCoinsEffect(
    trigger: Boolean,
    startX: Float,
    startY: Float,
    targetX: Float,
    targetY: Float,
    modifier: Modifier = Modifier
) {
    var coins by remember { mutableStateOf(emptyList<FlyingCoin>()) }
    val random = remember { java.util.Random() }

    LaunchedEffect(trigger) {
        if (trigger) {
            val list = List(12) { i ->
                FlyingCoin(
                    x = startX,
                    y = startY,
                    targetX = targetX,
                    targetY = targetY,
                    speed = 0.022f + random.nextFloat() * 0.012f,
                    delayFrames = i * 4,
                    rSpeed = -12f + random.nextFloat() * 24f
                )
            }
            coins = list
        }
    }

    if (coins.isNotEmpty()) {
        LaunchedEffect(Unit) {
            while (true) {
                withFrameMillis { _ ->
                    coins = coins.map { c ->
                        if (c.delayFrames > 0) {
                            c.delayFrames--
                        } else if (c.progress < 1f) {
                            c.progress = (c.progress + c.speed).coerceAtMost(1f)
                            val t = c.progress
                            val currentX = (1f - t) * startX + t * targetX + sin(t * PI.toFloat()) * 180f
                            val currentY = (1f - t) * startY + t * targetY - sin(t * PI.toFloat()) * 120f
                            c.x = currentX
                            c.y = currentY
                            c.rotation += c.rSpeed
                        }
                        c
                    }.filter { it.progress < 1f }
                }
                if (coins.isEmpty()) break
            }
        }

        Canvas(modifier = modifier.fillMaxSize()) {
            coins.forEach { c ->
                if (c.delayFrames <= 0) {
                    drawContext.canvas.save()
                    drawContext.canvas.translate(c.x, c.y)
                    drawContext.canvas.rotate(c.rotation)
                    drawCircle(
                        color = Color(0xFFFFD700),
                        radius = 16f
                    )
                    drawCircle(
                        color = Color(0xFFE6B800),
                        radius = 12f
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 6f
                    )
                    drawContext.canvas.restore()
                }
            }
        }
    }
}

fun Modifier.shimmerShine(
    trigger: Int,
    shineColor: Color = Color.White.copy(alpha = 0.6f)
): Modifier = this.drawWithContent {
    drawContent()
}

@Composable
fun Modifier.shimmerShineActive(
    trigger: Int,
    shineColor: Color = Color.White.copy(alpha = 0.6f)
): Modifier {
    var animateTrigger by remember { mutableStateOf(0) }
    val progress = remember { Animatable(0f) }

    LaunchedEffect(trigger) {
        if (trigger > 0 && animateTrigger > 0) {
            progress.snapTo(0f)
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(700, easing = FastOutSlowInEasing)
            )
        }
        animateTrigger = trigger
    }

    if (progress.value == 0f || progress.value == 1f) {
        return this
    }

    return this.drawWithContent {
        drawContent()
        val width = size.width
        val height = size.height
        val x = width * progress.value * 1.5f - (width * 0.25f)
        
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.Transparent,
                    shineColor.copy(alpha = 0.15f),
                    shineColor,
                    shineColor.copy(alpha = 0.15f),
                    Color.Transparent
                ),
                start = androidx.compose.ui.geometry.Offset(x, 0f),
                end = androidx.compose.ui.geometry.Offset(x + width * 0.25f, height)
            )
        )
    }
}

@Composable
fun Modifier.neon3DClickable(
    enabled: Boolean = true,
    shadowColor: Color = Color(0xFF0F111A),
    glowColor: Color = SupportTeal,
    isGlowing: Boolean = true,
    shapeRadius: Int = 16,
    onClick: () -> Unit
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val pressProg by animateFloatAsState(
        targetValue = if (isPressed) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 300f),
        label = "PressProg3D"
    )
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 400f),
        label = "ScaleProg3D"
    )

    LaunchedEffect(isPressed) {
        if (isPressed) {
            AppVibrator.vibrateClick()
        }
    }

    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
            translationY = pressProg * 4f
        }
        .then(
            if (isGlowing) {
                Modifier.shadow(
                    elevation = if (isPressed) 2.dp else 8.dp,
                    shape = RoundedCornerShape(shapeRadius.dp),
                    ambientColor = glowColor,
                    spotColor = glowColor
                )
            } else Modifier
        )
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onClick = {
                AppVibrator.vibrateClick()
                onClick()
            }
        )
}

@Composable
fun AbqariNeon3DButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    baseColor: Color = Color(0xFF0F111A),
    faceColor: Color = Color(0xFF1E2235),
    faceBrush: Brush? = null,
    glowColor: Color = SupportTeal,
    glowAlpha: Float = 0.35f,
    cornerRadius: Int = 15,
    content: @Composable BoxScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val pressProgress by animateFloatAsState(
        targetValue = if (isPressed) 1f else 0f,
        animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
        label = "PressAnim3D"
    )
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
        label = "ButtonScale3D"
    )

    LaunchedEffect(isPressed) {
        if (isPressed) {
            AppVibrator.vibrateClick()
        }
    }

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = {
                    AppVibrator.vibrateClick()
                    onClick()
                }
            )
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(y = 5.dp)
                .graphicsLayer {
                    scaleY = 1f - (pressProgress * 0.4f)
                    alpha = 1f - (pressProgress * 0.2f)
                }
                .background(
                    color = baseColor.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(cornerRadius.dp)
                )
        )

        val faceModifier = if (faceBrush != null) {
            Modifier.background(brush = faceBrush, shape = RoundedCornerShape(cornerRadius.dp))
        } else {
            Modifier.background(color = faceColor, shape = RoundedCornerShape(cornerRadius.dp))
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = (5 * pressProgress).dp)
                .then(faceModifier)
                .border(
                    width = 1.2.dp,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            glowColor,
                            glowColor.copy(alpha = 0.4f),
                            glowColor
                        )
                    ),
                    shape = RoundedCornerShape(cornerRadius.dp)
                )
                .padding(horizontal = 14.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center,
            content = content
        )
    }
}

@Composable
fun QuizGameApp(viewModel: QuizViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    // Force Right-to-Left (RTL) Layout for strict Arabic support, regardless of system locale
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = DarkBg
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AnimatedContent(
                    targetState = uiState.gameState,
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(400)) + scaleIn(initialScale = 0.94f, animationSpec = tween(400)))
                            .togetherWith(fadeOut(animationSpec = tween(250)))
                    },
                    label = "game_state_transition"
                ) { state ->
                    when (state) {
                        GameState.HOME -> HomeScreen(
                            uiState = uiState,
                            onCategorySelect = { viewModel.selectCategory(it) },
                            onLevelSelect = { viewModel.selectLevel(it) },
                            onStartGame = { viewModel.startGame() },
                            onToggleSound = { viewModel.toggleSound() },
                            onNavigateToLeaderboard = { viewModel.navigateToLeaderboard() },
                            onNavigateToSpinWheel = { viewModel.navigateToSpinWheel() },
                            onNavigateToShop = { viewModel.navigateToShop() },
                            onNavigateToFriends = { viewModel.navigateToFriends() }
                        )
                        GameState.LOADING -> LoadingScreen()
                        GameState.PLAYING -> QuizScreen(
                            uiState = uiState,
                            onAnswerSelect = { viewModel.selectAnswer(it) },
                            onExitQuiz = { viewModel.resetToHome() },
                            onSkipClick = { viewModel.skipCurrentQuestion() },
                            onFiftyFiftyClick = { viewModel.useFiftyFiftyHelp() },
                            onHintClick = { viewModel.useHintHelp() },
                            onShowCorrectClick = { viewModel.useShowCorrectHelp() },
                            onToggleSound = { viewModel.toggleSound() },
                            onToggleMusic = { viewModel.toggleMusic() }
                        )
                        GameState.RESULTS -> ResultsScreen(
                            uiState = uiState,
                            onRestart = { viewModel.startGame() },
                            onGoHome = { viewModel.resetToHome() }
                        )
                        GameState.LEADERBOARD -> LeaderboardScreen(
                            uiState = uiState,
                            onBackHome = { viewModel.resetToHome() }
                        )
                        GameState.SPIN_WHEEL -> SpinWheelScreen(
                            uiState = uiState,
                            onBackHome = { viewModel.resetToHome() },
                            onSpinClick = { viewModel.spinWheel() },
                            onWatchAdClick = { viewModel.triggerWatchAdForSpin() },
                            isSpinClaimable = viewModel.isDailySpinClaimable(),
                            remainingTimeStr = viewModel.getDailySpinRemainingTimeString()
                        )
                        GameState.SHOP -> ShopScreen(
                            uiState = uiState,
                            onBackHome = { viewModel.resetToHome() },
                            onBuyRemoveAds = { useCoins -> viewModel.buyRemoveAds(useCoins) },
                            onBuyCoinsPack = { coins, price -> viewModel.buyCoinsPack(coins, price) },
                            onBuyHintsPack = { hints, price -> viewModel.buyHintsPack(hints, price) },
                            onWatchAdForCoins = { viewModel.triggerWatchAdForCoins() }
                        )
                        GameState.FRIENDS -> FriendsScreen(
                            uiState = uiState,
                            onBackHome = { viewModel.resetToHome() },
                            onAddFriend = { uid, name -> viewModel.addFriend(uid, name) },
                            onHelpRequestClick = { uid, name -> viewModel.sendHelpRequestToFriend(uid, name) }
                        )
                    }
                }

                // Global Mocked Video Ad Overlay Integration
                if (uiState.isWatchingAd) {
                    VideoAdSimulateOverlay(
                        secondsRemaining = uiState.adSecondsRemaining
                    )
                }

                // Global dialogs for Spin rewards
                if (uiState.showSpinRewardDialog) {
                    RewardDialog(
                        title = uiState.spinRewardTitle,
                        coinsWon = uiState.spinRewardCoins,
                        skipsWon = uiState.spinRewardSkips,
                        onDismiss = { viewModel.dismissSpinRewardDialog() }
                    )
                }

                // Global level up celebration dialog
                if (uiState.showLevelUpDialog) {
                    LevelUpCelebrationDialog(
                        levelTitle = uiState.levelUpTitleState,
                        roleName = uiState.levelUpRoleState,
                        onDismiss = { viewModel.dismissLevelUpDialog() }
                    )
                }
            }
        }
    }
}

// 👑 UNIVERSAL TOP BAR STATUS HEADER (Reusable component)
@Composable
fun TopStatsHeader(
    coins: Int,
    skips: Int,
    isSoundEnabled: Boolean,
    isMusicEnabled: Boolean = true,
    onToggleSound: () -> Unit = {},
    onToggleMusic: () -> Unit = {},
    showBackButton: Boolean = false,
    onBackClick: () -> Unit = {}
) {
    val coinScale = remember { Animatable(1f) }
    LaunchedEffect(coins) {
        if (coins > 0) {
            coinScale.animateTo(1.3f, animationSpec = spring(dampingRatio = 0.4f, stiffness = 400f))
            coinScale.animateTo(1.0f, animationSpec = spring(dampingRatio = 0.5f, stiffness = 300f))
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (showBackButton) {
                IconButton(
                    onClick = {
                        SynthPlayer.playButtonClickSound()
                        onBackClick()
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFF1F2232), CircleShape)
                        .testTag("app_header_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "رجوع",
                        tint = TextWhite,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
            }

            // Glowing Coin Display Widget
            Row(
                modifier = Modifier
                    .graphicsLayer(scaleX = coinScale.value, scaleY = coinScale.value)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF231C0E))
                    .border(1.2.dp, PrimaryGold, RoundedCornerShape(12.dp))
                    .shimmerShineActive(coins)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = "النقود",
                    tint = PrimaryGold,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "$coins 🪙",
                    color = PrimaryGold,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Skips Badge Display Widget
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF142429))
                    .border(1.dp, SupportTeal.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                    contentDescription = "تخطي مجاني",
                    tint = SupportTeal,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "$skips تذاكر",
                    color = SupportTeal,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Music Toggle Button Widget
            IconButton(
                onClick = {
                    SynthPlayer.playButtonClickSound()
                    onToggleMusic()
                },
                modifier = Modifier
                    .background(Color(0xFF1E2130), CircleShape)
                    .size(40.dp)
                    .testTag("music_toggle_top_bar")
            ) {
                Icon(
                    imageVector = if (isMusicEnabled) Icons.Filled.MusicNote else Icons.Filled.MusicOff,
                    contentDescription = "كتم الموسيقى",
                    tint = PrimaryGold,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Sound Toggle Button Widget
            IconButton(
                onClick = {
                    SynthPlayer.playButtonClickSound()
                    onToggleSound()
                },
                modifier = Modifier
                    .background(Color(0xFF1E2130), CircleShape)
                    .size(40.dp)
                    .testTag("sound_toggle_top_bar")
            ) {
                Icon(
                    imageVector = if (isSoundEnabled) Icons.Filled.NotificationsActive else Icons.Filled.NotificationsOff,
                    contentDescription = "كتم الصوت",
                    tint = SupportTeal,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// 🏠 1. HOME SCREEN (Highly Premium Arabic RTL Layout)
@Composable
fun HomeScreen(
    uiState: QuizUiState,
    onCategorySelect: (String) -> Unit,
    onLevelSelect: (String) -> Unit,
    onStartGame: () -> Unit,
    onToggleSound: () -> Unit,
    onNavigateToLeaderboard: () -> Unit,
    onNavigateToSpinWheel: () -> Unit,
    onNavigateToShop: () -> Unit,
    onNavigateToFriends: () -> Unit
) {
    val scrollState = rememberScrollState()
    val totalCorrect = uiState.userStats?.totalCorrectAnswers ?: 0
    val coinsCount = uiState.userStats?.coins ?: 150
    val skipsCount = uiState.userStats?.totalSkipsAvailable ?: 3

    // Determine current Level Progress details
    val levelDetails = calculateLevelDetails(totalCorrect)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        DarkBg,
                        Color(0xFF13152B),
                        Color(0xFF0F111A)
                    )
                )
            )
            .padding(14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            // Header Stats block
            TopStatsHeader(
                coins = coinsCount,
                skips = skipsCount,
                isSoundEnabled = uiState.isSoundEnabled,
                onToggleSound = onToggleSound
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Game Logo Widget card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF181B2E)),
                shape = RoundedCornerShape(26.dp),
                border = BorderStroke(1.2.dp, SupportTeal.copy(alpha = 0.4f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(105.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        SupportTeal.copy(alpha = 0.25f),
                                        Color.Transparent
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        // Drawing interactive beautiful vector Brain logo safely loaded or styled
                        Canvas(modifier = Modifier.size(65.dp)) {
                            // Brain left side
                            drawCircle(
                                color = SupportTeal,
                                radius = size.minDimension / 3.5f,
                                center = center.copy(x = center.x - size.width * 0.08f),
                                style = Stroke(width = 4.dp.toPx())
                            )
                            // Brain right side
                            drawCircle(
                                color = PrimaryGold,
                                radius = size.minDimension / 3.5f,
                                center = center.copy(x = center.x + size.width * 0.08f),
                                style = Stroke(width = 4.dp.toPx())
                            )
                            // Brain connector
                            drawRect(
                                color = TextWhite,
                                size = size.copy(width = 12.dp.toPx(), height = 18.dp.toPx()),
                                topLeft = center.copy(x = center.x - 6.dp.toPx(), y = center.y - 9.dp.toPx())
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = stringResource(id = R.string.app_name),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        fontSize = 28.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "طوّر ذكاءك واكتسب مهارات يومية!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 🆙 ADVANCED LEVEL SYSTEM PROGRESS BADGE PANEL
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2235)),
                border = BorderStroke(1.dp, Color(0xFF2C324D))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "مستوى عبقريتك الحالي:",
                            color = TextMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(PrimaryGold.copy(alpha = 0.15f))
                                    .border(1.dp, PrimaryGold.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = levelDetails.levelTitle,
                                    color = PrimaryGold,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 15.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "(${levelDetails.roleName})",
                                color = TextWhite,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Progress to next rank
                        val percentage = levelDetails.progressPercentage
                        Text(
                            text = "التقدم للمستوى التالي: ${levelDetails.current} / ${levelDetails.target}",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { percentage },
                            color = SupportTeal,
                            trackColor = Color(0xFF2C3149),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Circular graphic score badge
                    Box(
                        modifier = Modifier.size(64.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            progress = { levelDetails.progressPercentage },
                            color = PrimaryGold,
                            trackColor = Color(0xFF2C3149),
                            strokeWidth = 5.dp,
                            modifier = Modifier.fillMaxSize()
                        )
                        Text(
                            text = "${(levelDetails.progressPercentage * 100).toInt()}%",
                            color = TextWhite,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 🎡 REWARD HUBS SHORTCUTS (LEADERBOARD & DAILY SPIN WHEEL)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // DAILY SPIN ACTION CARD
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateToSpinWheel() },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF201835)),
                    border = BorderStroke(1.2.dp, Color(0xFF5D3F9F))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Casino,
                            contentDescription = "دولاب الحظ",
                            tint = Color(0xFF9E7CFA),
                            modifier = Modifier.size(34.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "دولاب الحظ",
                            color = TextWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "اربح جوائز قيمة",
                            color = TextMuted,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // LEADERBOARD ACTION CARD
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateToLeaderboard() },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF142426)),
                    border = BorderStroke(1.2.dp, SupportTeal.copy(alpha = 0.6f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Leaderboard,
                            contentDescription = "لوحة المتصدرين",
                            tint = SupportTeal,
                            modifier = Modifier.size(34.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "المتصدرون",
                            color = TextWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "الترتيب المحلي",
                            color = TextMuted,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 🛒 SHOP & 👥 FRIENDS SHORTCUTS ROW
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // SHOP ACTION CARD
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateToShop() }
                        .testTag("home_shop_button"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2C1E1B)),
                    border = BorderStroke(1.2.dp, Color(0xFFC27E3A))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ShoppingCart,
                            contentDescription = "متجر العباقرة",
                            tint = Color(0xFFF9A634),
                            modifier = Modifier.size(34.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "متجر العباقرة",
                            color = TextWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "تذاكر، تلميحات وذهب",
                            color = TextMuted,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // FRIENDS ACTION CARD
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateToFriends() }
                        .testTag("home_friends_button"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF142435)),
                    border = BorderStroke(1.2.dp, Color(0xFF2196F3).copy(alpha = 0.6f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Filled.People,
                            contentDescription = "مجتمع الأصدقاء",
                            tint = Color(0xFF4AC3FF),
                            modifier = Modifier.size(34.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "الأصدقاء",
                            color = TextWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "طلب مساعدات وتبادل ذهب",
                            color = TextMuted,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Categories list selection UI
            Text(
                text = stringResource(id = R.string.select_category),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                textAlign = TextAlign.Start
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                contentPadding = PaddingValues(horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(uiState.categories) { catKey ->
                    CategoryItemCard(
                        categoryKey = catKey,
                        isSelected = uiState.selectedCategory == catKey,
                        onSelect = { onCategorySelect(catKey) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Levels list selection UI
            Text(
                text = stringResource(id = R.string.select_level),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                textAlign = TextAlign.Start
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                uiState.levels.forEach { levelKey ->
                    val isSelected = uiState.selectedLevel == levelKey
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (isSelected) SupportTeal else Color(0xFF1E2235)
                            )
                            .border(1.dp, if (isSelected) SupportTeal else Color(0xFF2E324A), RoundedCornerShape(14.dp))
                            .clickable { onLevelSelect(levelKey) }
                            .padding(vertical = 12.dp)
                            .testTag("level_${levelKey.lowercase()}_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = getArabicLevelName(levelKey),
                            color = if (isSelected) Color.White else TextWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Start Game Pulsating CTA action Button with modern 3D design
            val playButtonBrush = Brush.horizontalGradient(
                colors = listOf(
                    Color(0xFF2575FC), // Vibrant Blue
                    Color(0xFF6A11CB)  // Royal Purple
                )
            )
            AbqariNeon3DButton(
                onClick = onStartGame,
                faceBrush = playButtonBrush,
                glowColor = SupportTeal,
                cornerRadius = 15,
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(60.dp)
                    .testTag("start_game_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "ابدأ",
                        modifier = Modifier.size(26.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(id = R.string.start_game),
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(35.dp))
        }
    }
}

// ⏱️ 2. QUIZ SCREEN (Interactive Q&A Quiz Screen)
@Composable
fun QuizScreen(
    uiState: QuizUiState,
    onAnswerSelect: (String) -> Unit,
    onExitQuiz: () -> Unit,
    onSkipClick: () -> Unit,
    onFiftyFiftyClick: () -> Unit,
    onHintClick: () -> Unit,
    onShowCorrectClick: () -> Unit,
    onToggleSound: () -> Unit = {},
    onToggleMusic: () -> Unit = {}
) {
    val currentQuestion = uiState.questions.getOrNull(uiState.currentQuestionIndex)

    if (currentQuestion == null) {
        LoadingScreen()
        return
    }

    val progress = (uiState.currentQuestionIndex + 1).toFloat() / uiState.questions.size
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 150f),
        label = "QuestionProgressBarAnim"
    )
    val totalPlayedCoins = uiState.userStats?.coins ?: 150
    val totalPlayedSkips = uiState.userStats?.totalSkipsAvailable ?: 3
    val coroutineScope = rememberCoroutineScope()

    var triggerConfetti by remember { mutableStateOf(false) }
    var triggerCoinsFly by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.selectedAnswer) {
        if (uiState.selectedAnswer != null) {
            val isCorrect = uiState.selectedAnswer == uiState.highlightCorrectAnswer
            if (isCorrect) {
                triggerConfetti = true
                triggerCoinsFly = true
                kotlinx.coroutines.delay(150)
                triggerConfetti = false
                triggerCoinsFly = false
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBg)
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Stats Top row during play state
            TopStatsHeader(
                coins = totalPlayedCoins,
                skips = totalPlayedSkips,
                isSoundEnabled = uiState.isSoundEnabled,
                isMusicEnabled = uiState.isMusicEnabled,
                onToggleSound = onToggleSound,
                onToggleMusic = onToggleMusic,
                showBackButton = true,
                onBackClick = onExitQuiz
            )

            Spacer(modifier = Modifier.height(8.dp))
        }

        // Animated transitions of question context
        AnimatedContent(
            targetState = uiState.currentQuestionIndex,
            transitionSpec = {
                slideInHorizontally { width -> width / 2 } + fadeIn(animationSpec = tween(300)) togetherWith
                        slideOutHorizontally { width -> -width / 2 } + fadeOut(animationSpec = tween(300))
            },
            modifier = Modifier.weight(1f),
            label = "QuestionTransition"
        ) { targetIndex ->
            val activeQuestion = uiState.questions.getOrNull(targetIndex)
            if (activeQuestion != null) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Progress labels
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(
                                id = R.string.question_counter,
                                targetIndex + 1,
                                uiState.questions.size
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted
                        )

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF1E2845))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = getArabicLevelName(activeQuestion.level),
                                style = MaterialTheme.typography.labelSmall,
                                color = getLevelColor(activeQuestion.level),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape),
                        color = SupportTeal,
                        trackColor = Color(0xFF2C324D)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // ⏱️ TIMER BAR DISPLAY (Smooth 100ms ticker updates visually)
                    val timerPercentage = uiState.timeLeftSeconds / uiState.maxTimeSeconds
                    val timerColor = when {
                        timerPercentage > 0.6f -> SupportTeal
                        timerPercentage > 0.3f -> PrimaryGold
                        else -> IncorrectRed
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "الوقت المتبقي لليقظة:",
                            color = TextMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = String.format("%.1f ثانية", uiState.timeLeftSeconds),
                            color = timerColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LinearProgressIndicator(
                        progress = { timerPercentage },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(CircleShape),
                        color = timerColor,
                        trackColor = Color(0xFF1E2235)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Large Question Presentation Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF181B2E)),
                        border = BorderStroke(1.2.dp, Color(0xFF2E3555)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = activeQuestion.questionText,
                                style = MaterialTheme.typography.headlineMedium.copy(lineHeight = 34.sp),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 21.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Hint text box display if active
                    uiState.hintTextForCurrentQuestion?.let { hint ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF2C241B)),
                            border = BorderStroke(1.dp, PrimaryGold.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Lightbulb,
                                    contentDescription = "تلميح العبقري",
                                    tint = PrimaryGold,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = hint,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // Answer selections mapping: Option A, B, C, D
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val selectionOptions = listOf(
                            Pair("A", activeQuestion.optionA),
                            Pair("B", activeQuestion.optionB),
                            Pair("C", activeQuestion.optionC),
                            Pair("D", activeQuestion.optionD)
                        )

                        selectionOptions.forEach { (optionCode, optionText) ->
                            if (uiState.removedWrongAnswers.contains(optionCode)) {
                                return@forEach
                            }
                            val isSelected = uiState.selectedAnswer == optionCode
                            val isCorrect = uiState.highlightCorrectAnswer == optionCode

                            val optionFaceBrush = when {
                                uiState.selectedAnswer == null -> {
                                     Brush.horizontalGradient(
                                         colors = listOf(
                                             Color(0xFF2575FC), // Vibrant Blue
                                             Color(0xFF6A11CB)  // Royal Purple
                                         )
                                     )
                                 }
                                 isSelected && isCorrect -> {
                                     Brush.horizontalGradient(
                                         colors = listOf(
                                             Color(0xFF11998E),
                                             Color(0xFF38EF7D)
                                         )
                                     )
                                 }
                                 isSelected && !isCorrect -> {
                                     Brush.horizontalGradient(
                                         colors = listOf(
                                             Color(0xFFCB2D3E),
                                             Color(0xFFEF473A)
                                         )
                                     )
                                 }
                                 uiState.selectedAnswer != null && isCorrect -> {
                                     Brush.horizontalGradient(
                                         colors = listOf(
                                             Color(0xFF11998E),
                                             Color(0xFF38EF7D)
                                         )
                                     )
                                 }
                                 else -> null
                             }

                             val optionFaceColor = if (optionFaceBrush == null) {
                                 Color(0xFF151824).copy(alpha = 0.35f)
                             } else {
                                 Color.Transparent
                             }

                            val isAnySelected = uiState.selectedAnswer != null
                            val targetScale = when {
                                isSelected -> 1.04f
                                isAnySelected -> 0.95f
                                else -> 1.0f
                            }
                            val optionScale by animateFloatAsState(
                                targetValue = targetScale,
                                animationSpec = spring(dampingRatio = 0.5f, stiffness = 450f),
                                label = "OptionScale"
                            )

                            val shakeOffset = remember { Animatable(0f) }
                            LaunchedEffect(uiState.selectedAnswer) {
                                if (isSelected && !isCorrect) {
                                    for (i in 1..3) {
                                        shakeOffset.animateTo(-10f, animationSpec = tween(50))
                                        shakeOffset.animateTo(10f, animationSpec = tween(50))
                                    }
                                    shakeOffset.animateTo(0f, animationSpec = tween(50))
                                }
                            }

                            val coinOffset = remember { Animatable(30f) }
                            val coinAlpha = remember { Animatable(0f) }
                            LaunchedEffect(uiState.selectedAnswer) {
                                if (isSelected && isCorrect) {
                                    coinOffset.snapTo(30f)
                                    coinAlpha.snapTo(1f)
                                    launch {
                                        coinOffset.animateTo(-40f, animationSpec = tween(1000, easing = LinearOutSlowInEasing))
                                    }
                                    launch {
                                        coinAlpha.animateTo(0f, animationSpec = tween(1000, easing = LinearOutSlowInEasing))
                                    }
                                }
                            }

                            val glowColor = when {
                                uiState.selectedAnswer != null && isCorrect -> CorrectGreen
                                isSelected && !isCorrect -> IncorrectRed
                                uiState.selectedAnswer == null -> SupportTeal
                                else -> Color(0xFF2D324F).copy(alpha = 0.4f)
                            }

                            AbqariNeon3DButton(
                                onClick = { onAnswerSelect(optionCode) },
                                enabled = uiState.selectedAnswer == null,
                                glowColor = glowColor,
                                faceBrush = optionFaceBrush,
                                faceColor = optionFaceColor,
                                cornerRadius = 15,
                                baseColor = Color(0xFF0C0E18),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .graphicsLayer(translationX = shakeOffset.value)
                                    .testTag("option_${optionCode.lowercase()}_button")
                            ) {
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 13.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Start
                                    ) {
                                        // Choice Code Emblem
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (isSelected || (uiState.selectedAnswer != null && isCorrect)) {
                                                        Color.Black.copy(alpha = 0.2f)
                                                    } else {
                                                        SupportTeal.copy(alpha = 0.15f)
                                                    }
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = optionCode,
                                                color = if (isSelected || (uiState.selectedAnswer != null && isCorrect)) Color.White else SupportTeal,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 13.sp
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(14.dp))

                                        // Selection text
                                        Text(
                                            text = optionText,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = Color.White,
                                            textAlign = TextAlign.Start,
                                            modifier = Modifier.weight(1f)
                                        )

                                        // feedback icons
                                        if (uiState.selectedAnswer != null) {
                                            if (isCorrect) {
                                                Icon(
                                                    imageVector = Icons.Filled.Check,
                                                    contentDescription = "صحيحة",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            } else if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Filled.Close,
                                                    contentDescription = "خاطئة",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }

                                    if (coinAlpha.value > 0f) {
                                        Text(
                                            text = "+١٠ 🪙",
                                            color = PrimaryGold,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 18.sp,
                                            modifier = Modifier
                                                .align(Alignment.Center)
                                                .offset(y = coinOffset.value.dp)
                                                .graphicsLayer(alpha = coinAlpha.value)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 🔮 ASSIST HELP SYSTEMS ROW
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 50/50 help button
            val ffEnabled = uiState.selectedAnswer == null && !uiState.hasUsedFiftyFiftyOnCurrentQuestion
            AbqariNeon3DButton(
                onClick = onFiftyFiftyClick,
                enabled = ffEnabled,
                glowColor = if (ffEnabled) SupportTeal else Color.Transparent,
                faceColor = if (ffEnabled) Color(0xFF1B2C24) else Color(0xFF181A22),
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
                    .testTag("fifty_fifty_help_button")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Percent,
                        contentDescription = "50/50",
                        modifier = Modifier.size(14.dp),
                        tint = if (ffEnabled) SupportTeal else Color.Gray
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "50/50 (50 🪙)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (ffEnabled) SupportTeal else Color.Gray
                    )
                }
            }

            // Hint help button
            val hintEnabled = uiState.selectedAnswer == null && !uiState.hasUsedHintOnCurrentQuestion
            AbqariNeon3DButton(
                onClick = onHintClick,
                enabled = hintEnabled,
                glowColor = if (hintEnabled) PrimaryGold else Color.Transparent,
                faceColor = if (hintEnabled) Color(0xFF2C241B) else Color(0xFF181A22),
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
                    .testTag("hint_help_button")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Lightbulb,
                        contentDescription = "تلميح",
                        modifier = Modifier.size(14.dp),
                        tint = if (hintEnabled) PrimaryGold else Color.Gray
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "تلميح (30 🪙)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (hintEnabled) PrimaryGold else Color.Gray
                    )
                }
            }

            // Show correct answer help button
            val showCorrectEnabled = uiState.selectedAnswer == null
            val pinkGlow = Color(0xFFF06292)
            AbqariNeon3DButton(
                onClick = onShowCorrectClick,
                enabled = showCorrectEnabled,
                glowColor = if (showCorrectEnabled) pinkGlow else Color.Transparent,
                faceColor = if (showCorrectEnabled) Color(0xFF2C1B24) else Color(0xFF181A22),
                modifier = Modifier
                    .weight(1.3f)
                    .height(46.dp)
                    .testTag("show_correct_help_button")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "كشف الإجابة",
                        modifier = Modifier.size(14.dp),
                        tint = if (showCorrectEnabled) pinkGlow else Color.Gray
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "كشف (100 🪙)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (showCorrectEnabled) pinkGlow else Color.Gray
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // ⏭️ SHOP / SKIP QUESTION OPTION BUTTON
        // ⏭️ SKIP QUESTION OPTION BUTTON
        val skipEnabled = uiState.selectedAnswer == null
        AbqariNeon3DButton(
            onClick = onSkipClick,
            enabled = skipEnabled,
            glowColor = if (skipEnabled) PrimaryGold else Color.Transparent,
            faceColor = if (skipEnabled) Color(0xFF2C240E) else Color(0xFF1B1C22),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .height(50.dp)
                .testTag("skip_question_button")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                    contentDescription = "تخطي السؤال",
                    tint = if (skipEnabled) PrimaryGold else Color.Gray,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "تخطي السؤال (-30 🪙 أو تذكرة)",
                    color = if (skipEnabled) PrimaryGold else Color.Gray,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
        }

        // Overlay visuals
        QuizConfetti(trigger = triggerConfetti)
        FlyingCoinsEffect(
            trigger = triggerCoinsFly,
            startX = 540f,
            startY = 1000f,
            targetX = 140f,
            targetY = 100f
        )
    }
}

// 🎡 3. DAILY SPIN WHEEL (دولاب الحظ)
@Composable
fun SpinWheelScreen(
    uiState: QuizUiState,
    onBackHome: () -> Unit,
    onSpinClick: () -> Unit,
    onWatchAdClick: () -> Unit,
    isSpinClaimable: Boolean,
    remainingTimeStr: String
) {
    val rotationState by animateFloatAsState(
        targetValue = uiState.wheelRotation,
        animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
        label = "spinning_anim_degrees"
    )

    val coinsVal = uiState.userStats?.coins ?: 150
    val skipsVal = uiState.userStats?.totalSkipsAvailable ?: 3
    val canSpinNow = isSpinClaimable || uiState.extraSpinsAvailable > 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // App top row
        TopStatsHeader(
            coins = coinsVal,
            skips = skipsVal,
            isSoundEnabled = uiState.isSoundEnabled,
            onToggleSound = {},
            showBackButton = true,
            onBackClick = onBackHome
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "🎡 دولاب الحظ والعبقرية",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = Color.White,
                fontSize = 24.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "أدر العجلة يومياً مجاناً للفوز بالهدايا الصائبة",
                color = TextMuted,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Drawing custom graphical wheel canvas with pointer
        Box(
            modifier = Modifier
                .size(260.dp)
                .rotate(rotationState),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Slices coloring list
                val colors = listOf(
                    Color(0xFFE74C3C), Color(0xFFF1C40F), Color(0xFF2ECC71), Color(0xFF3498DB),
                    Color(0xFF9B59B6), Color(0xFF1ABC9C), Color(0xFFE67E22), Color(0xFFE84393)
                )

                val sweepAngle = 45f // 360 / 8 slices
                for (i in 0..7) {
                    drawArc(
                        color = colors[i],
                        startAngle = i * sweepAngle,
                        sweepAngle = sweepAngle,
                        useCenter = true
                    )
                }

                // Decorative borders
                drawCircle(
                    color = Color.White,
                    radius = size.minDimension / 2.05f,
                    style = Stroke(width = 4.dp.toPx())
                )

                drawCircle(
                    color = Color(0xFF181B2E),
                    radius = size.minDimension / 10f
                )
            }

            // Beautiful mini content tags representation on Canvas
            Text(
                text = "🎯",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryGold
            )
        }

        // Pointer indicators
        Icon(
            imageVector = Icons.Filled.PlayArrow,
            contentDescription = "مؤشر",
            tint = PrimaryGold,
            modifier = Modifier
                .size(32.dp)
                .rotate(90f) // Facing down
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Action panel or timers
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (!canSpinNow) {
                Text(
                    text = remainingTimeStr,
                    color = PrimaryGold,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            } else {
                Text(
                    text = if (isSpinClaimable) "المحاولة المجانية متاحة الآن!" else "لديك ${uiState.extraSpinsAvailable} فرص إضافية متاحة!",
                    color = CorrectGreen,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Spin CTA with modern 3D design
            val ctaEnabled = canSpinNow && !uiState.isSpinning
            val spinBrush = if (ctaEnabled) {
                Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF2575FC), // Vibrant Blue
                        Color(0xFF6A11CB)  // Royal Purple
                    )
                )
            } else null

            AbqariNeon3DButton(
                onClick = onSpinClick,
                enabled = ctaEnabled,
                faceBrush = spinBrush,
                faceColor = if (ctaEnabled) Color.Transparent else Color(0xFF2C273D),
                glowColor = if (ctaEnabled) SupportTeal else Color.Transparent,
                cornerRadius = 15,
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(54.dp)
                    .testTag("trigger_spin_wheel_button")
            ) {
                Text(
                    text = if (uiState.isSpinning) "جاري تدوير العجلة..." else "در العجلة الآن!",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (ctaEnabled) Color.White else Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
 
            // 📺 Rewarded Ads option trigger panel
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1C29)),
                border = BorderStroke(1.dp, Color(0xFF2E324A))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "احصل على فرص إضافية 📺",
                            color = TextWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "شاهد إعلان فيديو قصير للحصول على فرصة تدوير إضافية لتكديس الذهب والخطي!",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    val adEnabled = uiState.adSpinsWatchedCount < 2 && !uiState.isSpinning
                    val adBrush = if (adEnabled) {
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF2575FC), // Vibrant Blue
                                Color(0xFF6A11CB)  // Royal Purple
                            )
                        )
                    } else null

                    AbqariNeon3DButton(
                        onClick = onWatchAdClick,
                        enabled = adEnabled,
                        faceBrush = adBrush,
                        faceColor = if (adEnabled) Color.Transparent else Color(0xFF2C273D),
                        glowColor = if (adEnabled) SupportTeal else Color.Transparent,
                        cornerRadius = 15,
                        modifier = Modifier.testTag("watch_mock_ad_button")
                    ) {
                        Text(
                            text = "فرصة (+1)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (adEnabled) Color.White else Color.Gray,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
    }
}

// 🏆 4. LEADERBOARD SCREEN
@Composable
fun LeaderboardScreen(
    uiState: QuizUiState,
    onBackHome: () -> Unit
) {
    val currentLeaderboard = uiState.leaderboard
    val coinVal = uiState.userStats?.coins ?: 150
    val skipsVal = uiState.userStats?.totalSkipsAvailable ?: 3

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TopStatsHeader(
            coins = coinVal,
            skips = skipsVal,
            isSoundEnabled = uiState.isSoundEnabled,
            onToggleSound = {},
            showBackButton = true,
            onBackClick = onBackHome
        )

        Spacer(modifier = Modifier.height(10.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2235)),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.2.dp, SupportTeal.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Leaderboard,
                    contentDescription = "وسام الشرف",
                    tint = PrimaryGold,
                    modifier = Modifier.size(54.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "قاعة المشاهير والعباقرة",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp
                    )
                    Text(
                        text = "ترتيب أفضل الإنجازات التي تم تسجيلها محلياً على هذا الجهاز",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // List layout for scores
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF141624)),
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, Color(0xFF23263B))
        ) {
            if (currentLeaderboard.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Filled.Stars,
                            contentDescription = "فارغ",
                            tint = TextMuted,
                            modifier = Modifier.size(60.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "لا توجد نتائج مسجلة بعد!",
                            color = TextWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "العب بعض جولات التحدي لتسجيل حضورك الأول هنا",
                            color = TextMuted,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(currentLeaderboard) { index, scoreEntry ->
                        val rankNum = index + 1
                        val itemColor = when (rankNum) {
                            1 -> Color(0xFF2C230E) // Gold rank bg
                            2 -> Color(0xFF1E2429) // Silver rank bg
                            3 -> Color(0xFF291E19) // Bronze rank bg
                            else -> Color(0xFF1A1C2C)
                        }
                        val indicatorColor = when (rankNum) {
                            1 -> PrimaryGold
                            2 -> Color(0xFFCCCCCC)
                            3 -> Color(0xFFCD7F32)
                            else -> SupportTeal
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(itemColor)
                                .border(1.dp, indicatorColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Badge number
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(indicatorColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$rankNum",
                                        color = Color.Black,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 14.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text(
                                        text = "العبقري الممتاز #$rankNum",
                                        color = TextWhite,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "القسم: ${scoreEntry.category} • م: ${scoreEntry.level}",
                                        color = TextMuted,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            Text(
                                text = "${scoreEntry.score} نقطة 🌟",
                                color = indicatorColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

// 📺 5. FULL SCREEN VIDEO AD EMULATION DIALOG (UX is incredibly premium)
@Composable
fun VideoAdSimulateOverlay(
    secondsRemaining: Int
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.95f))
            .clickable(enabled = false) {} // block click intercepts
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF1E2235))
                    .border(2.dp, PrimaryGold, RoundedCornerShape(14.dp))
                    .padding(30.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.Tv,
                        contentDescription = "محاكي الإعلان",
                        tint = PrimaryGold,
                        modifier = Modifier.size(75.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "جاري عرض الإعلان الممول الممتع 📺",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "مكافأة مشاهدة الإعلانات تساعدنا على الاستمرار في تطوير ألعاب ذكية مجانية لكم!",
                        color = TextMuted,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Simulated Countdown indicator
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(PrimaryGold),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$secondsRemaining",
                            color = Color.Black,
                            fontWeight = FontWeight.Black,
                            fontSize = 24.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "احرص على عدم الإغلاق للحصول على المكافأة",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

// 🎉 6. POPUP DIALOGS TO AWARD PRIZES
@Composable
fun RewardDialog(
    title: String,
    coinsWon: Int,
    skipsWon: Int,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            val dismissBrush = Brush.horizontalGradient(
                colors = listOf(
                    Color(0xFF2575FC), // Vibrant Blue
                    Color(0xFF6A11CB)  // Royal Purple
                )
            )
            AbqariNeon3DButton(
                onClick = onDismiss,
                faceBrush = dismissBrush,
                glowColor = SupportTeal,
                cornerRadius = 15,
                modifier = Modifier.width(140.dp)
            ) {
                Text(
                    text = "شكرًا جزيلًا!",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        title = {
            Text(
                text = title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (skipsWon == -1) {
                    // special flag error
                    Icon(
                        imageVector = Icons.Filled.NotificationsActive,
                        contentDescription = "عملات غير كافية",
                        tint = IncorrectRed,
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "ليس لديك نقود كافية للتخطي! يتطلب التخطي ٣٠ قطعة ذهبية أو تذكرة تخطي مجانية. يمكنك كسب المزيد بزيارة دولاب الحظ يومياً!",
                        color = TextWhite,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Celebration,
                        contentDescription = "تهانينا",
                        tint = PrimaryGold,
                        modifier = Modifier.size(60.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "لقد حصلت على الجوائز التالية المضافة لحسابك العبقري فوراً:",
                        color = TextMuted,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    if (coinsWon > 0) {
                        Text(
                            text = "🪙 +$coinsWon قطعة ذهبية",
                            color = PrimaryGold,
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp
                        )
                    }
                    if (skipsWon > 0) {
                        Text(
                            text = "🎟️ +$skipsWon تذكرة تخطي مجاني",
                            color = SupportTeal,
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        },
        containerColor = Color(0xFF1E2235),
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
fun LevelUpCelebrationDialog(
    levelTitle: String,
    roleName: String,
    onDismiss: () -> Unit
) {
    val scale = remember { Animatable(0.7f) }
    LaunchedEffect(Unit) {
        scale.animateTo(1.05f, animationSpec = spring(dampingRatio = 0.5f, stiffness = 400f))
        scale.animateTo(1.0f, animationSpec = spring(dampingRatio = 0.6f, stiffness = 300f))
    }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .graphicsLayer(scaleX = scale.value, scaleY = scale.value)
                .border(2.dp, Brush.horizontalGradient(listOf(PrimaryGold, SupportTeal, PrimaryGold)), RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161A2B))
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Celebration,
                    contentDescription = "تهانينا",
                    tint = PrimaryGold,
                    modifier = Modifier.size(72.dp)
                )
                
                Spacer(modifier = Modifier.height(14.dp))
                
                Text(
                    text = "🌟 ترقية رتبة العبقري! 🌟",
                    color = PrimaryGold,
                    fontWeight = FontWeight.Black,
                    fontSize = 22.sp,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(10.dp))
                
                Text(
                    text = "لقد ارتقيت في سماء المعرفة وحققت رتبة علمية جديدة!",
                    color = TextMuted,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF231C0E))
                        .border(1.2.dp, PrimaryGold, RoundedCornerShape(16.dp))
                        .padding(horizontal = 24.dp, vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = levelTitle,
                            color = PrimaryGold,
                            fontWeight = FontWeight.Black,
                            fontSize = 26.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = roleName,
                            color = SupportTeal,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                val followBrush = Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF2575FC), // Vibrant Blue
                        Color(0xFF6A11CB)  // Royal Purple
                    )
                )
                AbqariNeon3DButton(
                    onClick = {
                        SynthPlayer.playButtonClickSound()
                        onDismiss()
                    },
                    faceBrush = followBrush,
                    glowColor = SupportTeal,
                    cornerRadius = 15,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("dismiss_level_up_button")
                ) {
                    Text(
                        text = "تابع المسير المعرفي 🚀",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

// 🏆 7. RESULTS OVERVIEW SCREEN
@Composable
fun ResultsScreen(
    uiState: QuizUiState,
    onRestart: () -> Unit,
    onGoHome: () -> Unit
) {
    val scorePercent = (uiState.score.toFloat() / uiState.questions.size) * 100
    val feedbackPair = getFeedbackMessage(uiState.score, uiState.questions.size)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // Big visual layout for game details
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF181B2E)),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.5.dp, SupportTeal),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = if (scorePercent >= 50f) Icons.Filled.EmojiEvents else Icons.Filled.Stars,
                    contentDescription = "النتيجة",
                    modifier = Modifier.size(105.dp),
                    tint = if (scorePercent >= 50f) PrimaryGold else TextMuted
                )

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = stringResource(id = if (scorePercent >= 60) R.string.congrats else R.string.game_over),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = feedbackPair.first,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = feedbackPair.second,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = stringResource(
                        id = R.string.your_score,
                        uiState.score,
                        uiState.questions.size
                    ),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    fontSize = 22.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Show session coins earned
                Text(
                    text = "الذهب المحصود من هذه الجولة: +${uiState.totalCoinsEarnedThisSession} 🪙",
                    color = PrimaryGold,
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp
                )

                // High score record alert
                if (uiState.isNewHighScore) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(CorrectGreen.copy(alpha = 0.2f))
                            .border(1.dp, CorrectGreen, RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "🏆 رقم قياسي محلي جديد العباقرة!",
                            color = CorrectGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Actions navigation buttons
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val restartBrush = Brush.horizontalGradient(
                colors = listOf(
                    Color(0xFF2575FC), // Vibrant Blue
                    Color(0xFF6A11CB)  // Royal Purple
                )
            )
            AbqariNeon3DButton(
                onClick = onRestart,
                faceBrush = restartBrush,
                glowColor = SupportTeal,
                cornerRadius = 15,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("restart_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "مجدداً",
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(id = R.string.restart_game),
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = Color.White
                    )
                }
            }

            AbqariNeon3DButton(
                onClick = onGoHome,
                faceColor = Color(0xFF1E2235),
                glowColor = Color(0xFF2E324A),
                cornerRadius = 15,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("back_home_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Filled.Home,
                        contentDescription = "الرئيسية",
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(id = R.string.back_to_home),
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
        }
    }
}


// UTILITIES COMPANIONS
data class LevelProgressDetails(
    val levelTitle: String,
    val roleName: String,
    val current: Int,
    val target: Int,
    val progressPercentage: Float
)

fun calculateLevelDetails(totalCorrect: Int): LevelProgressDetails {
    return when {
        totalCorrect < 25 -> {
            val percentage = totalCorrect.toFloat() / 25f
            LevelProgressDetails("مبتدئ (مستوى ١)", "الباحث الصاعد", totalCorrect, 25, percentage)
        }
        totalCorrect < 75 -> {
            val base = totalCorrect - 25
            val percentage = base.toFloat() / 50f
            LevelProgressDetails("متوسط (مستوى ٢)", "المثابر المكافح", totalCorrect, 75, percentage)
        }
        totalCorrect < 175 -> {
            val base = totalCorrect - 75
            val percentage = base.toFloat() / 100f
            LevelProgressDetails("متقدم (مستوى ٣)", "المستشار الذكي", totalCorrect, 175, percentage)
        }
        else -> {
            val base = (totalCorrect - 175).coerceAtMost(1000)
            val percentage = base.toFloat() / 1000f
            LevelProgressDetails("عبقري (مستوى ٤ 👑)", "صاحب العقل المطلق", totalCorrect, 1175, percentage)
        }
    }
}

@Composable
fun LoadingScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = SupportTeal)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(id = R.string.loading),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
fun CategoryItemCard(
    categoryKey: String,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(130.dp)
            .height(150.dp)
            .clickable { onSelect() }
            .testTag("category_${categoryKey.lowercase()}_button"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF1B2A40) else Color(0xFF1E2235)
        ),
        border = BorderStroke(
            1.5.dp,
            if (isSelected) SupportTeal else Color(0xFF2C324D)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = getCategoryIcon(categoryKey),
                contentDescription = null,
                tint = if (isSelected) SupportTeal else Color.White,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = getArabicCategoryName(categoryKey),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) SupportTeal else Color.White,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun getArabicCategoryName(categoryKey: String): String {
    return when (categoryKey) {
        "ALL" -> "جميع الأقسام"
        "General" -> stringResource(id = R.string.category_general)
        "Science" -> stringResource(id = R.string.category_science)
        "History" -> stringResource(id = R.string.category_history)
        "Sports" -> stringResource(id = R.string.category_sports)
        "Geography" -> stringResource(id = R.string.category_geography)
        "Islamic" -> stringResource(id = R.string.category_islamic)
        else -> categoryKey
    }
}

@Composable
fun getArabicLevelName(levelKey: String): String {
    return when (levelKey) {
        "ALL" -> "كل المستويات"
        "Easy" -> stringResource(id = R.string.level_easy)
        "Medium" -> stringResource(id = R.string.level_medium)
        "Hard" -> stringResource(id = R.string.level_hard)
        else -> levelKey
    }
}

fun getCategoryIcon(categoryKey: String): ImageVector {
    return when (categoryKey) {
        "ALL" -> Icons.Filled.AllInclusive
        "General" -> Icons.Filled.Lightbulb
        "Science" -> Icons.Filled.Science
        "History" -> Icons.Filled.HourglassEmpty
        "Sports" -> Icons.Filled.SportsSoccer
        "Geography" -> Icons.Filled.Public
        "Islamic" -> Icons.Default.MenuBook
        else -> Icons.Default.Help
    }
}

fun getLevelColor(levelKey: String): Color {
    return when (levelKey) {
        "Easy" -> CorrectGreen
        "Medium" -> PrimaryGold
        "Hard" -> IncorrectRed
        else -> SupportTeal
    }
}

@Composable
fun getFeedbackMessage(score: Int, total: Int): Pair<String, Color> {
    val percent = if (total > 0) (score.toFloat() / total) * 100 else 0f
    return when {
        percent >= 85f -> Pair(stringResource(id = R.string.performance_excellent), CorrectGreen)
        percent >= 65f -> Pair(stringResource(id = R.string.performance_good), SupportTeal)
        percent >= 45f -> Pair(stringResource(id = R.string.performance_average), PrimaryGold)
        else -> Pair(stringResource(id = R.string.performance_poor), IncorrectRed)
    }
}

// 🛒 4. SHOP SCREEN (متجر العباقرة)
@Composable
fun ShopScreen(
    uiState: QuizUiState,
    onBackHome: () -> Unit,
    onBuyRemoveAds: (Boolean) -> Unit,
    onBuyCoinsPack: (Int, Double) -> Unit,
    onBuyHintsPack: (Int, Int) -> Unit,
    onWatchAdForCoins: () -> Unit
) {
    val scrollState = rememberScrollState()
    val coins = uiState.userStats?.coins ?: 150
    val hintsCount = uiState.userStats?.hintsCount ?: 0
    val isAdsRemoved = uiState.userStats?.isAdsRemoved == true

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackHome,
                    modifier = Modifier.testTag("shop_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "الرجوع",
                        tint = Color.White
                    )
                }
                
                Text(
                    text = "متجر العباقرة",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Black
                )

                // Balances
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Coins counter badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF2C220E))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "$coins",
                                color = PrimaryGold,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "🪙", fontSize = 12.sp)
                        }
                    }

                    // Hints counter badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF142435))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "$hintsCount",
                                color = Color(0xFF4AC3FF),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "💡", fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Feature Banner
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1D1B26)),
                    border = BorderStroke(1.2.dp, Color(0xFF5E35B1))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "⚡ تميز برتبة العباقرة الكبرى ⚡",
                            color = Color(0xFFB39DDB),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "احصل على قطع الذهب والتلميحات لكشف وتخطي أصعب الأسئلة وتصدر قائمة الأذكياء!",
                            color = TextMuted,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // 🚫 REMOVE ADS PACK Category
                ShopSectionHeader(title = "🚫 إزالة الإعلانات")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, if (isAdsRemoved) Color.Gray else Color(0xFFE91E63)),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF24161C))
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "إزالة الإعلانات مدى الحياة",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "العب دون انقطاع واحصل على هدايا مجانية ومكافآت فورية مباشرة!",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(12.dp))

                        if (isAdsRemoved) {
                            AbqariNeon3DButton(
                                onClick = {},
                                enabled = false,
                                faceColor = Color(0xFF1B2C24),
                                glowColor = CorrectGreen,
                                cornerRadius = 15,
                                modifier = Modifier.width(100.dp)
                            ) {
                                Text(text = "مفعّل ✓", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CorrectGreen)
                            }
                        } else {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                AbqariNeon3DButton(
                                    onClick = { onBuyRemoveAds(true) },
                                    faceColor = Color(0xFF2C220E),
                                    glowColor = PrimaryGold,
                                    cornerRadius = 15,
                                    modifier = Modifier
                                        .width(115.dp)
                                        .height(44.dp)
                                        .testTag("buy_remove_ads_coins")
                                ) {
                                    Text(text = "5,000 🪙", color = PrimaryGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                AbqariNeon3DButton(
                                    onClick = { onBuyRemoveAds(false) },
                                    faceBrush = Brush.horizontalGradient(
                                        colors = listOf(
                                            Color(0xFFE91E63),
                                            Color(0xFFFF4081)
                                        )
                                    ),
                                    glowColor = Color(0xFFFF4081),
                                    cornerRadius = 15,
                                    modifier = Modifier
                                        .width(115.dp)
                                        .height(44.dp)
                                        .testTag("buy_remove_ads_cash")
                                ) {
                                    Text(text = "$2.99 USD", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // 💡 ASSIST HINT PACKETS Category
                ShopSectionHeader(title = "💡 هدايا وتلميحات العباقرة")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ShopItemCard(
                        title = "باقة بدء العباقرة",
                        subtitle = "5 تلميحات",
                        price = "150 🪙",
                        onBuy = { onBuyHintsPack(5, 150) },
                        badgeText = "💡 x5",
                        tag = "buy_hints_5"
                    )
                    ShopItemCard(
                        title = "باقة نخبة الذكاء",
                        subtitle = "20 تلميحًا",
                        price = "500 🪙",
                        onBuy = { onBuyHintsPack(20, 500) },
                        badgeText = "💡 x20",
                        tag = "buy_hints_20"
                    )
                }

                // 🪙 GOLD PACKETS Category
                ShopSectionHeader(title = "🪙 حزم الذهب الخالص")
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    GoldItemRow(
                        title = "جراب العباقرة الصغير",
                        amount = "+1,000 قطعة ذهبية",
                        price = "$0.99 USD",
                        onBuy = { onBuyCoinsPack(1000, 0.99) },
                        tag = "buy_gold_1000"
                    )
                    GoldItemRow(
                        title = "صندوق الكنز الذكي",
                        amount = "+5,000 قطعة ذهبية",
                        price = "$2.99 USD",
                        onBuy = { onBuyCoinsPack(5000, 2.99) },
                        tag = "buy_gold_5000"
                    )
                    GoldItemRow(
                        title = "سبيكة عبقري الملوك",
                        amount = "+15,000 قطعة ذهبية",
                        price = "$6.99 USD",
                        onBuy = { onBuyCoinsPack(15000, 6.99) },
                        tag = "buy_gold_15000"
                    )
                }

                // FREE GOLD REWARD Category
                ShopSectionHeader(title = "📺 مكافأة المشاهدة المجانية")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF142426)),
                    border = BorderStroke(1.dp, SupportTeal.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "شاهد فيديو مجاني واحصل على عملات",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "أكمل فيديو الإعلان المثير لتحصل على +100 قطعة ذهبية مجانًا!",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        val watchAdBrush = Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF2575FC), // Vibrant Blue
                                Color(0xFF6A11CB)  // Royal Purple
                            )
                        )
                        AbqariNeon3DButton(
                            onClick = onWatchAdForCoins,
                            faceBrush = watchAdBrush,
                            glowColor = SupportTeal,
                            cornerRadius = 15,
                            modifier = Modifier
                                .width(110.dp)
                                .height(44.dp)
                                .testTag("watch_ad_for_coins_button")
                        ) {
                            Text(text = "+100 🪙", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
fun ShopSectionHeader(title: String) {
    Text(
        text = title,
        color = Color.White,
        fontWeight = FontWeight.Black,
        fontSize = 15.sp,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Start
    )
}

@Composable
fun RowScope.ShopItemCard(
    title: String,
    subtitle: String,
    price: String,
    onBuy: () -> Unit,
    badgeText: String,
    tag: String
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1.0f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 400f),
        label = "ShopItemScale"
    )

    Card(
        modifier = Modifier
            .weight(1f)
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .clickable(interactionSource = interactionSource, indication = androidx.compose.foundation.LocalIndication.current) {
                SynthPlayer.playButtonClickSound()
                onBuy()
            }
            .testTag(tag),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C2235)),
        border = BorderStroke(1.dp, Color(0xFF2C324D))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF223A4E))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(text = badgeText, fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color(0xFF4AC3FF))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.Center)
            Text(text = subtitle, color = TextMuted, fontSize = 10.sp, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(10.dp))
            AbqariNeon3DButton(
                onClick = {
                    SynthPlayer.playButtonClickSound()
                    onBuy()
                },
                faceColor = Color(0xFF142435),
                glowColor = Color(0xFF4AC3FF),
                cornerRadius = 15,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
            ) {
                Text(text = price, color = Color(0xFF4AC3FF), fontSize = 11.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
fun GoldItemRow(
    title: String,
    amount: String,
    price: String,
    onBuy: () -> Unit,
    tag: String
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1.0f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 400f),
        label = "GoldItemScale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .clickable(interactionSource = interactionSource, indication = androidx.compose.foundation.LocalIndication.current) {
                SynthPlayer.playButtonClickSound()
                onBuy()
            }
            .testTag(tag),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF20221E)),
        border = BorderStroke(1.dp, Color(0xFF2F3224))
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "🎁", fontSize = 24.sp)
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(text = title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text(text = amount, color = PrimaryGold, fontWeight = FontWeight.Black, fontSize = 12.sp)
                }
            }
            AbqariNeon3DButton(
                onClick = {
                    SynthPlayer.playButtonClickSound()
                    onBuy()
                },
                faceColor = Color(0xFF2C220E),
                glowColor = PrimaryGold,
                cornerRadius = 15,
                modifier = Modifier
                    .width(105.dp)
                    .height(42.dp)
            ) {
                Text(text = price, color = PrimaryGold, fontSize = 11.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

// 👥 5. FRIENDS SCREEN (الأصدقاء والتحدي)
@Composable
fun FriendsScreen(
    uiState: QuizUiState,
    onBackHome: () -> Unit,
    onAddFriend: (String, String) -> Unit,
    onHelpRequestClick: (String, String) -> Unit
) {
    val scrollState = rememberScrollState()
    val list = uiState.friendsList
    val myUid = uiState.userStats?.userUniqueId ?: "ABQ-7964"

    var targetId by remember { mutableStateOf("") }
    var targetName by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackHome,
                    modifier = Modifier.testTag("friends_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "الرجوع",
                        tint = Color.White
                    )
                }

                Text(
                    text = "الأصدقاء والتحدي",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Black
                )

                IconButton(onClick = {}) {
                    Icon(
                        imageVector = Icons.Filled.Share,
                        contentDescription = "مشاركة",
                        tint = SupportTeal
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. My Unique ID section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF142435)),
                    border = BorderStroke(1.2.dp, Color(0xFF2196F3).copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "رقمك المعرّف الفريد للعباقرة",
                            color = TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = myUid,
                                color = Color(0xFF4AC3FF),
                                fontWeight = FontWeight.Black,
                                fontSize = 21.sp,
                                modifier = Modifier.testTag("my_unique_id_label")
                            )
                            AbqariNeon3DButton(
                                onClick = {}, // Mock copied dialog
                                faceColor = Color(0xFF21364E),
                                glowColor = Color(0xFF4AC3FF),
                                cornerRadius = 15,
                                modifier = Modifier
                                    .width(90.dp)
                                    .height(42.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.ContentCopy,
                                        contentDescription = "نسخ المعرّف",
                                        tint = Color(0xFF4AC3FF),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = "نسخ", color = Color(0xFF4AC3FF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // 2. Add Friend form
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1C2235)),
                    border = BorderStroke(1.dp, Color(0xFF2D324F))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "🤝 إضافة عبقري جديد لقائمتك",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )

                        OutlinedTextField(
                            value = targetId,
                            onValueChange = { targetId = it },
                            label = { Text("الرقم التعريفي (مثل ABQ-9900)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("friend_id_input"),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = SupportTeal,
                                unfocusedBorderColor = Color(0xFF2D324F)
                            )
                        )

                        OutlinedTextField(
                            value = targetName,
                            onValueChange = { targetName = it },
                            label = { Text("اسم الصديق") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("friend_name_input"),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = SupportTeal,
                                unfocusedBorderColor = Color(0xFF2D324F)
                            )
                        )

                        val addFriendBrush = Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF2575FC), // Vibrant Blue
                                Color(0xFF6A11CB)  // Royal Purple
                            )
                        )
                        AbqariNeon3DButton(
                            onClick = {
                                if (targetId.isNotBlank() && targetName.isNotBlank()) {
                                    onAddFriend(targetId.trim(), targetName.trim())
                                    targetId = ""
                                    targetName = ""
                                }
                            },
                            faceBrush = addFriendBrush,
                            glowColor = SupportTeal,
                            cornerRadius = 15,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("add_friend_submit_button")
                        ) {
                            Text(
                                text = "إضافة الصديق إلى القائمة",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                // Interaction feedback notification message
                if (uiState.isHelpFeedbackActive) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B2C24)),
                        border = BorderStroke(1.dp, CorrectGreen),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "📢", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = uiState.helpFeedbackMessage,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // 3. Friends List & Active Actions
                Text(
                    text = "👥 قائمة الأصدقاء ونشاط التبادل",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Start
                )

                if (list.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 30.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "لم تقم بإضافة أي أصدقاء بعد. ابدأ بإضافة أصدقائك وتحديهم!", color = TextMuted, fontSize = 12.sp, textAlign = TextAlign.Center)
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        list.forEach { friend ->
                            FriendRowItem(friend = friend, onHelpClick = { onHelpRequestClick(friend.friendUserId, friend.name) })
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
fun FriendRowItem(
    friend: com.example.data.model.Friend,
    onHelpClick: () -> Unit
) {
    val isOnline = friend.status == "نشط"
    val badgeBg = if (isOnline) Color(0xFF1B2C24) else Color(0xFF262626)
    val badgeColor = if (isOnline) CorrectGreen else Color.Gray

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("friend_row_${friend.friendUserId}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C2235)),
        border = BorderStroke(1.dp, Color(0xFF2D324F))
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF223A4E)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = friend.name.firstOrNull()?.toString() ?: "?", color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column {
                    Text(text = friend.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(text = friend.friendUserId, color = TextMuted, fontSize = 10.sp)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(badgeBg)
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text(text = friend.status, color = badgeColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "رصيد الأذكياء", color = TextMuted, fontSize = 9.sp)
                    Text(text = "${friend.score} نقطة", color = SupportTeal, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                val helpBrush = if (isOnline) {
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF2575FC), // Vibrant Blue
                            Color(0xFF6A11CB)  // Royal Purple
                        )
                    )
                } else null

                AbqariNeon3DButton(
                    onClick = onHelpClick,
                    enabled = isOnline,
                    faceBrush = helpBrush,
                    faceColor = if (isOnline) Color.Transparent else Color(0xFF23252E),
                    glowColor = if (isOnline) Color(0xFF4AC3FF) else Color.Transparent,
                    cornerRadius = 15,
                    modifier = Modifier
                        .width(115.dp)
                        .height(44.dp)
                        .testTag("friend_help_action_button_${friend.friendUserId}")
                ) {
                    Text(
                        text = "طلب مساعدة 🤝",
                        color = if (isOnline) Color.White else Color.Gray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
