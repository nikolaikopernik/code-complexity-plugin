package com.github.nikolaikopernik.codecomplexity.settings

import com.intellij.ide.HelpTooltip
import com.intellij.ui.IdeBorderFactory
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.ui.components.fields.IntegerField
import com.intellij.ui.layout.selected
import com.intellij.ui.util.maximumWidth
import com.intellij.util.ui.FormBuilder
import java.awt.BorderLayout
import java.awt.event.ItemEvent
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.JPanel

/**
 * Supports creating and managing a [JPanel] for the Settings Dialog.
 */
class SettingsComponent {
    val panel: JPanel
    private val explanationText = JBLabel(SettingsBundle.message("explanationText"))
    private val useDefaults = JBCheckBox(SettingsBundle.message("useDefaults"))
    private val showOriginalScore = JBCheckBox(SettingsBundle.message("showOriginalScore"))
    private val showIcon = JBCheckBox(SettingsBundle.message("showIcon"))
    private val simpleLimit = IntegerField(null, 1, 100)
    private val veryComplexLimit = IntegerField(null, 2, 100)
    private val customHintTextSimpleField = JBTextField(SettingsBundle.message("simpleComplexDefaultText"))
    private val customHintTextMildlyComplexField = JBTextField(SettingsBundle.message("mildlyComplexDefaultText"))
    private val customHintTextVeryComplexField = JBTextField(SettingsBundle.message("veryComplexDefaultText"))

    init {
        HelpTooltip().setDescription(SettingsBundle.message("customComplexDefaultTextToolTip")).installOn(customHintTextSimpleField)
        HelpTooltip().setDescription(SettingsBundle.message("customComplexDefaultTextToolTip")).installOn(customHintTextMildlyComplexField)
        HelpTooltip().setDescription(SettingsBundle.message("customComplexDefaultTextToolTip")).installOn(customHintTextVeryComplexField)
        val limits = JPanel(BorderLayout())
        limits.border = IdeBorderFactory.createTitledBorder(SettingsBundle.message("limitsLabel"))
        limits.add(useDefaults, BorderLayout.NORTH)
        limits.add(LabelledComponent(SettingsBundle.message("simpleComplexScoreLimit"), simpleLimit), BorderLayout.CENTER)
        limits.add(LabelledComponent(SettingsBundle.message("veryComplexScoreLimit"), veryComplexLimit), BorderLayout.SOUTH)
        val customTextPanel = JPanel()
        customTextPanel.layout = BoxLayout(customTextPanel, BoxLayout.Y_AXIS)
        customTextPanel.border = IdeBorderFactory.createTitledBorder(SettingsBundle.message("customDescriptionText"))
        customTextPanel.add(LabelledComponent(SettingsBundle.message("customSimpleComplexLabel"), customHintTextSimpleField))
        customTextPanel.add(LabelledComponent(SettingsBundle.message("customMildlyComplexLabel"), customHintTextMildlyComplexField))
        customTextPanel.add(LabelledComponent(SettingsBundle.message("customVeryComplexLabel"), customHintTextVeryComplexField))

        panel = FormBuilder.createFormBuilder()
            .addComponent(explanationText)
            .addComponent(JPanel())
            .addComponent(limits)
            .addComponent(customTextPanel)
            .addComponent(showOriginalScore)
            .addComponent(showIcon)
            .addComponentFillVertically(JPanel(), 0)
            .panel

        useDefaults.addItemListener {
            simpleLimit.setEnabled(it.stateChange != ItemEvent.SELECTED)
            veryComplexLimit.setEnabled(it.stateChange != ItemEvent.SELECTED)
            if (useDefaults.isSelected) {
                simpleLimit.value = DEFAULT_LIMIT_SIMPLE
                veryComplexLimit.value = DEFAULT_LIMIT_VERY_COMPLEX
            }
        }
        explanationText.isAllowAutoWrapping = true
        explanationText.maximumWidth = 400
    }

    val preferredFocusedComponent: JComponent
        get() = useDefaults

    fun setUseDefaults(useDefaults: Boolean) {
        this.useDefaults.setSelected(useDefaults)
    }

    fun setShowPlainComplexity(showPlainComplexity: Boolean) {
        this.showOriginalScore.setSelected(showPlainComplexity)
    }

    fun setShowIcon(showIcon: Boolean) {
        this.showIcon.setSelected(showIcon)
    }

    fun setSimpleLimit(simpleLimit: Int) {
        this.simpleLimit.value = simpleLimit
    }

    fun setVeryComplexLimit(veryComplexLimit: Int) {
        this.veryComplexLimit.value = veryComplexLimit
    }

    fun setSimpleComplexText(text: String) {
        this.customHintTextSimpleField.text = text
    }

    fun setMildlyComplexText(text: String) {
        this.customHintTextMildlyComplexField.text = text
    }

    fun setVeryComplexText(text: String) {
        this.customHintTextVeryComplexField.text = text
    }

    fun getUseDefaults(): Boolean = useDefaults.selected.invoke()

    fun getShowPlainComplexity(): Boolean = showOriginalScore.selected.invoke()

    fun getShowIcon(): Boolean = showIcon.selected.invoke()

    fun getSimpleLimit(): Int = simpleLimit.value

    fun getVeryComplexLimit(): Int = veryComplexLimit.value

    fun getSimpleComplexText(): String = customHintTextSimpleField.text

    fun getMildlyComplexText(): String = customHintTextMildlyComplexField.text

    fun getVeryComplexText(): String = customHintTextVeryComplexField.text
}
