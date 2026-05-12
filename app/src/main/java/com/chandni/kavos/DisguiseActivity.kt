package com.chandni.kavos

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class DisguiseActivity : AppCompatActivity() {

    private var currentInput = ""
    private var storedValue = 0.0
    private var pendingOperator = ""
    private var freshInput = false
    private var inputSequence = ""
    private lateinit var SECRET: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_disguise)

        val prefs = getSharedPreferences("KAVOS_PREFS", MODE_PRIVATE)
        val storedCode = prefs.getString("secret_code", "111") ?: "111"
        SECRET = "$storedCode="

        val tvDisplay = findViewById<TextView>(R.id.tvCalcDisplay)
        val tvHistory = findViewById<TextView>(R.id.tvCalcHistory)
        tvDisplay.text = "0"

        val numberMap = mapOf(
            R.id.btn0 to "0", R.id.btn1 to "1", R.id.btn2 to "2",
            R.id.btn3 to "3", R.id.btn4 to "4", R.id.btn5 to "5",
            R.id.btn6 to "6", R.id.btn7 to "7", R.id.btn8 to "8",
            R.id.btn9 to "9", R.id.btnDot to "."
        )

        for ((id, value) in numberMap) {
            findViewById<Button>(id).setOnClickListener {
                inputSequence += value
                checkSecretCode()
                if (freshInput) { currentInput = ""; freshInput = false }
                if (value == "." && currentInput.contains(".")) return@setOnClickListener
                currentInput += value
                tvDisplay.text = currentInput
            }
        }

        val operatorMap = mapOf(
            R.id.btnPlus to "+", R.id.btnMinus to "-",
            R.id.btnMultiply to "×", R.id.btnDivide to "÷"
        )

        for ((id, op) in operatorMap) {
            findViewById<Button>(id).setOnClickListener {
                if (currentInput.isNotEmpty()) {
                    storedValue = currentInput.toDoubleOrNull() ?: 0.0
                }
                pendingOperator = op
                tvHistory.text = "$storedValue $op"
                freshInput = true
            }
        }

        findViewById<Button>(R.id.btnEquals).setOnClickListener {
            inputSequence += "="
            checkSecretCode()

            val inputVal = currentInput.toDoubleOrNull() ?: 0.0
            val result = when (pendingOperator) {
                "+" -> storedValue + inputVal
                "-" -> storedValue - inputVal
                "×" -> storedValue * inputVal
                "÷" -> if (inputVal != 0.0) storedValue / inputVal else 0.0
                else -> inputVal
            }

            // Show clean result — no unnecessary decimals
            val display = if (result == result.toLong().toDouble()) {
                result.toLong().toString()
            } else {
                String.format("%.6g", result)
            }

            tvHistory.text = "$storedValue $pendingOperator $inputVal ="
            tvDisplay.text = display
            currentInput = display
            pendingOperator = ""
            freshInput = true
        }

        findViewById<Button>(R.id.btnClear).setOnClickListener {
            currentInput = ""
            storedValue = 0.0
            pendingOperator = ""
            freshInput = false
            inputSequence = ""
            tvDisplay.text = "0"
            tvHistory.text = ""
        }

        findViewById<Button>(R.id.btnPlusMinus).setOnClickListener {
            val value = currentInput.toDoubleOrNull() ?: 0.0
            val toggled = value * -1
            currentInput = if (toggled == toggled.toLong().toDouble())
                toggled.toLong().toString() else toggled.toString()
            tvDisplay.text = currentInput
        }

        findViewById<Button>(R.id.btnPercent).setOnClickListener {
            val value = currentInput.toDoubleOrNull() ?: 0.0
            val percent = value / 100
            currentInput = if (percent == percent.toLong().toDouble())
                percent.toLong().toString() else percent.toString()
            tvDisplay.text = currentInput
        }
    }

    private fun checkSecretCode() {
        if (inputSequence.takeLast(SECRET.length) == SECRET) {
            inputSequence = ""
            startActivity(Intent(this, MainActivity::class.java))
        }
    }
}
