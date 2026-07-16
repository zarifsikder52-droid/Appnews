package com.example

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    var displayState by mutableStateOf("")
    var resultState by mutableStateOf("")

    val allHistory: StateFlow<List<com.example.data.Calculation>>

    init {
        val database = AppDatabase.getDatabase(application)
        
        allHistory = database.calculationDao().getAllCalculations().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }
    
    private fun saveCalculation(expr: String, res: String) {
        viewModelScope.launch {
            AppDatabase.getDatabase(getApplication()).calculationDao().insertCalculation(
                com.example.data.Calculation(expression = expr, result = res)
            )
        }
    }
    
    fun clearHistory() {
        viewModelScope.launch {
            AppDatabase.getDatabase(getApplication()).calculationDao().clearHistory()
        }
    }

    fun onDigitClicked(digit: String) {
        if (displayState.length < 20) {
            displayState += digit
        }
    }

    fun onOperatorClicked(operator: String) {
        if (displayState.isNotEmpty()) {
            val lastChar = displayState.last()
            if (lastChar == '+' || lastChar == '-' || lastChar == '×' || lastChar == '÷' || lastChar == '.') {
                displayState = displayState.dropLast(1) + operator
            } else {
                displayState += operator
            }
        } else if (operator == "-") {
            displayState += operator
        }
    }

    fun onClearClicked() {
        displayState = ""
        resultState = ""
    }

    fun onBackspaceClicked() {
        if (displayState.isNotEmpty()) {
            displayState = displayState.dropLast(1)
        }
    }

    fun onParenthesesClicked() {
        val openCount = displayState.count { it == '(' }
        val closeCount = displayState.count { it == ')' }
        if (displayState.isNotEmpty() && displayState.last().isDigit() && openCount > closeCount) {
            displayState += ")"
        } else {
            displayState += "("
        }
    }

    fun onPercentClicked() {
        if (displayState.isNotEmpty() && (displayState.last().isDigit() || displayState.last() == ')')) {
            displayState += "%"
        }
    }

    fun onPlusMinusClicked() {
        if (displayState.isEmpty()) {
            displayState = "-"
        } else if (displayState.startsWith("-")) {
            displayState = displayState.substring(1)
        } else {
            displayState = "-$displayState"
        }
    }

    fun onEqualClicked() {
        val currentInput = displayState.trim()
        if (currentInput.isNotEmpty()) {
            try {
                val parsedExpr = currentInput.replace("×", "*").replace("÷", "/")
                val result = MathParser(parsedExpr).parse()
                resultState = if (result.isInfinite() || result.isNaN()) {
                    "Error"
                } else if (result % 1.0 == 0.0) {
                    result.toLong().toString()
                } else {
                    String.format("%.6f", result).trimEnd('0').trimEnd('.')
                }
                saveCalculation(currentInput, resultState)
            } catch (e: Exception) {
                resultState = "Error"
            }
        }
    }
}

// Inline Math Expression Parser for robust calculations
class MathParser(private val str: String) {
    private var pos = -1
    private var ch = 0

    private fun nextChar() {
        ch = if (++pos < str.length) str[pos].code else -1
    }

    private fun eat(charToEat: Int): Boolean {
        while (ch == ' '.code) nextChar()
        if (ch == charToEat) {
            nextChar()
            return true
        }
        return false
    }

    fun parse(): Double {
        nextChar()
        val x = parseExpression()
        if (pos < str.length) throw RuntimeException("Unexpected: " + ch.toChar())
        return x
    }

    private fun parseExpression(): Double {
        var x = parseTerm()
        while (true) {
            if (eat('+'.code)) x += parseTerm()
            else if (eat('-'.code)) x -= parseTerm()
            else break
        }
        return x
    }

    private fun parseTerm(): Double {
        var x = parseFactor()
        while (true) {
            if (eat('*'.code)) x *= parseFactor()
            else if (eat('/'.code)) {
                val divisor = parseFactor()
                if (divisor == 0.0) throw RuntimeException("Division by zero")
                x /= divisor
            } else break
        }
        return x
    }

    private fun parseFactor(): Double {
        if (eat('+'.code)) return parseFactor()
        if (eat('-'.code)) return -parseFactor()

        var x: Double
        val startPos = this.pos
        if (eat('('.code)) {
            x = parseExpression()
            eat(')'.code)
        } else if ((ch >= '0'.code && ch <= '9'.code) || ch == '.'.code) {
            while ((ch >= '0'.code && ch <= '9'.code) || ch == '.'.code) nextChar()
            x = str.substring(startPos, this.pos).toDouble()
        } else if (ch >= 'a'.code && ch <= 'z'.code) {
            val sb = StringBuilder()
            while (ch >= 'a'.code && ch <= 'z'.code) {
                sb.append(ch.toChar())
                nextChar()
            }
            val func = sb.toString()
            x = parseFactor()
            x = when (func) {
                "sin" -> Math.sin(Math.toRadians(x))
                "cos" -> Math.cos(Math.toRadians(x))
                "tan" -> Math.tan(Math.toRadians(x))
                "sqrt" -> Math.sqrt(x)
                "log" -> Math.log10(x)
                else -> throw RuntimeException("Unknown function: $func")
            }
        } else {
            throw RuntimeException("Unexpected: " + ch.toChar())
        }

        if (eat('%'.code)) {
            x /= 100.0
        }

        return x
    }
}
