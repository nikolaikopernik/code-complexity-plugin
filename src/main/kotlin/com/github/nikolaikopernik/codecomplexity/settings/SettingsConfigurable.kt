package com.github.nikolaikopernik.codecomplexity.settings

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import javax.swing.JComponent

class SettingsConfigurable(val project: Project) : Configurable {
    private var component: SettingsComponent? = null

    override fun createComponent(): JComponent {
        component = SettingsComponent()
        return component!!.panel
    }

    override fun isModified(): Boolean {
        val state = SettingsState.INSTANCE
        return state.useDefaults != component?.getUseDefaults() ||
            state.usePlainComplexity != component?.getShowPlainComplexity() ||
            state.showIcon != component?.getShowIcon() ||
            state.limitSimpleLessThan != component?.getSimpleLimit() ||
            state.limitVeryComplexMoreThan != component?.getVeryComplexLimit() ||
            state.hintTextSimpleComplex != component?.getSimpleComplexText() ||
            state.hintTextMildlyComplex != component?.getMildlyComplexText() ||
            state.hintTextVeryComplex != component?.getVeryComplexText()
    }

    override fun apply() {
        val state = SettingsState.INSTANCE
        with(state) {
            useDefaults = component!!.getUseDefaults()
            usePlainComplexity = component!!.getShowPlainComplexity()
            showIcon = component!!.getShowIcon()
            limitSimpleLessThan = component!!.getSimpleLimit()
            limitVeryComplexMoreThan = component!!.getVeryComplexLimit()
            hintTextSimpleComplex = component!!.getSimpleComplexText()
            hintTextMildlyComplex = component!!.getMildlyComplexText()
            hintTextVeryComplex = component!!.getVeryComplexText()
        }
    }

    override fun reset() {
        val settings = SettingsState.INSTANCE
        component?.let {
            it.setSimpleLimit(settings.limitSimpleLessThan)
            it.setVeryComplexLimit(settings.limitVeryComplexMoreThan)
            it.setUseDefaults(settings.useDefaults)
            it.setShowPlainComplexity(settings.usePlainComplexity)
            it.setShowIcon(settings.showIcon)
            it.setSimpleComplexText(settings.hintTextSimpleComplex)
            it.setMildlyComplexText(settings.hintTextMildlyComplex)
            it.setVeryComplexText(settings.hintTextVeryComplex)
        }
    }

    override fun getDisplayName(): String {
        return "Something"
    }
}
