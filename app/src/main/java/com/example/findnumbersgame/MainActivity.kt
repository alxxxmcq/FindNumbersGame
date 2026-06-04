package com.example.findnumbersgame

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.GridLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.findnumbersgame.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private var numbers = mutableListOf<Int>()
    private var targetNumber = 1
    private var mistakes = 0
    private var secondsElapsed = 0
    private var timerRunning = false

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var runnable: Runnable

    private val buttonMap = mutableMapOf<Button, Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNewGame()

        binding.btnNewGame.setOnClickListener {
            setupNewGame()
        }
    }

    private fun setupNewGame() {
        timerRunning = false
        handler.removeCallbacksAndMessages(null)

        targetNumber = 1
        mistakes = 0
        secondsElapsed = 0
        updateInfo()

        numbers = (1..25).toMutableList()
        numbers.shuffle()

        binding.gridGame.removeAllViews()
        buttonMap.clear()

        val grid = binding.gridGame
        val totalColumns = 5

        for (i in numbers.indices) {
            val number = numbers[i]

            val button = Button(this)
            button.text = number.toString()
            button.textSize = 18f
            button.setBackgroundResource(android.R.drawable.btn_default)

            val params = GridLayout.LayoutParams()
            params.width = 0
            params.height = GridLayout.LayoutParams.WRAP_CONTENT
            params.columnSpec = GridLayout.spec(i % totalColumns, 1f)
            params.rowSpec = GridLayout.spec(i / totalColumns, 1f)
            params.setMargins(4, 4, 4, 4)

            button.layoutParams = params

            button.setOnClickListener {
                handleButtonClick(button, number)
            }

            grid.addView(button)
            buttonMap[button] = number
        }

        startTimer()
    }

    private fun handleButtonClick(button: Button, number: Int) {
        if (!timerRunning) return

        if (number == targetNumber) {
            button.isEnabled = false
            button.setBackgroundColor(getColor(android.R.color.holo_green_light))

            targetNumber++

            if (targetNumber > 25) {
                timerRunning = false
                handler.removeCallbacksAndMessages(null)

                Toast.makeText(
                    this,
                    "Поздравляем! Вы нашли все числа за ${formatTime(secondsElapsed)}",
                    Toast.LENGTH_LONG
                ).show()
            }
        } else {
            mistakes++
            button.setBackgroundColor(getColor(android.R.color.holo_red_light))

            button.postDelayed({
                if (button.isEnabled) {
                    button.setBackgroundResource(android.R.drawable.btn_default)
                }
            }, 200)
        }

        updateInfo()
    }

    private fun startTimer() {
        timerRunning = true

        runnable = object : Runnable {
            override fun run() {
                if (timerRunning) {
                    secondsElapsed++
                    updateInfo()
                    handler.postDelayed(this, 1000)
                }
            }
        }

        handler.post(runnable)
    }

    private fun updateInfo() {
        binding.tvTargetNumber.text = targetNumber.toString()
        binding.tvMistakes.text = mistakes.toString()
        binding.tvTimer.text = formatTime(secondsElapsed)
    }

    private fun formatTime(sec: Int): String {
        val minutes = sec / 60
        val seconds = sec % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    override fun onDestroy() {
        super.onDestroy()
        timerRunning = false
        handler.removeCallbacksAndMessages(null)
    }
}