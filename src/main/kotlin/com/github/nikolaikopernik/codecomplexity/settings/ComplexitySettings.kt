package com.github.nikolaikopernik.codecomplexity.settings

import com.github.nikolaikopernik.codecomplexity.core.ComplexitySink
import com.github.nikolaikopernik.codecomplexity.core.PointType
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.util.IconLoader
import java.awt.Color
import javax.swing.Icon

object ComplexitySettings {
    private val thresholdMethod: Int = 8
    private val thresholdClass: Int = 4 * thresholdMethod
    private val colorSimpleComplexity: Color = "#4bb14f".parseColor()
    private val colorMiddleComplexity: Color = "#FF8400".parseColor()
    private val colorHighComplexity: Color = "#FC2947".parseColor()

    private val icons = mutableMapOf<Color, Icon>()

    fun getText(complexity: ComplexitySink): String {
        val threshold = if (complexity.getPoints().any { it.type == PointType.METHOD })
            thresholdClass else thresholdMethod
        val points = complexity.getComplexity()
        val value = points * 100 / threshold
        return when {
            value < 100 -> "simple ($value% -> $points points)"
            value < 150 -> "mildly complex ($value% -> $points points)"
            else -> "very complex ($value% -> $points points)"
        }
    }

    fun getColor(complexity: ComplexitySink): Color {
        val threshold = if (complexity.getPoints().any { it.type == PointType.METHOD })
            thresholdClass else thresholdMethod
        val value = complexity.getComplexity() * 100 / threshold
        return when {
            value < 100 -> DefaultLanguageHighlighterColors.INLINE_PARAMETER_HINT.defaultAttributes.foregroundColor
            value < 150 -> colorMiddleComplexity
            else -> colorHighComplexity
        }
    }

    fun getIcon(complexity: ComplexitySink): Icon {
        val threshold = if (complexity.getPoints().any { it.type == PointType.METHOD })
            thresholdClass else thresholdMethod
        val value = complexity.getComplexity() * 100 / threshold
        return when {
            value < 100 -> IconLoader.getIcon("simple.svg", ComplexitySettings::class.java.classLoader)
            value < 150 -> IconLoader.getIcon("medium.svg", ComplexitySettings::class.java.classLoader)
            else -> IconLoader.getIcon("hard.svg", ComplexitySettings::class.java.classLoader)
        }
    }
}

private fun String.parseColor() = Color(this.drop(1).toInt(16), false)


fun Color.getContrastColor(): Color {
    val y = (299 * this.red + 587 * this.green + 114 * this.blue) / 1000.0
    return if (y >= 128) Color.black else Color.white
}

fun Color.toHex(): String = String.format("#%02x%02x%02x", this.red, this.green, this.blue);
