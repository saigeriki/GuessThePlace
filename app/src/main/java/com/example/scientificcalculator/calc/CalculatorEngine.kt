package com.example.scientificcalculator.calc

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cbrt
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/** Angle unit used by trigonometric functions. */
enum class AngleMode(val label: String) {
    DEGREES("DEG"),
    RADIANS("RAD"),
    GRADIANS("GRAD")
}

/** Thrown for malformed input or domain errors. */
class CalculatorException(message: String) : Exception(message)

/** Supported function names and their arity. */
object Functions {
    private val ARITY: Map<String, Int> = mapOf(
        "sin" to 1, "cos" to 1, "tan" to 1,
        "asin" to 1, "acos" to 1, "atan" to 1,
        "sinh" to 1, "cosh" to 1, "tanh" to 1,
        "ln" to 1, "log" to 1, "log2" to 1,
        "sqrt" to 1, "cbrt" to 1, "abs" to 1,
        "exp" to 1, "inv" to 1,
        "mod" to 2, "nrt" to 2, "pow" to 2
    )

    fun isFunction(name: String): Boolean = name in ARITY
    fun arity(name: String): Int = ARITY[name] ?: throw CalculatorException("Unknown function: $name")

    /** The strings used by the UI buttons, each ends with '(' so the tokenizer can parse them. */
    val buttonInsertions: List<String> = ARITY.keys.map { "$it(" }
}

private sealed interface Token {
    data class Number(val value: Double) : Token
    data class Operator(val symbol: Char) : Token
    data class Function(val name: String) : Token
    data class Constant(val name: String) : Token
    data object LParen : Token
    data object RParen : Token
    data object Comma : Token
    data class Postfix(val symbol: Char) : Token
}

/**
 * A small, dependency-free expression engine.
 *
 * Supported:
 *   + - * / ^  ( ^ is right associative )
 *   implicit multiplication  e.g. 2(3+4), 2π, 2sin(30)
 *   postfix ! (factorial) and % (percent)
 *   constants π and e
 *   unary functions sin cos tan asin acos atan sinh cosh tanh
 *   ln log log2 sqrt cbrt abs exp inv
 *   binary functions mod(a,b), nrt(n,x), pow(a,b)
 */
object CalculatorEngine {

    /** Evaluates a mathematical expression and returns the numeric result. */
    fun evaluate(expression: String, angleMode: AngleMode = AngleMode.DEGREES): Double {
        if (expression.isBlank()) throw CalculatorException("Empty expression")
        val tokens = tokenize(expression)
        val parser = Parser(tokens, angleMode)
        return parser.parse()
    }

    // ---------------------------------------------------------------- tokenizer
    private fun tokenize(expression: String): List<Token> {
        val tokens = mutableListOf<Token>()
        var i = 0
        while (i < expression.length) {
            val c = expression[i]
            when {
                c.isWhitespace() -> i++
                c.isDigit() || c == '.' -> {
                    val start = i
                    while (i < expression.length && (expression[i].isDigit() || expression[i] == '.')) i++
                    val raw = expression.substring(start, i)
                    tokens += Token.Number(raw.toDoubleOrNull()
                        ?: throw CalculatorException("Invalid number: $raw"))
                }
                c == 'π' -> {
                    i++
                    tokens += Token.Constant("pi")
                }
                c.isLetter() -> {
                    val start = i
                    while (i < expression.length && expression[i].isLetter()) i++
                    val name = expression.substring(start, i)
                    when {
                        name == "pi" -> tokens += Token.Constant("pi")
                        name == "e" -> tokens += Token.Constant("e")
                        Functions.isFunction(name) -> tokens += Token.Function(name)
                        else -> throw CalculatorException("Unknown symbol: $name")
                    }
                }
                c == '×' -> { i++; tokens += Token.Operator('*') }
                c == '÷' -> { i++; tokens += Token.Operator('/') }
                c == '−' -> { i++; tokens += Token.Operator('-') }
                c == '+' || c == '-' || c == '*' || c == '/' || c == '^' -> {
                    i++
                    tokens += Token.Operator(c)
                }
                c == '(' -> { i++; tokens += Token.LParen }
                c == ')' -> { i++; tokens += Token.RParen }
                c == ',' -> { i++; tokens += Token.Comma }
                c == '!' || c == '%' -> { i++; tokens += Token.Postfix(c) }
                else -> throw CalculatorException("Unexpected character: '$c'")
            }
        }
        return tokens
    }

    // ------------------------------------------------------------------ parser
    private class Parser(private val tokens: List<Token>, private val mode: AngleMode) {
        private var index = 0

