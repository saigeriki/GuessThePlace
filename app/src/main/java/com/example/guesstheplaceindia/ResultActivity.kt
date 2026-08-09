package com.example.guesstheplaceindia

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.guesstheplaceindia.ui.theme.BackgroundDark
import com.example.guesstheplaceindia.ui.theme.ErrorContainerLight
import com.example.guesstheplaceindia.ui.theme.GoldContainerLight
import com.example.guesstheplaceindia.ui.theme.GoldLight
import com.example.guesstheplaceindia.ui.theme.GradientAccentEnd
import com.example.guesstheplaceindia.ui.theme.GradientAccentStart
import com.example.guesstheplaceindia.ui.theme.GradientPrimaryEnd
import com.example.guesstheplaceindia.ui.theme.GradientPrimaryStart
import com.example.guesstheplaceindia.ui.theme.GuessThePlaceIndiaTheme
import com.example.guesstheplaceindia.ui.theme.PrimaryLight
import com.example.guesstheplaceindia.ui.theme.SuccessContainerLight
import kotlinx.coroutines.delay
import kotlin.random.Random

class ResultActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val score = intent.getIntExtra("score", 0)
        val total = intent.getIntExtra("total", 0)
        setContent {
            GuessThePlaceIndiaTheme {
                ResultScreenPro(score = score, total = total,
                    onPlayAgain = {
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    },
                    onShare = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "I scored $score / $total in Guess The Place India! 🇮🇳 Can you beat my score? #GuessThePlaceIndia")
                        }
                        startActivity(Intent.createChooser(shareIntent, "Share your score"))
                    }
                )
            }
        }
    }
}

