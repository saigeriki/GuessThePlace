package com.example.guesstheplaceindia

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity

data class Place(
    val imageResId: Int,
    val options: List<String>,
    val correctAnswer: String
)

class MainActivity : ComponentActivity() {

    private lateinit var imageView: ImageView
    private lateinit var btn1: Button
    private lateinit var btn2: Button
    private lateinit var btn3: Button
    private lateinit var btn4: Button
    private lateinit var btnNext: Button
    private lateinit var btnRestart: Button
    private lateinit var tvScore: TextView
    private lateinit var tvTimer: TextView
    private lateinit var tvQuestion: TextView

    private var countDownTimer: CountDownTimer? = null
    private val totalTime = 10000L

    private var currentIndex = 0
    private var score = 0
    private lateinit var sharedPref: android.content.SharedPreferences

    private val placesList = mutableListOf(
        Place(R.drawable.tajmahal, listOf("Taj Mahal","Qutub Minar","Charminar","Red Fort"), "Taj Mahal"),
        Place(R.drawable.charminar, listOf("Charminar","Golconda Fort","Mysore Palace","Red Fort"), "Charminar"),
        Place(R.drawable.gatewayofindia, listOf("Gateway of India","India Gate","Victoria Memorial","Hawa Mahal"), "Gateway of India"),
        Place(R.drawable.mysore_palace, listOf("Mysore Palace","City Palace","Umaid Bhawan","Falaknuma Palace"), "Mysore Palace"),
        Place(R.drawable.qutub_minar, listOf("Qutub Minar","Charminar","Taj Mahal","Gateway of India"), "Qutub Minar"),
        Place(R.drawable.hawa_mahal, listOf("Hawa Mahal","Jal Mahal","City Palace","Amber Fort"), "Hawa Mahal"),
        Place(R.drawable.golden_temple, listOf("Golden Temple","Akshardham","Iskcon Temple","Meenakshi Temple"), "Golden Temple"),
        Place(R.drawable.india_gate, listOf("India Gate","Gateway of India","Red Fort","Qutub Minar"), "India Gate"),
        Place(R.drawable.red_fort, listOf("Red Fort","Amber Fort","Golconda Fort","Agra Fort"), "Red Fort"),
        Place(R.drawable.lotus_temple, listOf("Lotus Temple","Akshardham","Iskcon Temple","Golden Temple"), "Lotus Temple"),
        Place(R.drawable.meenakshi_temple, listOf("Meenakshi Temple","Brihadeeswara Temple","Iskcon Temple","Akshardham"), "Meenakshi Temple"),
        Place(R.drawable.akshardham, listOf("Akshardham","Lotus Temple","Iskcon Temple","Golden Temple"), "Akshardham"),
        Place(R.drawable.victoria_memorial, listOf("Victoria Memorial","Howrah Bridge","India Gate","Gateway of India"), "Victoria Memorial"),
        Place(R.drawable.ajanta_caves, listOf("Ajanta Caves","Ellora Caves","Elephanta Caves","Badami Caves"), "Ajanta Caves"),
        Place(R.drawable.ellora_caves, listOf("Ellora Caves","Ajanta Caves","Elephanta Caves","Badami Caves"), "Ellora Caves"),
        Place(R.drawable.sanchi_stupa, listOf("Sanchi Stupa","Dhamek Stupa","Mahabodhi Temple","Nalanda"), "Sanchi Stupa"),
        Place(R.drawable.konark_sun_temple, listOf("Konark Sun Temple","Jagannath Temple","Brihadeeswara Temple","Meenakshi Temple"), "Konark Sun Temple")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        imageView = findViewById(R.id.imageViewPlace)
        btn1 = findViewById(R.id.btnOption1)
        btn2 = findViewById(R.id.btnOption2)
        btn3 = findViewById(R.id.btnOption3)
        btn4 = findViewById(R.id.btnOption4)
        btnNext = findViewById(R.id.btnNext)
        btnRestart = findViewById(R.id.btnRestart)
        tvScore = findViewById(R.id.tvScore)
        tvTimer = findViewById(R.id.tvTimer)
        tvQuestion = findViewById(R.id.tvQuestion)

        sharedPref = getSharedPreferences("GuessPlacePrefs", MODE_PRIVATE)
        currentIndex = sharedPref.getInt("currentIndex", 0)
        score = sharedPref.getInt("score", 0)

        placesList.shuffle()
        loadQuestion()

        btn1.setOnClickListener { checkAnswer(btn1, btn1.text.toString()) }
        btn2.setOnClickListener { checkAnswer(btn2, btn2.text.toString()) }
        btn3.setOnClickListener { checkAnswer(btn3, btn3.text.toString()) }
        btn4.setOnClickListener { checkAnswer(btn4, btn4.text.toString()) }

        btnNext.setOnClickListener {

            btnNext.animate()
                .scaleX(0.92f)
                .scaleY(0.92f)
                .setDuration(80)
                .withEndAction {

                    btnNext.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(80)
                        .start()

                    btnNext.visibility = View.GONE
                    enableOptionButtons(true)
                    resetButtonColors()
                    currentIndex++
                    saveProgress()
                    loadQuestion()
                }
                .start()
        }

        btnRestart.setOnClickListener {
            restartGame()
        }
    }