        private fun peek() = tokens.getOrNull(index)
        private fun next() = tokens.getOrNull(index++) ?: throw CalculatorException("Unexpected end of expression")

        fun parse(): Double {
            if (tokens.isEmpty()) throw CalculatorException("Empty expression")
            val result = parseExpression()
            if (index != tokens.size) throw CalculatorException("Unexpected input at end")
            if (result.isNaN()) throw CalculatorException("Result is not a number")
            if (result.isInfinite()) throw CalculatorException("Result is too large")
            return result
        }

        private fun parseExpression(): Double {
            var value = parseTerm()
            while (true) {
                when {
                    matchOperator('+') -> value += parseTerm()
                    matchOperator('-') -> value -= parseTerm()
                    else -> return value
                }
            }
        }

        private fun parseTerm(): Double {
            var value = parseUnary()
            while (true) {
                when {
                    matchOperator('*') -> value *= parseUnary()
                    matchOperator('/') -> {
                        val right = parseUnary()
                        if (right == 0.0) throw CalculatorException("Cannot divide by zero")
                        value /= right
                    }
                    startsUnary() -> value *= parseUnary() // implicit multiplication
                    else -> return value
                }
            }
        }

        private fun parseUnary(): Double {
            if (matchOperator('-')) return -parseUnary()
            if (matchOperator('+')) return parseUnary()
            return parsePower()
        }

        private fun parsePower(): Double {
            val base = parsePostfix()
            if (matchOperator('^')) {
                val exponent = parseUnary() // right associative
                val result = base.pow(exponent)
                if (result.isNaN()) throw CalculatorException("Invalid power")
                return result
            }
            return base
        }

        private fun parsePostfix(): Double {
            var value = parsePrimary()
            while (true) {
                when {
                    matchPostfix('!') -> value = factorial(value)
                    matchPostfix('%') -> value /= 100.0
                    else -> return value
                }
            }
        }

        private fun parsePrimary(): Double {
            return when (val token = next()) {
                is Token.Number -> token.value
                is Token.Constant -> when (token.name) {
                    "pi" -> PI
                    "e" -> kotlin.math.E
                    else -> throw CalculatorException("Unknown constant: ${token.name}")
                }
                is Token.Function -> parseFunction(token.name)
                Token.LParen -> {
                    val value = parseExpression()
                    expect(Token.RParen)
                    value
                }
                else -> throw CalculatorException("Expected a number")
            }
        }

        private fun parseFunction(name: String): Double {
            expect(Token.LParen)
            val args = mutableListOf(parseExpression())
            while (match(Token.Comma)) {
                args += parseExpression()
            }
            expect(Token.RParen)

            val expected = Functions.arity(name)
            if (args.size != expected) {
                throw CalculatorException("$name expects $expected argument(s)")
            }
            return applyFunction(name, args)
        }

        private fun startsUnary(): Boolean = when (peek()) {
            is Token.Number, is Token.Constant, is Token.Function, Token.LParen -> true
            else -> false
        }

        private fun matchOperator(symbol: Char): Boolean {
            val t = peek()
            return t is Token.Operator && t.symbol == symbol && { index++; true }()
        }

        private fun matchPostfix(symbol: Char): Boolean {
            val t = peek()
            return t is Token.Postfix && t.symbol == symbol && { index++; true }()
        }

        private fun match(tokenClass: Token): Boolean {
            return when {
                peek()?.javaClass == tokenClass.javaClass -> { index++; true }
                else -> false
            }
        }

        private fun expect(token: Token) {
            val t = next()
            if (t.javaClass != token.javaClass) throw CalculatorException("Expected ${token.javaClass.simpleName}")
        }
    }

