package com.example.scientificcalculator.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.scientificcalculator.calc.AngleMode
import com.example.scientificcalculator.calc.CalculatorEngine
import com.example.scientificcalculator.calc.CalculatorException
import com.example.scientificcalculator.calc.Functions
import com.example.scientificcalculator.calc.NumberFormatter
import com.example.scientificcalculator.ui.theme.AccentGreen
import com.example.scientificcalculator.ui.theme.AccentOrange
import com.example.scientificcalculator.ui.theme.AccentRed
import com.example.scientificcalculator.ui.theme.DarkBackground
import com.example.scientificcalculator.ui.theme.DarkKeyAction
import com.example.scientificcalculator.ui.theme.DarkKeyFunction
import com.example.scientificcalculator.ui.theme.DarkKeyMemory
import com.example.scientificcalculator.ui.theme.DarkKeyNumber
import com.example.scientificcalculator.ui.theme.DarkKeyOperator
import com.example.scientificcalculator.ui.theme.DarkSurface
import com.example.scientificcalculator.ui.theme.DarkText
import com.example.scientificcalculator.ui.theme.DarkTextDim
import com.example.scientificcalculator.ui.theme.LightBackground
import com.example.scientificcalculator.ui.theme.LightKeyAction
import com.example.scientificcalculator.ui.theme.LightKeyFunction
import com.example.scientificcalculator.ui.theme.LightKeyMemory
import com.example.scientificcalculator.ui.theme.LightKeyNumber
import com.example.scientificcalculator.ui.theme.LightKeyOperator
import com.example.scientificcalculator.ui.theme.LightSurface
import com.example.scientificcalculator.ui.theme.LightText
import com.example.scientificcalculator.ui.theme.LightTextDim

// --------------------------------------------------------------------------- key model
private enum class KeyStyle { NUMBER, OPERATOR, FUNCTION, ACTION, MEMORY, EQUALS }

private sealed interface CalcAction {
    data class Insert(val value: String) : CalcAction
    data object Equals : CalcAction
    data object Clear : CalcAction
    data object Backspace : CalcAction
    data object Negate : CalcAction
    data object MemoryClear : CalcAction
    data object MemoryRecall : CalcAction
    data object MemoryAdd : CalcAction
    data object MemorySubtract : CalcAction
    data object MemoryStore : CalcAction
}

private data class CalcKey(
    val label: String,
    val action: CalcAction,
    val style: KeyStyle = KeyStyle.NUMBER,
    val span: Int = 1
)

// ------------------------------------------------------------------ key definitions
private val memoryRow = listOf(
    CalcKey("MC", CalcAction.MemoryClear, KeyStyle.MEMORY),
    CalcKey("MR", CalcAction.MemoryRecall, KeyStyle.MEMORY),
    CalcKey("M+", CalcAction.MemoryAdd, KeyStyle.MEMORY),
    CalcKey("M-", CalcAction.MemorySubtract, KeyStyle.MEMORY),
    CalcKey("MS", CalcAction.MemoryStore, KeyStyle.MEMORY)
)

