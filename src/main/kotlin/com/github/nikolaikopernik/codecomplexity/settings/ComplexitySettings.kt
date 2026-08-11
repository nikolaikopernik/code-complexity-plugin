package com.github.nikolaikopernik.codecomplexity.settings

import com.github.nikolaikopernik.codecomplexity.core.ComplexitySink
import com.github.nikolaikopernik.codecomplexity.core.PointType
import com.github.nikolaikopernik.codecomplexity.settings.ComplexityLevel.HARD
import com.github.nikolaikopernik.codecomplexity.settings.ComplexityLevel.MIDDLE
import com.github.nikolaikopernik.codecomplexity.settings.ComplexityLevel.SIMPLE
import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

fun ComplexitySink.getConfiguredText(): String {
    val state = SettingsState.INSTANCE
    val value = this.getComplexity()
    val showValue = this.getValueToShow()
    return state.determineLevel(value,
                                this.getPoints().any { it.type == PointType.METHOD },
                                { customHintTextWithScore(state.hintTextSimpleComplex, showValue) },
                                { customHintTextWithScore(state.hintTextMildlyComplex, showValue) },
                                { customHintTextWithScore(state.hintTextVeryComplex, showValue) })
}

fun ComplexitySink.getValueToShow(): String {
    val state = SettingsState.INSTANCE
    return if (state.usePlainComplexity) {
        this.getComplexity().toString()
    } else {
        val threshold = if (this.getPoints().any { it.type == PointType.METHOD })
            state.limitSimpleLessThan * 4 else state.limitSimpleLessThan
        "${this.getComplexity() * 100 / threshold}%"
    }
}

fun ComplexitySink.getConfiguredIcon(): Icon {
    return SettingsState.INSTANCE.determineLevel(this.getComplexity(),
                                                 this.getPoints().any { it.type == PointType.METHOD },
                                                 { IconLoader.getIcon("simple.svg", this::class.java.classLoader) },
                                                 { IconLoader.getIcon("medium.svg", this::class.java.classLoader) },
                                                 { IconLoader.getIcon("hard.svg", this::class.java.classLoader) })
}

fun ComplexitySink.getConfiguredLevel() =
    SettingsState.INSTANCE.determineLevel(this.getComplexity(), false, { SIMPLE }, { MIDDLE }, { HARD })

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
