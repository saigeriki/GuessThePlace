package com.example.guesstheplaceindia

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.guesstheplaceindia.ui.theme.BackgroundDark
import com.example.guesstheplaceindia.ui.theme.BackgroundLight
import com.example.guesstheplaceindia.ui.theme.ErrorContainerLight
import com.example.guesstheplaceindia.ui.theme.ErrorDark
import com.example.guesstheplaceindia.ui.theme.GoldContainerLight
import com.example.guesstheplaceindia.ui.theme.GoldDark
import com.example.guesstheplaceindia.ui.theme.GoldLight
import com.example.guesstheplaceindia.ui.theme.GradientAccentEnd
import com.example.guesstheplaceindia.ui.theme.GradientAccentStart
import com.example.guesstheplaceindia.ui.theme.GradientPrimaryEnd
import com.example.guesstheplaceindia.ui.theme.GradientPrimaryStart
import com.example.guesstheplaceindia.ui.theme.GuessThePlaceIndiaTheme
import com.example.guesstheplaceindia.ui.theme.PrimaryDark
import com.example.guesstheplaceindia.ui.theme.PrimaryLight
import com.example.guesstheplaceindia.ui.theme.SuccessContainerLight
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class Place(
    val imageResId: Int,
    val options: List<String>,
    val correctAnswer: String
)

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GuessThePlaceIndiaTheme {
                GameScreen(
                    sharedPref = getSharedPreferences("GuessPlacePrefs", MODE_PRIVATE),
                    onFinishGame = { finalScore, total ->
                        val intent = Intent(this, ResultActivity::class.java).apply {
                            putExtra("score", finalScore)
                            putExtra("total", total)
                        }
                        startActivity(intent)
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
fun GameScreen(
    sharedPref: SharedPreferences,
    onFinishGame: (Int, Int) -> Unit
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    // Game data - shuffled once per session
    val fullPlaces = remember {
        mutableListOf(
            Place(R.drawable.tajmahal, listOf("Taj Mahal", "Qutub Minar", "Charminar", "Red Fort"), "Taj Mahal"),
            Place(R.drawable.charminar, listOf("Charminar", "Golconda Fort", "Mysore Palace", "Red Fort"), "Charminar"),
            Place(R.drawable.gatewayofindia, listOf("Gateway of India", "India Gate", "Victoria Memorial", "Hawa Mahal"), "Gateway of India"),
            Place(R.drawable.mysore_palace, listOf("Mysore Palace", "City Palace", "Umaid Bhawan", "Falaknuma Palace"), "Mysore Palace"),
            Place(R.drawable.qutub_minar, listOf("Qutub Minar", "Charminar", "Taj Mahal", "Gateway of India"), "Qutub Minar"),
            Place(R.drawable.hawa_mahal, listOf("Hawa Mahal", "Jal Mahal", "City Palace", "Amber Fort"), "Hawa Mahal"),
            Place(R.drawable.golden_temple, listOf("Golden Temple", "Akshardham", "Iskcon Temple", "Meenakshi Temple"), "Golden Temple"),
            Place(R.drawable.india_gate, listOf("India Gate", "Gateway of India", "Red Fort", "Qutub Minar"), "India Gate"),
            Place(R.drawable.red_fort, listOf("Red Fort", "Amber Fort", "Golconda Fort", "Agra Fort"), "Red Fort"),
            Place(R.drawable.lotus_temple, listOf("Lotus Temple", "Akshardham", "Iskcon Temple", "Golden Temple"), "Lotus Temple"),
            Place(R.drawable.meenakshi_temple, listOf("Meenakshi Temple", "Brihadeeswara Temple", "Iskcon Temple", "Akshardham"), "Meenakshi Temple"),
            Place(R.drawable.akshardham, listOf("Akshardham", "Lotus Temple", "Iskcon Temple", "Golden Temple"), "Akshardham"),
            Place(R.drawable.victoria_memorial, listOf("Victoria Memorial", "Howrah Bridge", "India Gate", "Gateway of India"), "Victoria Memorial"),
            Place(R.drawable.ajanta_caves, listOf("Ajanta Caves", "Ellora Caves", "Elephanta Caves", "Badami Caves"), "Ajanta Caves"),
            Place(R.drawable.ellora_caves, listOf("Ellora Caves", "Ajanta Caves", "Elephanta Caves", "Badami Caves"), "Ellora Caves"),
            Place(R.drawable.sanchi_stupa, listOf("Sanchi Stupa", "Dhamek Stupa", "Mahabodhi Temple", "Nalanda"), "Sanchi Stupa"),
            Place(R.drawable.konark_sun_temple, listOf("Konark Sun Temple", "Jagannath Temple", "Brihadeeswara Temple", "Meenakshi Temple"), "Konark Sun Temple")
        ).apply { shuffle() }
    }

    var currentIndex by remember {
        mutableIntStateOf(sharedPref.getInt("currentIndex", 0))
    }
    var score by remember {
        mutableIntStateOf(sharedPref.getInt("score", 0))
    }

    // Question state
    var timeLeft by remember { mutableIntStateOf(10) }
    var isAnswered by remember { mutableStateOf(false) }
    var selectedAnswer by remember { mutableStateOf<String?>(null) }
    var shuffledOptions by remember { mutableStateOf<List<String>>(emptyList()) }
    var showNext by remember { mutableStateOf(false) }

    // Animation values
    val imageScale = remember { Animatable(0.92f) }
    val progressAnim = remember { Animatable(currentIndex / fullPlaces.size.toFloat()) }

    fun saveProgress() {
        sharedPref.edit()
            .putInt("currentIndex", currentIndex)
            .putInt("score", score)
            .apply()
    }

    fun loadNextQuestion() {
        if (currentIndex >= fullPlaces.size) {
            sharedPref.edit().clear().apply()
            onFinishGame(score, fullPlaces.size)
            return
        }
        val place = fullPlaces[currentIndex]
        shuffledOptions = place.options.shuffled()
        isAnswered = false
        selectedAnswer = null
        showNext = false
        timeLeft = 10
        scope.launch {
            imageScale.snapTo(0.92f)
            imageScale.animateTo(1f, animationSpec = tween(350, easing = FastOutSlowInEasing))
            progressAnim.animateTo(currentIndex / fullPlaces.size.toFloat(), tween(500))
        }
    }

    // Initial load
    LaunchedEffect(currentIndex) {
        loadNextQuestion()
    }

    // Timer effect
    LaunchedEffect(currentIndex, isAnswered) {
        if (isAnswered) return@LaunchedEffect
        while (timeLeft > 0 && !isAnswered) {
            delay(1000)
            timeLeft--
        }
        if (timeLeft == 0 && !isAnswered) {
            // Time out - reveal and auto next
            isAnswered = true
            showNext = true
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            delay(1200)
            currentIndex++
            saveProgress()
        }
    }

    fun checkAnswer(answer: String) {
        if (isAnswered) return
        isAnswered = true
        selectedAnswer = answer
        val correct = fullPlaces[currentIndex].correctAnswer
        if (answer == correct) {
            score++
            saveProgress()
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        } else {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        showNext = true
    }

    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val backgroundBrush = if (isDark) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF1A1B23),
                Color(0xFF121318),
                Color(0xFF0D0E13)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFFF8F7FF),
                Color(0xFFF0EDFA),
                Color(0xFFFFFFFF)
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- Top App Bar Pro ---
            TopBar(
                score = score,
                total = fullPlaces.size,
                current = currentIndex + 1,
                onRestart = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    currentIndex = 0
                    score = 0
                    fullPlaces.shuffle()
                    sharedPref.edit().clear().apply()
                    // trigger reload
                    shuffledOptions = emptyList()
                    // force recompose load
                    // We'll trigger via side effect by reassigning currentIndex already 0 which triggers LaunchedEffect? need extra
                    // So manually call load
                    // but LaunchedEffect on currentIndex will run
                }
            )

            // --- Progress Row ---
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(GradientPrimaryStart, GradientPrimaryEnd)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.LocationOn,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "QUESTION",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.2.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "${currentIndex + 1} / ${fullPlaces.size}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Timer Chip - Pro
                    TimerChip(timeLeft = timeLeft, totalTime = 10)
                }

                // Fancy Progress Bar
                Box(modifier = Modifier.fillMaxWidth()) {
                    LinearProgressIndicator(
                        progress = { (currentIndex + if (isAnswered) 1 else 0) / fullPlaces.size.toFloat() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(100.dp)),
                        color = PrimaryLight,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        strokeCap = StrokeCap.Round
                    )
                }
            }

            // --- Image Card Pro ---
            if (currentIndex < fullPlaces.size) {
                PlaceImageCardPro(
                    place = fullPlaces[currentIndex],
                    scale = imageScale.value,
                    isAnswered = isAnswered
                )
            }

            // --- Prompt ---
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Icon(
                    Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    tint = PrimaryLight,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Guess this iconic place",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // --- Options Grid Pro ---
            if (shuffledOptions.isNotEmpty() && currentIndex < fullPlaces.size) {
                val placeForOptions = fullPlaces[currentIndex]
                val correctAnswer = placeForOptions.correctAnswer

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    shuffledOptions.forEachIndexed { idx, option ->
                        val letter = listOf("A", "B", "C", "D")[idx]
                        val isSelected = selectedAnswer == option
                        val isCorrect = option == correctAnswer

                        val state = when {
                            !isAnswered -> OptionState.Default
                            isCorrect -> OptionState.Correct
                            isSelected && !isCorrect -> OptionState.Wrong
                            else -> OptionState.Disabled
                        }

                        OptionCardPro(
                            letter = letter,
                            text = option,
                            state = state,
                            enabled = !isAnswered,
                            onClick = { checkAnswer(option) }
                        )
                    }
                }
            }

            // --- Action Buttons Row ---
            AnimatedVisibility(
                visible = showNext,
                enter = fadeIn(tween(250)) + scaleIn(tween(250)),
                exit = fadeOut() + scaleOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Restart as secondary (small)
                    FilledTonalButton(
                        onClick = {
                            currentIndex = 0
                            score = 0
                            fullPlaces.shuffle()
                            sharedPref.edit().clear().apply()
                        },
                        modifier = Modifier
                            .weight(0.35f)
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Filled.Refresh, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Reset", fontWeight = FontWeight.SemiBold)
                    }

                    // Next / Finish Primary - Gradient button
                    val nextText = if (currentIndex == fullPlaces.size - 1) "Finish Journey" else "Next Challenge"
                    Button(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            if (currentIndex >= fullPlaces.size - 1) {
                                sharedPref.edit().clear().apply()
                                onFinishGame(score, fullPlaces.size)
                            } else {
                                currentIndex++
                                saveProgress()
                            }
                        },
                        modifier = Modifier
                            .weight(0.65f)
                            .height(56.dp)
                            .shadow(
                                elevation = 12.dp,
                                shape = RoundedCornerShape(16.dp),
                                spotColor = PrimaryLight.copy(alpha = 0.5f)
                            ),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryLight,
                            contentColor = Color.White
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                    ) {
                        Text(
                            nextText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("→", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

enum class OptionState { Default, Correct, Wrong, Disabled }

@Composable
fun TopBar(score: Int, total: Int, current: Int, onRestart: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Logo / Brand
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(GradientPrimaryStart, GradientPrimaryEnd)
                        )
                    )
                    .shadow(8.dp, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.EmojiEvents,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    "GUESS THE",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "PLACE • INDIA",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.3).sp
                )
            }
        }

        // Score Pill + Restart
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Score Chip
            Surface(
                shape = RoundedCornerShape(100.dp),
                color = GoldContainerLight,
                contentColor = Color(0xFF5D4200),
                shadowElevation = 0.dp,
                border = BorderStroke(1.dp, GoldLight.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Rounded.Star,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = GoldLight
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "$score",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        "/$total",
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        color = Color(0xFF5D4200).copy(alpha = 0.7f)
                    )
                }
            }

            // Restart icon
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                modifier = Modifier.size(40.dp),
                onClick = onRestart
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Filled.Refresh,
                        contentDescription = "Restart",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun TimerChip(timeLeft: Int, totalTime: Int) {
    val isLow = timeLeft <= 3
    val isMedium = timeLeft <= 6

    val bgColor by animateColorAsState(
        targetValue = when {
            isLow -> ErrorContainerLight
            isMedium -> GoldContainerLight
            else -> MaterialTheme.colorScheme.surfaceContainerHighest
        },
        label = "timerBg"
    )
    val contentColor by animateColorAsState(
        targetValue = when {
            isLow -> ErrorDark
            isMedium -> Color(0xFF5D4200)
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        label = "timerContent"
    )

    val progress = timeLeft / totalTime.toFloat()

    Surface(
        shape = RoundedCornerShape(100.dp),
        color = bgColor,
        contentColor = contentColor,
        border = BorderStroke(
            1.dp,
            if (isLow) Color.Red.copy(alpha = 0.3f) else Color.Transparent
        )
    ) {
        Column {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Timer,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "${timeLeft}s",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                if (isLow) {
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        Icons.Rounded.Bolt,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = Color.Red
                    )
                }
            }
            // mini progress inside chip bottom
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                color = if (isLow) Color.Red else if (isMedium) GoldLight else PrimaryLight,
                trackColor = Color.Transparent,
                strokeCap = StrokeCap.Round
            )
        }
    }
}

