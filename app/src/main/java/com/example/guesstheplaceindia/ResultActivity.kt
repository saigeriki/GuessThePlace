package com.example.guesstheplaceindia

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import kotlin.random.Random

class ResultActivity : ComponentActivity() {

    private lateinit var tvResult: TextView
    private lateinit var btnPlayAgain: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)

        // Screen fade-in
        val rootView: View = findViewById(android.R.id.content)
        rootView.alpha = 0f
        rootView.animate().alpha(1f).setDuration(350).start()

        tvResult = findViewById(R.id.tvResult)
        btnPlayAgain = findViewById(R.id.btnPlayAgain)

        val score = intent.getIntExtra("score", 0)
        val total = intent.getIntExtra("total", 0)

        val percent = if (total > 0) (score * 100) / total else 0

        val message = when {
            percent >= 80 -> "Excellent 🎉"
            percent >= 50 -> "Good 👍"
            else -> "Try Again 💪"
        }

        tvResult.text = "Your Score : $score / $total\n$message"

        // Score bounce
        tvResult.scaleX = 0.6f
        tvResult.scaleY = 0.6f
        tvResult.animate()
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(300)
            .start()

        // Confetti only for good performance
        if (percent >= 50) {
            launchConfetti()
        }

        btnPlayAgain.setOnClickListener {

            btnPlayAgain.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(80)
                .withEndAction {

                    btnPlayAgain.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(80)
                        .start()

                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                }
                .start()
        }
    }

    private fun launchConfetti() {

        val root = findViewById<ViewGroup>(android.R.id.content)

        val colors = listOf(
            Color.RED,
            Color.GREEN,
            Color.BLUE,
            Color.YELLOW,
            Color.MAGENTA,
            Color.CYAN
        )

        val screenWidth = resources.displayMetrics.widthPixels
        val screenHeight = resources.displayMetrics.heightPixels

        repeat(25) {

            val confetti = View(this)

            val size = Random.nextInt(12, 24)
            val params = ViewGroup.LayoutParams(size, size)
            confetti.layoutParams = params

            confetti.setBackgroundColor(colors.random())

            confetti.x = Random.nextInt(0, screenWidth - size).toFloat()
            confetti.y = -50f

            root.addView(confetti)

            val endY = screenHeight + Random.nextInt(100, 400)

            confetti.animate()
                .translationY(endY.toFloat())
                .rotation(Random.nextInt(180, 720).toFloat())
                .alpha(0f)
                .setDuration(Random.nextLong(1200, 2000))
                .withEndAction {
                    root.removeView(confetti)
                }
                .start()
        }
    }
}