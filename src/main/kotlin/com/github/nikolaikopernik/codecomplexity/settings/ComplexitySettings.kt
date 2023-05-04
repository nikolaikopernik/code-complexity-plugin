package com.github.nikolaikopernik.codecomplexity.settings

import com.intellij.ui.JBColor
import java.awt.Color

object ComplexitySettings {
    private val threshold: Int = 8
    private val colorSimpleComplexity: Color = "#4bb14f".parseColor()
    private val colorMiddleComplexity: Color = "#ffcc00".parseColor()
    private val colorHighComplexity: Color = "#ff0000".parseColor()

    fun getText(complexity: Int): String {
        val value = complexity * 100 / threshold
        return when {
            value < 100 -> "simple ($value%)"
            value < 150 -> "mildly complex ($value%)"
            else -> "very complex ($value%)"
        }
    }

    fun getColor(complexity: Int): Color {
        val value = complexity * 100 / threshold
        return when {
            value < 100 -> JBColor(colorSimpleComplexity, colorSimpleComplexity)
            value < 150 -> JBColor(colorMiddleComplexity, colorMiddleComplexity)
            else -> JBColor(colorHighComplexity, colorHighComplexity)
        }
    }
}

private fun String.parseColor() = Color(this.drop(1).toInt(16), false)


fun Color.getContrastColor(): Color {
    val y = (299 * this.red + 587 * this.green + 114 * this.blue) / 1000.0
    return if (y >= 128) Color.black else Color.white
}
