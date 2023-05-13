package com.github.nikolaikopernik.codecomplexity.settings

import com.github.nikolaikopernik.codecomplexity.core.ComplexitySink
import com.github.nikolaikopernik.codecomplexity.core.PointType
import com.intellij.ui.JBColor
import java.awt.Color

object ComplexitySettings {
    private val thresholdMethod: Int = 8
    private val thresholdClass: Int = 4 * thresholdMethod
    private val colorSimpleComplexity: Color = "#4bb14f".parseColor()
    private val colorMiddleComplexity: Color = "#ffcc00".parseColor()
    private val colorHighComplexity: Color = "#ff0000".parseColor()

    fun getText(complexity: ComplexitySink): String {
        val threshold = if (complexity.getPoints().any { it.type == PointType.METHOD })
            thresholdClass else thresholdMethod
        val value = complexity.getComplexity() * 100 / threshold
        return when {
            value < 100 -> "simple ($value%)"
            value < 150 -> "mildly complex ($value%)"
            else -> "very complex ($value%)"
        }
    }

    fun getIcon(complexity: ComplexitySink): String {
        val threshold = if (complexity.getPoints().any { it.type == PointType.METHOD })
            thresholdClass else thresholdMethod
        val value = complexity.getComplexity() * 100 / threshold
        return when {
            value < 100 -> "simple.svg"
            value < 150 -> "medium.svg"
            else -> "hard.svg"
        }
    }
}

private fun String.parseColor() = Color(this.drop(1).toInt(16), false)


fun Color.getContrastColor(): Color {
    val y = (299 * this.red + 587 * this.green + 114 * this.blue) / 1000.0
    return if (y >= 128) Color.black else Color.white
}
