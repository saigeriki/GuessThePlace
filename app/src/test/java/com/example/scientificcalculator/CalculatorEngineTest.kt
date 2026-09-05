package com.example.scientificcalculator

import com.example.scientificcalculator.calc.AngleMode
import com.example.scientificcalculator.calc.CalculatorEngine
import com.example.scientificcalculator.calc.CalculatorException
import com.example.scientificcalculator.calc.NumberFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI

class CalculatorEngineTest {

    private val delta = 1e-9

    @Test
    fun `basic arithmetic follows precedence`() {
        assertEquals(14.0, CalculatorEngine.evaluate("2+3*4"), delta)
        assertEquals(5.0, CalculatorEngine.evaluate("10/2"), delta)
        assertEquals(1.0, CalculatorEngine.evaluate("2-3+2"), delta)
    }

    @Test
    fun `parentheses change precedence`() {
        assertEquals(20.0, CalculatorEngine.evaluate("(2+3)*4"), delta)
        assertEquals(5.0, CalculatorEngine.evaluate("(2+3)"), delta)
    }

    @Test
    fun `implicit multiplication works`() {
        assertEquals(14.0, CalculatorEngine.evaluate("2(3+4)"), delta)
        assertEquals(2.0 * PI, CalculatorEngine.evaluate("2π"), delta)
        assertEquals(1.0, CalculatorEngine.evaluate("1sin(90)"), delta)
    }

    @Test
    fun `power is right associative`() {
        assertEquals(512.0, CalculatorEngine.evaluate("2^3^2"), delta)
        assertEquals(4.0, CalculatorEngine.evaluate("2^2"), delta)
    }

    @Test
    fun `percent is postfix`() {
        assertEquals(0.5, CalculatorEngine.evaluate("50%"), delta)
        assertEquals(100.5, CalculatorEngine.evaluate("100+50%"), delta)
    }

    @Test
    fun `factorial and gamma`() {
        assertEquals(120.0, CalculatorEngine.evaluate("5!"), delta)
        assertEquals(1.0, CalculatorEngine.evaluate("0!"), delta)
        // Γ(2.5) = 1.5 * 0.5 * sqrt(π)
        assertEquals(1.5 * 0.5 * kotlin.math.sqrt(PI), CalculatorEngine.evaluate("1.5!"), 1e-6)
    }

    @Test
    fun `trigonometry in degrees`() {
        assertEquals(1.0, CalculatorEngine.evaluate("sin(90)", AngleMode.DEGREES), delta)
        assertEquals(0.0, CalculatorEngine.evaluate("cos(90)", AngleMode.DEGREES), 1e-9)
        assertEquals(0.5, CalculatorEngine.evaluate("sin(30)", AngleMode.DEGREES), delta)
    }

    @Test
    fun `trigonometry in radians`() {
        assertEquals(0.0, CalculatorEngine.evaluate("sin(0)", AngleMode.RADIANS), delta)
        assertEquals(1.0, CalculatorEngine.evaluate("sin(pi/2)", AngleMode.RADIANS), delta)
    }

    @Test
    fun `inverse trig returns in selected angle mode`() {
        assertEquals(30.0, CalculatorEngine.evaluate("asin(0.5)", AngleMode.DEGREES), delta)
        assertEquals(PI / 6.0, CalculatorEngine.evaluate("asin(0.5)", AngleMode.RADIANS), delta)
    }

    @Test
    fun `log and roots`() {
        assertEquals(100.0, CalculatorEngine.evaluate("ln(e^4)*25"), delta)
        assertEquals(3.0, CalculatorEngine.evaluate("log(1000)"), delta)
        assertEquals(4.0, CalculatorEngine.evaluate("sqrt(16)"), delta)
        assertEquals(3.0, CalculatorEngine.evaluate("cbrt(27)"), delta)
        assertEquals(3.0, CalculatorEngine.evaluate("nrt(2,9)"), delta)
    }

    @Test
    fun `modulo and reciprocal`() {
        assertEquals(1.0, CalculatorEngine.evaluate("mod(10,3)"), delta)
        assertEquals(0.25, CalculatorEngine.evaluate("inv(4)"), delta)
    }

    @Test
    fun `power function and log base 2`() {
        assertEquals(1024.0, CalculatorEngine.evaluate("pow(2,10)"), delta)
        assertEquals(3.0, CalculatorEngine.evaluate("log2(8)"), delta)
    }

    @Test
    fun `divide by zero throws`() {
        assertThrows { CalculatorEngine.evaluate("5/0") }
    }

    @Test
    fun `domain errors throw`() {
        assertThrows { CalculatorEngine.evaluate("sqrt(-1)") }
        assertThrows { CalculatorEngine.evaluate("ln(-1)") }
        assertThrows { CalculatorEngine.evaluate("asin(2)") }
    }

    @Test
    fun `formatting avoids unnecessary decimals`() {
        assertEquals("0", NumberFormatter.format(0.0))
        assertEquals("2", NumberFormatter.format(2.0))
        assertEquals("2.5", NumberFormatter.format(2.5))
        assertTrue(NumberFormatter.format(1e13).contains("E"))
    }

    private fun assertThrows(block: () -> Unit) {
        try {
            block()
            throw AssertionError("Expected CalculatorException")
        } catch (expected: CalculatorException) {
            // expected
        }
    }
}
