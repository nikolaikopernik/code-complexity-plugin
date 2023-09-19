package com.github.nikolaikopernik.codecomplexity.settings

import com.intellij.ui.IdeBorderFactory
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.fields.IntegerField
import com.intellij.ui.layout.selected
import com.intellij.ui.util.maximumWidth
import com.intellij.util.ui.FormBuilder
import java.awt.BorderLayout
import java.awt.event.ItemEvent
import javax.swing.JComponent
import javax.swing.JPanel

/**
 * Supports creating and managing a [JPanel] for the Settings Dialog.
 */
class SettingsComponent {
    val panel: JPanel
    private val explanationText = JBLabel("""<html>
        Language constructions which increase the complexity add +1 to the complexity score. <br>
        Nesting increases this rate proportionally to the nesting level. <br>
        The sum of all the scores for a method determines it's complexity. <br>
        By default, method is highlighted as simple if it's complexity is less then 8 (and <br>
        very complex if it's more than 12) but using this form you can adjust these limits. <br>
        <br>
        For classes the complexity limits are calculated as `4 * method complexity limits`.
        </html>
        """)
    private val useDefaults = JBCheckBox("Use default values ")
    private val showOriginalScore = JBCheckBox("Show the original score instead of percentages ")
    private val simpleLimit = IntegerField("Complexity score for simple methods:", 1, 100)
    private val veryComplexLimit = IntegerField("Complexity score for very complex methods:", 2, 100)

    init {
        val limits = JPanel(BorderLayout())
        limits.border = IdeBorderFactory.createTitledBorder("Limits")
        limits.add(useDefaults, BorderLayout.NORTH)
        limits.add(LabelledComponent("Simple score less than: ", simpleLimit), BorderLayout.CENTER)
        limits.add(LabelledComponent("Very complex score more than: ", veryComplexLimit), BorderLayout.SOUTH)
        panel = FormBuilder.createFormBuilder()
            .addComponent(explanationText)
            .addComponent(JPanel())
            .addComponent(limits)
            .addComponent(showOriginalScore)
            .addComponentFillVertically(JPanel(), 0)
            .panel

        useDefaults.addItemListener(){
            simpleLimit.setEnabled(it.stateChange != ItemEvent.SELECTED)
            veryComplexLimit.setEnabled(it.stateChange != ItemEvent.SELECTED)
            if(useDefaults.isSelected){
                simpleLimit.value = DEFAULT_LIMIT_SIMPLE
                veryComplexLimit.value = DEFAULT_LIMIT_VERY_COMPLEX
            }
        }
        explanationText.isAllowAutoWrapping = true
        explanationText.maximumWidth = 400
    }

    val preferredFocusedComponent: JComponent
        get() = useDefaults

    fun setUseDefaults(useDefaults: Boolean){
        this.useDefaults.setSelected(useDefaults)
    }

    fun setShowPlainComplexity(showPlainComplexity: Boolean){
        this.showOriginalScore.setSelected(showPlainComplexity)
    }

    fun setSimpleLimit(simpleLimit: Int){
        this.simpleLimit.value = simpleLimit
    }

    fun setVeryComplexLimit(veryComplexLimit: Int){
        this.veryComplexLimit.value = veryComplexLimit
    }

    fun getUseDefaults(): Boolean = useDefaults.selected.invoke()

    fun getShowPlainComplexity(): Boolean = showOriginalScore.selected.invoke()

    fun getSimpleLimit(): Int = simpleLimit.value

    fun getVeryComplexLimit():Int = veryComplexLimit.value
}