    private fun loadQuestion() {

        if (currentIndex >= placesList.size) {
            sharedPref.edit().clear().apply()
            val intent = Intent(this, ResultActivity::class.java)
            intent.putExtra("score", score)
            intent.putExtra("total", placesList.size)
            startActivity(intent)
            finish()
            return
        }

        val place = placesList[currentIndex]

        imageView.alpha = 0f
        imageView.scaleX = 0.9f
        imageView.scaleY = 0.9f
        imageView.setImageResource(place.imageResId)

        imageView.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(250)
            .start()

        val shuffledOptions = place.options.shuffled()

        btn1.text = shuffledOptions[0]
        btn2.text = shuffledOptions[1]
        btn3.text = shuffledOptions[2]
        btn4.text = shuffledOptions[3]

        animateOptionButtons()

        tvQuestion.text = "Question : ${currentIndex + 1} / ${placesList.size}"

        // ✅ Next / Finish text handling
        if (currentIndex == placesList.size - 1) {
            btnNext.text = "Finish"
        } else {
            btnNext.text = "Next"
        }

        tvQuestion.alpha = 0f
        tvQuestion.scaleX = 0.8f
        tvQuestion.scaleY = 0.8f

        tvQuestion.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(200)
            .start()

        btnNext.visibility = View.GONE
        enableOptionButtons(true)
        resetButtonColors()
        updateScoreText()
        startTimer()
    }

    private fun animateOptionButtons() {
        val buttons = listOf(btn1, btn2, btn3, btn4)

        buttons.forEachIndexed { index, button ->
            button.alpha = 0f
            button.translationY = 20f

            button.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay((index * 50).toLong())
                .setDuration(150)
                .start()
        }
    }

    private fun checkAnswer(selectedButton: Button, selectedAnswer: String) {

        enableOptionButtons(false)

        selectedButton.isHapticFeedbackEnabled = true
        selectedButton.performHapticFeedback(
            HapticFeedbackConstants.CONFIRM,
            HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
        )

        selectedButton.animate()
            .scaleX(0.92f)
            .scaleY(0.92f)
            .setDuration(80)
            .withEndAction {
                selectedButton.animate().scaleX(1f).scaleY(1f).setDuration(80).start()
            }.start()

        countDownTimer?.cancel()

        val correctAnswer = placesList[currentIndex].correctAnswer

        if (selectedAnswer != correctAnswer) {
            shakeButton(selectedButton)
        }

        highlightOptions(selectedButton, correctAnswer)

        if (selectedAnswer == correctAnswer) {
            score++
            saveProgress()
            updateScoreText()
            Toast.makeText(this, "Correct!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Wrong! Answer: $correctAnswer", Toast.LENGTH_SHORT).show()
        }
    }

