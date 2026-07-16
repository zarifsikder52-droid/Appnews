package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MyApplicationTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        CalculatorScreen(viewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun CalculatorScreen(viewModel: MainViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Output / display area
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(24.dp),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.End
        ) {
            // Input expression string
            Text(
                text = viewModel.displayState.ifEmpty { "0" },
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Light,
                    fontSize = 44.sp
                ),
                color = Color(0xFF212121),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth().testTag("calculator_display")
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Evaluated result display
            if (viewModel.resultState.isNotEmpty()) {
                Text(
                    text = viewModel.resultState,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFF757575)
                    ),
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth().testTag("calculator_result")
                )
            }
        }

        // Backspace icon
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(
                onClick = { viewModel.onBackspaceClicked() },
                modifier = Modifier.testTag("backspace_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Backspace,
                    contentDescription = "Backspace",
                    tint = Color(0xFF7CB342),
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Horizontal divider
        HorizontalDivider(
            color = Color(0xFFEEEEEE),
            thickness = 1.dp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Grid of Keypad Keys matching the reference image's custom layout & colors
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(bottom = 16.dp, start = 12.dp, end = 12.dp)
        ) {
            val keys = listOf(
                listOf("C", "()", "%", "÷"),
                listOf("7", "8", "9", "×"),
                listOf("4", "5", "6", "−"),
                listOf("1", "2", "3", "+"),
                listOf("+/-", "0", ".", "=")
            )

            keys.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    row.forEach { key ->
                        val isNum = key.all { it.isDigit() } || key == "." || key == "+/-"
                        val isOperator = key == "÷" || key == "×" || key == "−" || key == "+" || key == "()" || key == "%"
                        val isC = key == "C"
                        val isEqual = key == "="

                        val bg = if (isEqual) Color(0xFF388E3C) else Color(0xFFF1F3F4)
                        val textCol = when {
                            isEqual -> Color.White
                            isC -> Color(0xFFD32F2F)
                            isOperator -> Color(0xFF388E3C)
                            else -> Color(0xFF3C4043)
                        }

                        CalculatorButton(
                            text = key,
                            onClick = {
                                when (key) {
                                    "C" -> viewModel.onClearClicked()
                                    "()" -> viewModel.onParenthesesClicked()
                                    "%" -> viewModel.onPercentClicked()
                                    "÷" -> viewModel.onOperatorClicked("÷")
                                    "×" -> viewModel.onOperatorClicked("×")
                                    "−" -> viewModel.onOperatorClicked("−")
                                    "+" -> viewModel.onOperatorClicked("+")
                                    "+/-" -> viewModel.onPlusMinusClicked()
                                    "." -> viewModel.onDigitClicked(".")
                                    "=" -> viewModel.onEqualClicked()
                                    else -> viewModel.onDigitClicked(key)
                                }
                            },
                            backgroundColor = bg,
                            contentColor = textCol,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CalculatorButton(
    text: String,
    onClick: () -> Unit,
    backgroundColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .aspectRatio(1.2f)
            .padding(6.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .testTag("btn_$text")
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Normal,
                fontSize = 24.sp
            ),
            color = contentColor
        )
    }
}


