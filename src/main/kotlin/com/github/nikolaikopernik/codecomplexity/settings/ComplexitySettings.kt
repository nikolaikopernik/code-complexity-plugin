package com.github.nikolaikopernik.codecomplexity.settings

import com.github.nikolaikopernik.codecomplexity.core.ComplexitySink
import com.github.nikolaikopernik.codecomplexity.core.PointType
import com.github.nikolaikopernik.codecomplexity.settings.ComplexityLevel.HARD
import com.github.nikolaikopernik.codecomplexity.settings.ComplexityLevel.MIDDLE
import com.github.nikolaikopernik.codecomplexity.settings.ComplexityLevel.SIMPLE
import com.intellij.openapi.application.CachedSingletonsRegistry
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.util.IconLoader
import com.intellij.ui.JBColor
import java.awt.Color
import java.util.function.Supplier
import javax.swing.Icon

private val COLOR_SIMPLE_COMPLEXITY: Color = "#4bb14f".parseColor()
private val COLOR_MIDDLE_COMPLEXITY: Color = "#FF8400".parseColor()
private val COLOR_HIGH_COMPLEXITY: Color = "#FC2947".parseColor()

private val stateSupplier: Supplier<SettingsState> = CachedSingletonsRegistry.lazy { SettingsState.INSTANCE }

private var settings = SettingsState.INSTANCE

fun ComplexitySink.getConfiguredText(): String {
    val value = this.getComplexity()
    return if (stateSupplier.get().usePlainComplexity) {
        stateSupplier.get().determineLevel(value,
                                           this.getPoints().any { it.type == PointType.METHOD },
                                           { customHintTextWithScore(settings.hintTextSimpleComplex, "$value") },
                                           { customHintTextWithScore(settings.hintTextMildlyComplex, "$value") },
                                           { customHintTextWithScore(settings.hintTextVeryComplex, "$value") })
    } else {
        val threshold = if (this.getPoints().any { it.type == PointType.METHOD })
            stateSupplier.get().limitSimpleLessThan * 4 else stateSupplier.get().limitSimpleLessThan
        val pncValue = this.getComplexity() * 100 / threshold
        stateSupplier.get().determineLevel(value,
                                           this.getPoints().any { it.type == PointType.METHOD },
                                           { customHintTextWithScore(settings.hintTextSimpleComplex, "$pncValue%") },
                                           { customHintTextWithScore(settings.hintTextMildlyComplex, "$pncValue%") },
                                           { customHintTextWithScore(settings.hintTextVeryComplex, "$pncValue%") })
    }
}

fun ComplexitySink.getConfiguredColor(): Color {
    return stateSupplier.get().determineLevel(this.getComplexity(),
                                              this.getPoints().any { it.type == PointType.METHOD },
                                              { DefaultLanguageHighlighterColors.INLINE_PARAMETER_HINT.defaultAttributes.foregroundColor },
                                              { COLOR_MIDDLE_COMPLEXITY },
                                              { COLOR_HIGH_COMPLEXITY })
}


fun ComplexitySink.getConfiguredIcon(): Icon {
    return stateSupplier.get().determineLevel(this.getComplexity(),
                                              this.getPoints().any { it.type == PointType.METHOD },
                                              { IconLoader.getIcon("simple.svg", this::class.java.classLoader) },
                                              { IconLoader.getIcon("medium.svg", this::class.java.classLoader) },
                                              { IconLoader.getIcon("hard.svg", this::class.java.classLoader) })
}

fun ComplexitySink.getConfiguredLevel() =
    stateSupplier.get().determineLevel(this.getComplexity(), false, { SIMPLE }, { MIDDLE }, { HARD })

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
        complexity <= limitVeryComplexMoreThan * if (isClassComplexity) 4 else 1 -> middleFun.invoke(complexity)
        else -> hardFun.invoke(complexity)
    }

/**
 * Because custom hint text is supported now (with score values templates in the text), we need to
 * replace the wildcard for the score with the actual value.
 */
private fun customHintTextWithScore(template: String, score: String): String {
    return if (template.isEmpty()) {
        template
    } else {
        template.replace("{score}", score)
    }
}

private fun String.parseColor() = Color(this.drop(1).toInt(16), false)


fun Color.getContrastColor(): Color {
    val y = (299 * this.red + 587 * this.green + 114 * this.blue) / 1000.0
    return if (y >= 128) JBColor.BLACK else JBColor.WHITE
}

fun Color.toHex(): String = String.format("#%02x%02x%02x", this.red, this.green, this.blue)