    // ---------------------------------------------------------------- functions
    private fun applyFunction(name: String, args: List<Double>): Double {
        return when (name) {
            // trigonometric
            "sin" -> sin(toRadians(args[0]))
            "cos" -> cos(toRadians(args[0]))
            "tan" -> {
                val r = toRadians(args[0])
                val c = cos(r)
                if (abs(c) < 1e-12) throw CalculatorException("Undefined tangent")
                sin(r) / c
            }
            "asin" -> {
                val v = args[0]
                if (v < -1.0 || v > 1.0) throw CalculatorException("asin domain: [-1, 1]")
                fromRadians(kotlin.math.asin(v))
            }
            "acos" -> {
                val v = args[0]
                if (v < -1.0 || v > 1.0) throw CalculatorException("acos domain: [-1, 1]")
                fromRadians(kotlin.math.acos(v))
            }
            "atan" -> fromRadians(kotlin.math.atan(args[0]))

            // hyperbolic
            "sinh" -> kotlin.math.sinh(args[0])
            "cosh" -> kotlin.math.cosh(args[0])
            "tanh" -> kotlin.math.tanh(args[0])

            // logarithmic
            "ln" -> {
                if (args[0] <= 0.0) throw CalculatorException("ln domain: x > 0")
                ln(args[0])
            }
            "log" -> {
                if (args[0] <= 0.0) throw CalculatorException("log domain: x > 0")
                log10(args[0])
            }
            "log2" -> {
                if (args[0] <= 0.0) throw CalculatorException("log₂ domain: x > 0")
                ln(args[0]) / ln(2.0)
            }

            // powers & roots
            "sqrt" -> {
                if (args[0] < 0.0) throw CalculatorException("sqrt domain: x >= 0")
                sqrt(args[0])
            }
            "cbrt" -> cbrt(args[0])
            "exp" -> exp(args[0])
            "pow" -> {
                val r = args[0].pow(args[1])
                if (r.isNaN()) throw CalculatorException("Invalid power")
                r
            }
            "nrt" -> {
                val n = args[0]
                val x = args[1]
                if (n == 0.0) throw CalculatorException("Root index cannot be zero")
                if (x < 0.0 && n % 2.0 == 0.0) throw CalculatorException("Even root of negative number")
                x.pow(1.0 / n)
            }

            // miscellaneous
            "abs" -> abs(args[0])
            "inv" -> {
                if (args[0] == 0.0) throw CalculatorException("Cannot take reciprocal of zero")
                1.0 / args[0]
            }
            "mod" -> {
                val b = args[1]
                if (b == 0.0) throw CalculatorException("Cannot divide by zero")
                args[0] % b
            }

            else -> throw CalculatorException("Unknown function: $name")
        }
    }

    private fun toRadians(value: Double) = when (mode) {
        AngleMode.DEGREES -> value * PI / 180.0
        AngleMode.RADIANS -> value
        AngleMode.GRADIANS -> value * PI / 200.0
    }

    private fun fromRadians(value: Double) = when (mode) {
        AngleMode.DEGREES -> value * 180.0 / PI
        AngleMode.RADIANS -> value
        AngleMode.GRADIANS -> value * 200.0 / PI
    }

    private fun factorial(value: Double): Double {
        if (value == floor(value)) {
            val n = value.toInt()
            if (n < 0) throw CalculatorException("Factorial of negative number")
            var result = 1.0
            for (i in 2..n) result *= i
            return result
        }
        if (value < 0) throw CalculatorException("Factorial of negative number")
        return gamma(value + 1.0)
    }

    /** Lanczos approximation for factorial of non-integers. */
    private fun gamma(value: Double): Double {
        if (value < 0.5) return PI / (sin(PI * value) * gamma(1.0 - value))
        val g = 7.0
        val coefficients = doubleArrayOf(
            0.99999999999980993, 676.5203681218851, -1259.1392167224028,
            771.32342877765313, -176.61502916214059, 12.507343278686905,
            -0.13857109526572012, 9.9843695780195716e-6, 1.5056327351493116e-7
        )
        val x = value - 1.0
        var a = coefficients[0]
        val t = x + g + 0.5
        for (i in 1 until coefficients.size) {
            a += coefficients[i] / (x + i)
        }
        return sqrt(2.0 * PI) * t.pow(x + 0.5) * exp(-t) * a
    }
}

/** Formats a double for display: plain decimal in normal range, scientific notation for extremes. */
object NumberFormatter {
    private const val MAX_DECIMAL_DIGITS = 12
    private const val SCREENSHOT_TRIGGER = 1e12
    private const val SMALL_TRIGGER = 1e-6

    fun format(value: Double): String {
        if (value.isNaN()) return "Undefined"
        if (value.isInfinite()) return if (value > 0) "∞" else "-∞"
        if (value == 0.0) return "0"

        val abs = abs(value)
        if (abs >= SCREENSHOT_TRIGGER || abs < SMALL_TRIGGER) {
            return String.format(Locale.US, "%.8E", value).trim()
        }

        val bd = BigDecimal(value)
            .round(MathContext(MAX_DECIMAL_DIGITS, RoundingMode.HALF_UP))
            .stripTrailingZeros()
        return bd.toPlainString()
    }

    /** Formats a result so it can be used as the next expression (plain decimal, no E-notation). */
    fun forExpression(value: Double): String {
        if (value.isNaN() || value.isInfinite()) return "Undefined"
        val bd = BigDecimal(value)
            .round(MathContext(MAX_DECIMAL_DIGITS, RoundingMode.HALF_UP))
            .stripTrailingZeros()
        return bd.toPlainString()
    }
}
