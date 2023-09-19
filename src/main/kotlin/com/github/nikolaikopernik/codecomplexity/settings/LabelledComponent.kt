package com.github.nikolaikopernik.codecomplexity.settings

import com.intellij.ui.components.JBLabel
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel

/**
 * A simple wrapper for label + component combination
 */
class LabelledComponent(private val description: String,
                        private val component: JComponent) : JPanel(BorderLayout()) {
    init {
        this.add(JBLabel(description), BorderLayout.WEST)
        this.add(component, BorderLayout.CENTER)
    }
}