private val scientificRows = listOf(
    listOf(
        CalcKey("sin", CalcAction.Insert("sin("), KeyStyle.FUNCTION),
        CalcKey("cos", CalcAction.Insert("cos("), KeyStyle.FUNCTION),
        CalcKey("tan", CalcAction.Insert("tan("), KeyStyle.FUNCTION),
        CalcKey("(", CalcAction.Insert("("), KeyStyle.FUNCTION),
        CalcKey(")", CalcAction.Insert(")"), KeyStyle.FUNCTION)
    ),
    listOf(
        CalcKey("asin", CalcAction.Insert("asin("), KeyStyle.FUNCTION),
        CalcKey("acos", CalcAction.Insert("acos("), KeyStyle.FUNCTION),
        CalcKey("atan", CalcAction.Insert("atan("), KeyStyle.FUNCTION),
        CalcKey("ln", CalcAction.Insert("ln("), KeyStyle.FUNCTION),
        CalcKey("log", CalcAction.Insert("log("), KeyStyle.FUNCTION)
    ),
    listOf(
        CalcKey("x²", CalcAction.Insert("^2"), KeyStyle.FUNCTION),
        CalcKey("xʸ", CalcAction.Insert("^"), KeyStyle.OPERATOR),
        CalcKey("√", CalcAction.Insert("sqrt("), KeyStyle.FUNCTION),
        CalcKey("π", CalcAction.Insert("π"), KeyStyle.FUNCTION),
        CalcKey("e", CalcAction.Insert("e"), KeyStyle.FUNCTION)
    ),
    listOf(
        CalcKey("x!", CalcAction.Insert("!"), KeyStyle.FUNCTION),
        CalcKey("1/x", CalcAction.Insert("inv("), KeyStyle.FUNCTION),
        CalcKey("10ˣ", CalcAction.Insert("10^"), KeyStyle.OPERATOR),
        CalcKey("mod", CalcAction.Insert("mod("), KeyStyle.FUNCTION),
        CalcKey("|x|", CalcAction.Insert("abs("), KeyStyle.FUNCTION)
    ),
    listOf(
        CalcKey("sinh", CalcAction.Insert("sinh("), KeyStyle.FUNCTION),
        CalcKey("cosh", CalcAction.Insert("cosh("), KeyStyle.FUNCTION),
        CalcKey("tanh", CalcAction.Insert("tanh("), KeyStyle.FUNCTION),
        CalcKey("∛", CalcAction.Insert("cbrt("), KeyStyle.FUNCTION),
        CalcKey("ⁿ√", CalcAction.Insert("nrt("), KeyStyle.FUNCTION)
    ),
    listOf(
        CalcKey(",", CalcAction.Insert(","), KeyStyle.FUNCTION),
        CalcKey("eˣ", CalcAction.Insert("e^"), KeyStyle.OPERATOR),
        CalcKey("log₂", CalcAction.Insert("log2("), KeyStyle.FUNCTION),
        CalcKey("x³", CalcAction.Insert("^3"), KeyStyle.OPERATOR),
        CalcKey("pow", CalcAction.Insert("pow("), KeyStyle.FUNCTION)
    )
)

private val basicRows = listOf(
    listOf(
        CalcKey("AC", CalcAction.Clear, KeyStyle.ACTION),
        CalcKey("±", CalcAction.Negate, KeyStyle.ACTION),
        CalcKey("⌫", CalcAction.Backspace, KeyStyle.ACTION),
        CalcKey("÷", CalcAction.Insert("/"), KeyStyle.OPERATOR)
    ),
    listOf(
        CalcKey("7", CalcAction.Insert("7")),
        CalcKey("8", CalcAction.Insert("8")),
        CalcKey("9", CalcAction.Insert("9")),
        CalcKey("×", CalcAction.Insert("*"), KeyStyle.OPERATOR)
    ),
    listOf(
        CalcKey("4", CalcAction.Insert("4")),
        CalcKey("5", CalcAction.Insert("5")),
        CalcKey("6", CalcAction.Insert("6")),
        CalcKey("−", CalcAction.Insert("-"), KeyStyle.OPERATOR)
    ),
    listOf(
        CalcKey("1", CalcAction.Insert("1")),
        CalcKey("2", CalcAction.Insert("2")),
        CalcKey("3", CalcAction.Insert("3")),
        CalcKey("+", CalcAction.Insert("+"), KeyStyle.OPERATOR)
    ),
    listOf(
        CalcKey("0", CalcAction.Insert("0")),
        CalcKey(".", CalcAction.Insert(".")),
        CalcKey("%", CalcAction.Insert("%"), KeyStyle.ACTION),
        CalcKey("=", CalcAction.Equals, KeyStyle.EQUALS)
    )
)

