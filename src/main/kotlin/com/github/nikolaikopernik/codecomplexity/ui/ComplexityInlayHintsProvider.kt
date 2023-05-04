package com.github.nikolaikopernik.codecomplexity.ui

import com.github.nikolaikopernik.codecomplexity.core.LanguageInfoProvider
import com.intellij.codeInsight.hints.ChangeListener
import com.intellij.codeInsight.hints.ImmediateConfigurable
import com.intellij.codeInsight.hints.InlayHintsCollector
import com.intellij.codeInsight.hints.InlayHintsProvider
import com.intellij.codeInsight.hints.InlayHintsSink
import com.intellij.codeInsight.hints.NoSettings
import com.intellij.codeInsight.hints.SettingsKey
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiFile
import javax.swing.JPanel

@Suppress("UnstableApiUsage")
class ComplexityInlayHintsProvider(private val languageInfoProvider: LanguageInfoProvider) : InlayHintsProvider<NoSettings> {
    override fun getCollectorFor(file: PsiFile,
                                 editor: Editor,
                                 settings: NoSettings,
                                 sink: InlayHintsSink): InlayHintsCollector {
        return ComplexityFactoryInlayHintsCollector(languageInfoProvider, editor)
    }

    override fun createSettings() = NoSettings()

    override val key: SettingsKey<NoSettings> = LanguageInfoProvider.myKey

    override val name: String = "ComplexityInlayProvider"

    override val previewText = "ComplexityInlayProvider"

    override val isVisibleInSettings: Boolean = false

    override fun createConfigurable(settings: NoSettings): ImmediateConfigurable {
        return object : ImmediateConfigurable {
            override fun createComponent(listener: ChangeListener) = JPanel()
        }
    }
}