@Composable
fun PlaceImageCardPro(place: Place, scale: Float, isAnswered: Boolean) {
    val shape = RoundedCornerShape(28.dp)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .shadow(
                elevation = 20.dp,
                shape = shape,
                spotColor = PrimaryLight.copy(alpha = 0.25f),
                ambientColor = Color.Black.copy(alpha = 0.1f)
            ),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
        ) {
            // Image
            Image(
                painter = painterResource(id = place.imageResId),
                contentDescription = place.correctAnswer,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(shape)
            )

            // Top gradient scrim
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.45f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // Bottom gradient scrim for text protection
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.55f)
                            )
                        )
                    )
            )

            // Location badge top-start
            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(14.dp),
                shape = RoundedCornerShape(100.dp),
                color = Color.Black.copy(alpha = 0.45f),
                contentColor = Color.White
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = Color.White
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "INDIA • HERITAGE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                }
            }

            // Reveal answer chip if answered
            AnimatedVisibility(
                visible = isAnswered,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(14.dp),
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF2E7D32))
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = place.correctAnswer,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color(0xFF1B1B1F)
                        )
                    }
                }
            }

            // Subtle border overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(shape)
                    .background(Color.Transparent)
            )
        }
    }
}

@Composable
fun OptionCardPro(
    letter: String,
    text: String,
    state: OptionState,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val haptics = LocalHapticFeedback.current

    val backgroundColor by animateColorAsState(
        targetValue = when (state) {
            OptionState.Default -> MaterialTheme.colorScheme.surfaceContainerLow
            OptionState.Correct -> SuccessContainerLight
            OptionState.Wrong -> ErrorContainerLight
            OptionState.Disabled -> MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.6f)
        },
        animationSpec = tween(300),
        label = "optionBg"
    )
    val borderColor by animateColorAsState(
        targetValue = when (state) {
            OptionState.Default -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
            OptionState.Correct -> Color(0xFF2E7D32)
            OptionState.Wrong -> Color(0xFFBA1A1A)
            OptionState.Disabled -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
        },
        label = "border"
    )
    val letterBg by animateColorAsState(
        targetValue = when (state) {
            OptionState.Default -> MaterialTheme.colorScheme.surfaceContainerHighest
            OptionState.Correct -> Color(0xFF2E7D32)
            OptionState.Wrong -> Color(0xFFBA1A1A)
            OptionState.Disabled -> MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f)
        },
        label = "letterBg"
    )
    val letterColor by animateColorAsState(
        targetValue = when (state) {
            OptionState.Default -> MaterialTheme.colorScheme.onSurfaceVariant
            OptionState.Correct, OptionState.Wrong -> Color.White
            OptionState.Disabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        },
        label = "letterColor"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = BorderStroke(
            width = if (state == OptionState.Default) 1.dp else 1.8.dp,
            color = borderColor
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (state == OptionState.Default) 0.dp else 2.dp
        ),
        onClick = {
            if (enabled) {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
        },
        enabled = enabled
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Letter circle
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(letterBg),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        letter,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        color = letterColor
                    )
                }
                Spacer(Modifier.width(14.dp))
                Text(
                    text,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = when (state) {
                        OptionState.Disabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
            }

            // State icon
            AnimatedContent(
                targetState = state,
                transitionSpec = {
                    (fadeIn(tween(200)) + scaleIn(tween(200)))
                        .togetherWith(fadeOut(tween(100)) + scaleOut(tween(100)))
                },
                label = "stateIcon"
            ) { target ->
                when (target) {
                    OptionState.Correct -> {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF2E7D32)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    OptionState.Wrong -> {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFBA1A1A)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    else -> {
                        Box(modifier = Modifier.size(28.dp))
                    }
                }
            }
        }
    }
}