// ------------------------------------------------------------------------ main app
@Composable
fun ScientificCalculatorApp(
    darkTheme: Boolean,
    onDarkThemeChange: (Boolean) -> Unit
) {
    var expression by rememberSaveable { mutableStateOf("") }
    var angleMode by rememberSaveable { mutableStateOf(AngleMode.DEGREES) }
    var memory by rememberSaveable { mutableStateOf(0.0) }
    var hasMemory by rememberSaveable { mutableStateOf(false) }
    var history by remember { mutableStateOf<List<String>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var lastResult by remember { mutableStateOf(0.0) }

    val liveResult = remember(expression, angleMode) {
        if (expression.isBlank()) "0" else try {
            val v = CalculatorEngine.evaluate(expression, angleMode)
            NumberFormatter.format(v)
        } catch (e: CalculatorException) {
            // While the user is typing, allow "4+" to preview 4 and "4*" to preview 0.
            try {
                val candidate = if (expression.endsWithOperator()) expression + "0" else expression
                NumberFormatter.format(CalculatorEngine.evaluate(candidate, angleMode))
            } catch (ignored: CalculatorException) {
                ""
            }
        }
    }

    fun currentNumericValue(): Double = try {
        CalculatorEngine.evaluate(expression.ifBlank { "0" }, angleMode)
    } catch (ignored: CalculatorException) {
        lastResult
    }

    fun applyAction(action: CalcAction) {
        when (action) {
            is CalcAction.Insert -> {
                if (error != null) {
                    expression = ""
                    error = null
                }
                expression += action.value
            }

            CalcAction.Equals -> {
                var expr = expression
                if (expr.isBlank()) return
                if (expr.endsWithOperator()) expr += "0"
                try {
                    val value = CalculatorEngine.evaluate(expr, angleMode)
                    val result = NumberFormatter.forExpression(value)
                    history = (history + "${displayExpression(expr)} = $result").takeLast(6)
                    expression = result
                    lastResult = value
                    error = null
                } catch (e: CalculatorException) {
                    error = e.message ?: "Invalid expression"
                }
            }

            CalcAction.Clear -> {
                expression = ""
                error = null
            }

            CalcAction.Backspace -> {
                val function = Functions.buttonInsertions.firstOrNull { expression.endsWith(it) }
                expression = if (function != null) {
                    expression.dropLast(function.length)
                } else {
                    expression.dropLast(1)
                }
                error = null
            }

            CalcAction.Negate -> {
                val regex = Regex("""\d+(\.\d+)?$""")
                val match = regex.find(expression)
                expression = if (match != null) {
                    expression.removeRange(match.range) + "(-${match.value})"
                } else {
                    expression + "-"
                }
                error = null
            }

            CalcAction.MemoryClear -> {
                memory = 0.0
                hasMemory = false
            }

            CalcAction.MemoryRecall -> {
                expression += NumberFormatter.forExpression(memory)
                error = null
            }

            CalcAction.MemoryAdd -> {
                memory += currentNumericValue()
                hasMemory = true
            }

            CalcAction.MemorySubtract -> {
                memory -= currentNumericValue()
                hasMemory = true
            }

            CalcAction.MemoryStore -> {
                memory = currentNumericValue()
                hasMemory = true
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (darkTheme) DarkBackground else LightBackground)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        // top bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "SciCalc",
                        color = if (darkTheme) DarkText else LightText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )
                    if (hasMemory) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "M",
                            color = AccentGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
                Text(
                    text = "Scientific Calculator",
                    color = if (darkTheme) DarkTextDim else LightTextDim,
                    fontSize = 12.sp
                )
            }
            Text(
                text = if (darkTheme) "☀️" else "🌙",
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onDarkThemeChange(!darkTheme) }
                    .padding(10.dp),
                fontSize = 18.sp,
                textAlign = TextAlign.Center
            )
        }

        // angle mode selector
        AngleModeSelector(
            angleMode = angleMode,
            onModeChange = { angleMode = it },
            darkTheme = darkTheme,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        // display panel
        DisplayPanel(
            expression = expression,
            preview = liveResult,
            error = error,
            history = history,
            darkTheme = darkTheme,
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.34f)
        )

        Spacer(Modifier.height(10.dp))

        // keypad
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.66f)
                .padding(horizontal = 2.dp)
        ) {
            KeypadRow(
                keys = memoryRow,
                darkTheme = darkTheme,
                onKey = { applyAction(it) },
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.height(4.dp))
            scientificRows.forEach { row ->
                KeypadRow(
                    keys = row,
                    darkTheme = darkTheme,
                    onKey = { applyAction(it) },
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.height(4.dp))
            }
            Spacer(Modifier.height(2.dp))
            basicRows.forEach { row ->
                KeypadRow(
                    keys = row,
                    darkTheme = darkTheme,
                    onKey = { applyAction(it) },
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

// ----------------------------------------------------------------------- widgets
@Composable
private fun AngleModeSelector(
    angleMode: AngleMode,
    onModeChange: (AngleMode) -> Unit,
    darkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        AngleMode.entries.forEach { mode ->
            val selected = mode == angleMode
            val bg = if (selected) AccentOrange else if (darkTheme) DarkSurface else LightSurface
            val fg = if (selected) Color.White else if (darkTheme) DarkTextDim else LightTextDim
            Box(
                modifier = Modifier
                    .padding(horizontal = 3.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(bg)
                    .clickable { onModeChange(mode) }
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = mode.label, color = fg, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun DisplayPanel(
    expression: String,
    preview: String,
    error: String?,
    history: List<String>,
    darkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    val surface = if (darkTheme) DarkSurface else LightSurface
    val text = if (darkTheme) DarkText else LightText
    val dim = if (darkTheme) DarkTextDim else LightTextDim

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(surface)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = "HISTORY",
                color = dim,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.4.sp
            )
            Spacer(Modifier.height(2.dp))
            history.takeLast(4).forEach { line ->
                Text(
                    text = line,
                    color = dim.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontFamily = FontFamily.Monospace
                )
            }
            Spacer(Modifier.weight(1f))
            SelectionContainer {
                Text(
                    text = displayExpression(expression).ifBlank { "0" },
                    color = dim,
                    fontSize = 20.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End,
                    fontFamily = FontFamily.Monospace
                )
            }
            Spacer(Modifier.height(6.dp))
            SelectionContainer {
                Text(
                    text = error ?: preview.ifBlank { "…" },
                    color = if (error != null) AccentRed else text,
                    fontSize = if (error != null) 22.sp else 44.sp,
                    fontWeight = FontWeight.Light,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
private fun KeypadRow(
    keys: List<CalcKey>,
    darkTheme: Boolean,
    onKey: (CalcAction) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.fillMaxWidth().fillMaxHeight()) {
        keys.forEach { key ->
            val cellModifier = Modifier
                .weight(key.span.toFloat())
                .fillMaxHeight()
                .padding(horizontal = 3.dp, vertical = 2.dp)
            CalcButton(
                key = key,
                darkTheme = darkTheme,
                modifier = cellModifier,
                onClick = { onKey(key.action) }
            )
        }
    }
}

@Composable
private fun CalcButton(
    key: CalcKey,
    darkTheme: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val background = when (key.style) {
        KeyStyle.NUMBER -> if (darkTheme) DarkKeyNumber else LightKeyNumber
        KeyStyle.FUNCTION -> if (darkTheme) DarkKeyFunction else LightKeyFunction
        KeyStyle.OPERATOR -> if (darkTheme) DarkKeyOperator else LightKeyOperator
        KeyStyle.ACTION -> if (darkTheme) DarkKeyAction else LightKeyAction
        KeyStyle.MEMORY -> if (darkTheme) DarkKeyMemory else LightKeyMemory
        KeyStyle.EQUALS -> AccentOrange
    }
    val foreground = when (key.style) {
        KeyStyle.NUMBER -> if (darkTheme) DarkText else LightText
        KeyStyle.FUNCTION -> if (darkTheme) AccentOrangeDark else AccentOrange
        KeyStyle.OPERATOR -> if (darkTheme) AccentOrangeDark else Color(0xFFB34E00)
        KeyStyle.ACTION -> if (darkTheme) AccentRed else Color(0xFFB3224B)
        KeyStyle.MEMORY -> if (darkTheme) AccentGreen else Color(0xFF11845B)
        KeyStyle.EQUALS -> Color.White
    }
    val fontSize: TextUnit = when {
        key.style == KeyStyle.MEMORY || key.label.length >= 3 -> 13.sp
        key.style == KeyStyle.EQUALS -> 26.sp
        else -> 19.sp
    }

    val buttonModifier = if (key.style == KeyStyle.EQUALS) {
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.linearGradient(listOf(AccentOrange, AccentRed)))
    } else {
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(background)
    }

    Box(
        modifier = buttonModifier
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = key.label,
            color = foreground,
            fontWeight = if (key.style == KeyStyle.EQUALS) FontWeight.Black else FontWeight.Medium,
            fontSize = fontSize,
            maxLines = 1,
            textAlign = TextAlign.Center,
            fontFamily = FontFamily.SansSerif
        )
    }
}

// ------------------------------------------------------------------- helpers
private fun String.endsWithOperator(): Boolean {
    if (isEmpty()) return false
    val last = this.last()
    return last == '+' || last == '-' || last == '*' || last == '/' || last == '^'
}

private fun displayExpression(raw: String): String {
    return raw
        .replace("/", "÷")
        .replace("-", "−")
        .replace("*", "×")
        .replace("sqrt(", "\u221A(")
        .replace("cbrt(", "\u221B(")
        .replace("nrt(", "\u207F\u221A(")
        .replace("log2(", "\u2082(")
        .replace("abs(", "|")
        .replace("inv(", "1/")
}