@Composable
fun ResultScreenPro(score: Int, total: Int, onPlayAgain: () -> Unit, onShare: () -> Unit) {
    val percent = if (total > 0) (score * 100) / total else 0

    val title = when {
        percent >= 90 -> "Legendary!"
        percent >= 80 -> "Excellent!"
        percent >= 60 -> "Great Job!"
        percent >= 40 -> "Good Try!"
        else -> "Keep Exploring!"
    }
    val subtitle = when {
        percent >= 90 -> "You're a true explorer of Incredible India! 🎉"
        percent >= 80 -> "Outstanding knowledge of Indian heritage! ✨"
        percent >= 60 -> "You know India quite well! Keep traveling! 🌍"
        percent >= 40 -> "Not bad! India has so much more to discover."
        else -> "Every expert was once a beginner. Try again! 💪"
    }
    val emoji = when {
        percent >= 90 -> "🏆"
        percent >= 80 -> "🎉"
        percent >= 60 -> "👍"
        percent >= 40 -> "🌟"
        else -> "💪"
    }

    var animateScore by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(300)
        animateScore = true
    }
    val progress by animateFloatAsState(
        targetValue = if (animateScore) percent / 100f else 0f,
        animationSpec = tween(1200, easing = FastOutSlowInEasing),
        label = "scoreProgress"
    )

    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val backgroundBrush = if (isDark) {
        Brush.verticalGradient(
            listOf(Color(0xFF1E1B2E), Color(0xFF121318), Color(0xFF0D0E13))
        )
    } else {
        Brush.verticalGradient(
            listOf(Color(0xFFE8E4FF), Color(0xFFF9F7FF), Color(0xFFFFFFFF))
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        // Confetti background if good score
        if (percent >= 50) {
            ConfettiPro(modifier = Modifier.fillMaxSize())
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(Modifier.height(12.dp))

            // Top badge
            Surface(
                shape = RoundedCornerShape(100.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                shadowElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2ECC71))
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "JOURNEY COMPLETED",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                }
            }

            // Main Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(24.dp, RoundedCornerShape(32.dp), spotColor = PrimaryLight.copy(0.2f)),
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Circular score
                    Box(
                        modifier = Modifier.size(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Background track
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawCircle(
                                color = Color(0xFFEAE7F4),
                                style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                        // Progress
                        CircularProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxSize(),
                            color = when {
                                percent >= 80 -> Color(0xFF2E7D32)
                                percent >= 50 -> PrimaryLight
                                else -> Color(0xFFFF7043)
                            },
                            strokeWidth = 14.dp,
                            trackColor = Color.Transparent,
                            strokeCap = StrokeCap.Round
                        )
                        // Center content
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                emoji,
                                fontSize = 32.sp
                            )
                            Spacer(Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    "$score",
                                    fontSize = 42.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    lineHeight = 42.sp
                                )
                                Text(
                                    "/$total",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 4.dp, start = 2.dp)
                                )
                            }
                            Text(
                                "$percent%",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryLight,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    Text(
                        title,
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )

                    Spacer(Modifier.height(24.dp))

                    // Stats row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatChipPro(
                            modifier = Modifier.weight(1f),
                            label = "Correct",
                            value = "$score",
                            icon = Icons.Rounded.Check,
                            containerColor = SuccessContainerLight,
                            contentColor = Color(0xFF1B5E20)
                        )
                        StatChipPro(
                            modifier = Modifier.weight(1f),
                            label = "Wrong",
                            value = "${total - score}",
                            icon = Icons.Rounded.Close,
                            containerColor = ErrorContainerLight,
                            contentColor = Color(0xFF93000A)
                        )
                        StatChipPro(
                            modifier = Modifier.weight(1f),
                            label = "Accuracy",
                            value = "$percent%",
                            icon = Icons.Rounded.Star,
                            containerColor = GoldContainerLight,
                            contentColor = Color(0xFF5D4200)
                        )
                    }
                }
            }

            // Message card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryLight.copy(alpha = 0.15f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.linearGradient(listOf(GradientPrimaryStart, GradientPrimaryEnd))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.EmojiEvents, null, tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Did you know?", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            "India has 43 UNESCO World Heritage Sites - you just explored ${total} of them!",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            // Action Buttons
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onPlayAgain,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .shadow(12.dp, RoundedCornerShape(16.dp), spotColor = PrimaryLight.copy(0.4f)),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryLight),
                    elevation = ButtonDefaults.buttonElevation(0.dp)
                ) {
                    Icon(Icons.Filled.Refresh, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Play Again", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FilledTonalButton(
                        onClick = onShare,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Filled.Share, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Share", fontWeight = FontWeight.SemiBold)
                    }
                    FilledTonalButton(
                        onClick = onPlayAgain,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Filled.Home, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Home", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun StatChipPro(
    modifier: Modifier,
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    containerColor: Color,
    contentColor: Color
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        contentColor = contentColor
    ) {
        Column(
            modifier = Modifier.padding(vertical = 14.dp, horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, null, modifier = Modifier.size(18.dp))
            Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.5.sp)
        }
    }
}

@Composable
fun ConfettiPro(modifier: Modifier = Modifier) {
    val particles = remember {
        List(35) {
            ConfettiParticle(
                x = Random.nextFloat(),
                y = Random.nextFloat() * -0.5f,
                size = Random.nextFloat() * 8 + 4,
                color = listOf(
                    Color(0xFF6C5CFF),
                    Color(0xFFFF7043),
                    Color(0xFF00C2A2),
                    Color(0xFFFFB300),
                    Color(0xFF9C27B0),
                    Color(0xFF2ECC71)
                ).random(),
                speed = Random.nextFloat() * 0.8f + 0.3f,
                rotation = Random.nextFloat() * 360f,
                rotationSpeed = Random.nextFloat() * 10 - 5
            )
        }
    }
    val infinite = rememberInfiniteTransition(label = "confetti")
    val anim by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "anim"
    )

    Canvas(modifier = modifier) {
        particles.forEach { p ->
            val yPos = (p.y + anim * p.speed) % 1.2f
            val xOffset = kotlin.math.sin((anim * 3 + p.x * 10).toDouble()).toFloat() * 0.02f
            drawCircle(
                color = p.color.copy(alpha = 0.8f),
                radius = p.size,
                center = Offset(
                    x = (p.x + xOffset).coerceIn(0f, 1f) * size.width,
                    y = yPos * size.height
                )
            )
        }
    }
}

data class ConfettiParticle(
    val x: Float,
    val y: Float,
    val size: Float,
    val color: Color,
    val speed: Float,
    val rotation: Float,
    val rotationSpeed: Float
)
