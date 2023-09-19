package com.github.nikolaikopernik.codecomplexity.settings

import com.github.nikolaikopernik.codecomplexity.core.ComplexitySink
import com.github.nikolaikopernik.codecomplexity.core.PointType
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.util.IconLoader
import java.awt.Color
import javax.swing.Icon

object ComplexitySettings {
    private val colorSimpleComplexity: Color = "#4bb14f".parseColor()
    private val colorMiddleComplexity: Color = "#FF8400".parseColor()
    private val colorHighComplexity: Color = "#FC2947".parseColor()

    private val icons = mutableMapOf<Color, Icon>()

    fun getText(complexity: ComplexitySink, state: SettingsState): String {
        val value = complexity.getComplexity()
        return if (state.usePlainComplexity) {
            state.determineLevel(value,
                                 complexity.getPoints().any { it.type == PointType.METHOD },
                                 { "simple ($value)" },
                                 { "mildly complex ($value)" },
                                 { "very complex ($value)" })
        } else {
            val threshold = if (complexity.getPoints().any { it.type == PointType.METHOD })
                state.limitSimpleLessThan * 4 else state.limitSimpleLessThan
            val pncValue = complexity.getComplexity() * 100 / threshold
            state.determineLevel(value,
                                 complexity.getPoints().any { it.type == PointType.METHOD },
                                 { "simple ($pncValue%)" },
                                 { "mildly complex ($pncValue%)" },
                                 { "very complex ($pncValue%)" })
        }
    }

    fun getColor(complexity: ComplexitySink, state: SettingsState): Color {
        return state.determineLevel(complexity.getComplexity(),
                                    complexity.getPoints().any { it.type == PointType.METHOD },
                                    { DefaultLanguageHighlighterColors.INLINE_PARAMETER_HINT.defaultAttributes.foregroundColor },
                                    { colorMiddleComplexity },
                                    { colorHighComplexity })
    }


    fun getIcon(complexity: ComplexitySink, state: SettingsState): Icon {
        return state.determineLevel(complexity.getComplexity(),
                                    complexity.getPoints().any { it.type == PointType.METHOD },
                                    { IconLoader.getIcon("simple.svg", ComplexitySettings::class.java.classLoader) },
                                    { IconLoader.getIcon("medium.svg", ComplexitySettings::class.java.classLoader) },
                                    { IconLoader.getIcon("hard.svg", ComplexitySettings::class.java.classLoader) })
    }


    /**
     * This method can determine complexity level based on
     * the actual complexity score and the setting from user.
     */
    private fun <T> SettingsState.determineLevel(complexity: Int,
                                                 isClassComplexity: Boolean,
                                                 simpleFun: (Int) -> T,
                                                 middleFun: (Int) -> T,
                                                 hardFun: (Int) -> T) =
        when {
            complexity < limitSimpleLessThan * if (isClassComplexity) 4 else 1 -> simpleFun.invoke(complexity)
            complexity < limitVeryComplexMoreThan * if (isClassComplexity) 4 else 1 -> middleFun.invoke(complexity)
            else -> hardFun.invoke(complexity)
        }
}

private fun String.parseColor() = Color(this.drop(1).toInt(16), false)


fun Color.getContrastColor(): Color {
    val y = (299 * this.red + 587 * this.green + 114 * this.blue) / 1000.0
    return if (y >= 128) Color.black else Color.white
}

fun Color.toHex(): String = String.format("#%02x%02x%02x", this.red, this.green, this.blue);