    private fun highlightOptions(selectedButton: Button, correctAnswer: String) {
        val buttons = listOf(btn1, btn2, btn3, btn4)

        var correctBtn: Button? = null

        buttons.forEach { btn ->
            when {
                btn.text == correctAnswer -> {
                    btn.setBackgroundColor(Color.GREEN)
                    correctBtn = btn
                }
                btn == selectedButton -> btn.setBackgroundColor(Color.RED)
                else -> btn.setBackgroundColor(Color.LTGRAY)
            }
            btn.isEnabled = false
        }

        correctBtn?.let { flashCorrectButton(it) }

        btnNext.alpha = 0f
        btnNext.visibility = View.VISIBLE
        btnNext.animate().alpha(1f).setDuration(200).start()
    }

    private fun flashCorrectButton(button: Button) {
        button.animate()
            .alpha(0.3f)
            .setDuration(80)
            .withEndAction {
                button.animate()
                    .alpha(1f)
                    .setDuration(80)
                    .withEndAction {
                        button.animate()
                            .alpha(0.3f)
                            .setDuration(80)
                            .withEndAction {
                                button.animate()
                                    .alpha(1f)
                                    .setDuration(80)
                                    .start()
                            }.start()
                    }.start()
            }.start()
    }

    private fun shakeButton(button: Button) {
        button.animate()
            .translationX(10f)
            .setDuration(40)
            .withEndAction {
                button.animate()
                    .translationX(-10f)
                    .setDuration(40)
                    .withEndAction {
                        button.animate()
                            .translationX(6f)
                            .setDuration(40)
                            .withEndAction {
                                button.animate()
                                    .translationX(0f)
                                    .setDuration(40)
                                    .start()
                            }.start()
                    }.start()
            }.start()
    }

    private fun enableOptionButtons(enable: Boolean) {
        btn1.isEnabled = enable
        btn2.isEnabled = enable
        btn3.isEnabled = enable
        btn4.isEnabled = enable
    }

    private fun resetButtonColors() {
        val buttons = listOf(btn1, btn2, btn3, btn4)
        buttons.forEach { it.setBackgroundColor(Color.LTGRAY) }
    }

    private fun saveProgress() {
        sharedPref.edit()
            .putInt("currentIndex", currentIndex)
            .putInt("score", score)
            .apply()
    }

    private fun updateScoreText() {
        tvScore.text = "Score : $score / ${placesList.size}"

        tvScore.scaleX = 0.8f
        tvScore.scaleY = 0.8f
        tvScore.animate().scaleX(1f).scaleY(1f).setDuration(200).start()
    }

    private fun restartGame() {
        countDownTimer?.cancel()
        currentIndex = 0
        score = 0
        placesList.shuffle()
        sharedPref.edit().clear().apply()
        loadQuestion()
    }

    private fun startTimer() {
        countDownTimer?.cancel()
        tvTimer.text = "Time : 10"

        countDownTimer = object : CountDownTimer(totalTime, 1000) {

            override fun onTick(millisUntilFinished: Long) {
                tvTimer.text = "Time : ${millisUntilFinished / 1000}"
            }

            override fun onFinish() {
                tvTimer.text = "Time : 0"

                val correctAnswer = placesList[currentIndex].correctAnswer
                val buttons = listOf(btn1, btn2, btn3, btn4)

                var correctBtn: Button? = null

                buttons.forEach { btn ->
                    if (btn.text == correctAnswer) {
                        btn.setBackgroundColor(Color.GREEN)
                        correctBtn = btn
                    } else {
                        btn.setBackgroundColor(Color.LTGRAY)
                    }
                    btn.isEnabled = false
                }

                correctBtn?.let { flashCorrectButton(it) }

                btnNext.alpha = 0f
                btnNext.visibility = View.VISIBLE
                btnNext.animate().alpha(1f).setDuration(200).start()

                Handler(Looper.getMainLooper()).postDelayed({
                    if (!isFinishing && !isDestroyed)
                        btnNext.performClick()
                }, 1000)
            }
        }.start()
    }
}