package com.example.findnumbersgame

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.GridLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.findnumbersgame.databinding.ActivityMainBinding
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private enum class GameMode {
        CLASSIC,
        TEN,
        GUESS
    }

    private var currentMode = GameMode.CLASSIC

    private var numbers = mutableListOf<Int>()
    private var targetNumber = 1
    private var mistakes = 0
    private var secondsElapsed = 0
    private var timerRunning = false

    private var foundPairs = 0
    private var selectedButton: Button? = null
    private var selectedNumber: Int? = null

    private var secretNumber = 0

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var runnable: Runnable

    private val prefs by lazy {
        getSharedPreferences("records", Context.MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnClassicMode.setOnClickListener {
            currentMode = GameMode.CLASSIC
            setupClassicGame()
        }

        binding.btnTenMode.setOnClickListener {
            currentMode = GameMode.TEN
            setupTenGame()
        }

        binding.btnGuessMode.setOnClickListener {
            currentMode = GameMode.GUESS
            setupGuessGame()
        }

        binding.btnNewGame.setOnClickListener {
            when (currentMode) {
                GameMode.CLASSIC -> setupClassicGame()
                GameMode.TEN -> setupTenGame()
                GameMode.GUESS -> setupGuessGame()
            }
        }

        binding.btnNewGame.setOnLongClickListener {
            clearRecords()
            Toast.makeText(this, "Рекорды сброшены", Toast.LENGTH_SHORT).show()
            true
        }

        binding.btnCheckGuess.setOnClickListener {
            checkGuess()
        }

        loadRecords()
        setupClassicGame()
    }

    private fun setupClassicGame() {
        stopTimer()

        currentMode = GameMode.CLASSIC
        binding.gridGame.visibility = View.VISIBLE
        binding.guessLayout.visibility = View.GONE

        binding.tvTitle.text = "НАЙДИ ЧИСЛА"
        targetNumber = 1
        mistakes = 0
        secondsElapsed = 0
        updateInfo()

        numbers = (1..25).toMutableList()
        numbers.shuffle()

        binding.gridGame.columnCount = 5
        binding.gridGame.rowCount = 5
        binding.gridGame.removeAllViews()

        for (i in numbers.indices) {
            val number = numbers[i]
            val button = createGridButton(number.toString(), i, 5)

            button.setOnClickListener {
                handleClassicClick(button, number)
            }

            binding.gridGame.addView(button)
        }

        startTimer()
    }

    private fun setupTenGame() {
        stopTimer()

        currentMode = GameMode.TEN
        binding.gridGame.visibility = View.VISIBLE
        binding.guessLayout.visibility = View.GONE

        binding.tvTitle.text = "СОБЕРИ 10"
        foundPairs = 0
        mistakes = 0
        secondsElapsed = 0
        selectedButton = null
        selectedNumber = null
        binding.tvTargetNumber.text = "Пары: 0"
        binding.tvMistakes.text = mistakes.toString()
        binding.tvTimer.text = formatTime(secondsElapsed)

        val tenNumbers = MutableList(25) { Random.nextInt(1, 10) }

        binding.gridGame.columnCount = 5
        binding.gridGame.rowCount = 5
        binding.gridGame.removeAllViews()

        for (i in tenNumbers.indices) {
            val number = tenNumbers[i]
            val button = createGridButton(number.toString(), i, 5)

            button.setOnClickListener {
                handleTenClick(button, number)
            }

            binding.gridGame.addView(button)
        }

        startTimer()
    }

    private fun setupGuessGame() {
        stopTimer()

        currentMode = GameMode.GUESS
        binding.gridGame.visibility = View.GONE
        binding.guessLayout.visibility = View.VISIBLE

        binding.tvTitle.text = "УГАДАЙ ЧИСЛО"
        mistakes = 0
        secondsElapsed = 0
        secretNumber = Random.nextInt(1, 101)

        binding.tvTargetNumber.text = "1–100"
        binding.tvMistakes.text = "0"
        binding.tvTimer.text = formatTime(secondsElapsed)
        binding.tvGuessHint.text = "Число загадано"
        binding.editGuessNumber.text.clear()

        startTimer()
    }

    private fun createGridButton(text: String, index: Int, columns: Int): Button {
        val button = Button(this)
        button.text = text
        button.textSize = 18f
        button.setBackgroundResource(android.R.drawable.btn_default)

        val params = GridLayout.LayoutParams()
        params.width = 0
        params.height = GridLayout.LayoutParams.WRAP_CONTENT
        params.columnSpec = GridLayout.spec(index % columns, 1f)
        params.rowSpec = GridLayout.spec(index / columns, 1f)
        params.setMargins(4, 4, 4, 4)

        button.layoutParams = params
        return button
    }

    private fun handleClassicClick(button: Button, number: Int) {
        if (!timerRunning) return

        if (number == targetNumber) {
            button.isEnabled = false
            button.setBackgroundColor(getColor(android.R.color.holo_green_light))

            targetNumber++

            if (targetNumber > 25) {
                finishClassicGame()
            }
        } else {
            mistakes++
            showWrongButton(button)
        }

        updateInfo()
    }

    private fun handleTenClick(button: Button, number: Int) {
        if (!timerRunning || !button.isEnabled) return

        if (selectedButton == null) {
            selectedButton = button
            selectedNumber = number
            button.setBackgroundColor(getColor(android.R.color.holo_blue_light))
            return
        }

        if (selectedButton == button) {
            return
        }

        val firstButton = selectedButton
        val firstNumber = selectedNumber

        if (firstButton != null && firstNumber != null && firstNumber + number == 10) {
            firstButton.isEnabled = false
            button.isEnabled = false
            firstButton.setBackgroundColor(getColor(android.R.color.holo_green_light))
            button.setBackgroundColor(getColor(android.R.color.holo_green_light))
            foundPairs++
            binding.tvTargetNumber.text = "Пары: $foundPairs"
        } else {
            mistakes++
            firstButton?.setBackgroundResource(android.R.drawable.btn_default)
            showWrongButton(button)
        }

        selectedButton = null
        selectedNumber = null
        binding.tvMistakes.text = mistakes.toString()

        if (allButtonsDisabled()) {
            finishTenGame()
        }
    }

    private fun checkGuess() {
        if (!timerRunning) return

        val userText = binding.editGuessNumber.text.toString().trim()
        val userNumber = userText.toIntOrNull()

        if (userNumber == null || userNumber !in 1..100) {
            mistakes++
            binding.tvMistakes.text = mistakes.toString()
            Toast.makeText(this, "Введите число от 1 до 100", Toast.LENGTH_SHORT).show()
            return
        }

        when {
            userNumber < secretNumber -> {
                mistakes++
                binding.tvGuessHint.text = "Больше"
            }
            userNumber > secretNumber -> {
                mistakes++
                binding.tvGuessHint.text = "Меньше"
            }
            else -> {
                binding.tvGuessHint.text = "Поздравляю! Число найдено"
                stopTimer()
                Toast.makeText(
                    this,
                    "Вы угадали за ${formatTime(secondsElapsed)}. Ошибок: $mistakes",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        binding.tvMistakes.text = mistakes.toString()
    }

    private fun showWrongButton(button: Button) {
        button.setBackgroundColor(getColor(android.R.color.holo_red_light))
        button.postDelayed({
            if (button.isEnabled) {
                button.setBackgroundResource(android.R.drawable.btn_default)
            }
        }, 200)
    }

    private fun allButtonsDisabled(): Boolean {
        for (i in 0 until binding.gridGame.childCount) {
            val view = binding.gridGame.getChildAt(i)
            if (view is Button && view.isEnabled) {
                return false
            }
        }
        return true
    }

    private fun finishClassicGame() {
        stopTimer()

        if (mistakes < 5) {
            saveBestClassicRecord()
        }

        Toast.makeText(
            this,
            "Поздравляем! Все числа найдены за ${formatTime(secondsElapsed)}",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun finishTenGame() {
        stopTimer()
        saveBestTenRecord()

        Toast.makeText(
            this,
            "Игра окончена! Найдено пар: $foundPairs",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun updateInfo() {
        if (currentMode == GameMode.CLASSIC) {
            binding.tvTargetNumber.text = targetNumber.toString()
        }

        binding.tvMistakes.text = mistakes.toString()
        binding.tvTimer.text = formatTime(secondsElapsed)
    }

    private fun startTimer() {
        timerRunning = true

        runnable = object : Runnable {
            override fun run() {
                if (timerRunning) {
                    secondsElapsed++
                    binding.tvTimer.text = formatTime(secondsElapsed)
                    handler.postDelayed(this, 1000)
                }
            }
        }

        handler.post(runnable)
    }

    private fun stopTimer() {
        timerRunning = false
        handler.removeCallbacksAndMessages(null)
    }

    private fun formatTime(sec: Int): String {
        val minutes = sec / 60
        val seconds = sec % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    private fun saveBestClassicRecord() {
        val bestTime = prefs.getInt("classic_best_time", -1)
        val bestMistakes = prefs.getInt("classic_best_mistakes", -1)

        if (bestTime == -1 || secondsElapsed < bestTime) {
            prefs.edit()
                .putInt("classic_best_time", secondsElapsed)
                .putInt("classic_best_mistakes", mistakes)
                .apply()
        } else if (secondsElapsed == bestTime && (bestMistakes == -1 || mistakes < bestMistakes)) {
            prefs.edit()
                .putInt("classic_best_mistakes", mistakes)
                .apply()
        }

        loadRecords()
    }

    private fun saveBestTenRecord() {
        val bestPairs = prefs.getInt("ten_best_pairs", -1)

        if (foundPairs > bestPairs) {
            prefs.edit()
                .putInt("ten_best_pairs", foundPairs)
                .putInt("ten_best_time", secondsElapsed)
                .apply()
        }

        loadRecords()
    }

    private fun loadRecords() {
        val bestTime = prefs.getInt("classic_best_time", -1)
        val bestMistakes = prefs.getInt("classic_best_mistakes", -1)
        val bestPairs = prefs.getInt("ten_best_pairs", -1)

        binding.tvBestTime.text = if (bestTime == -1) {
            "Лучшее время: —"
        } else {
            "Лучшее время: ${formatTime(bestTime)}"
        }

        binding.tvBestMistakes.text = if (bestMistakes == -1 && bestPairs == -1) {
            "Мин. ошибок: —"
        } else if (bestPairs != -1) {
            "Мин. ошибок: $bestMistakes | Пар: $bestPairs"
        } else {
            "Мин. ошибок: $bestMistakes"
        }
    }

    private fun clearRecords() {
        prefs.edit().clear().apply()
        loadRecords()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopTimer()
    }
}