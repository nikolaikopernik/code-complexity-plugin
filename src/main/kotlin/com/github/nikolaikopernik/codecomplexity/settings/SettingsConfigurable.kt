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
            state.simpleComplexText != component?.getSimpleComplexText() ||
            state.mildlyComplexText != component?.getMildlyComplexText() ||
            state.veryComplexText != component?.getVeryComplexText()
    }

    override fun apply() {
        val state = SettingsState.INSTANCE
        with(state) {
            useDefaults = component!!.getUseDefaults()
            usePlainComplexity = component!!.getShowPlainComplexity()
            showIcon = component!!.getShowIcon()
            limitSimpleLessThan = component!!.getSimpleLimit()
            limitVeryComplexMoreThan = component!!.getVeryComplexLimit()
            simpleComplexText = component!!.getSimpleComplexText()
            mildlyComplexText = component!!.getMildlyComplexText()
            veryComplexText = component!!.getVeryComplexText()
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
            it.setSimpleComplexText(settings.simpleComplexText)
            it.setMildlyComplexText(settings.mildlyComplexText)
            it.setVeryComplexText(settings.veryComplexText)
        }
    }

    override fun getDisplayName(): String {
        return "Something"
    }
}
