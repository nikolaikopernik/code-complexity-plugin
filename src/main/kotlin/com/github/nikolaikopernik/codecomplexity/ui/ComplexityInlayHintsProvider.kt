package com.github.nikolaikopernik.codecomplexity.ui

import com.github.nikolaikopernik.codecomplexity.core.ComplexityInfoProvider
import com.github.nikolaikopernik.codecomplexity.core.PLUGIN_HINT_KEY
import com.github.nikolaikopernik.codecomplexity.core.isSupportedByComplexityPlugin
import com.intellij.codeInsight.hints.ChangeListener
import com.intellij.codeInsight.hints.ImmediateConfigurable
import com.intellij.codeInsight.hints.InlayHintsCollector
import com.intellij.codeInsight.hints.InlayHintsProvider
import com.intellij.codeInsight.hints.InlayHintsSink
import com.intellij.codeInsight.hints.NoSettings
import com.intellij.codeInsight.hints.SettingsKey
import com.intellij.lang.Language
import com.intellij.openapi.diagnostic.LoggerRt
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorKind
import com.intellij.psi.PsiFile
import javax.swing.JPanel

@Suppress("UnstableApiUsage")
class ComplexityInlayHintsProvider(private val complexityInfoProvider: ComplexityInfoProvider) : InlayHintsProvider<NoSettings> {

    private val logger = LoggerRt.getInstance(ComplexityInlayHintsProvider::class.java)

    override fun getCollectorFor(file: PsiFile,
                                 editor: Editor,
                                 settings: NoSettings,
                                 sink: InlayHintsSink): InlayHintsCollector? {
        if (editor.editorKind != EditorKind.MAIN_EDITOR) {
            // we don't want to show hint in all the possible editors, only in MAIN
            return null
        }
        return ComplexityFactoryInlayHintsCollector(complexityInfoProvider, editor)
    }

    override fun createSettings() = NoSettings()

    override val key: SettingsKey<NoSettings> = PLUGIN_HINT_KEY

    override val name: String = "ComplexityInlayProvider"

    override val previewText = "ComplexityInlayProvider"

    override val isVisibleInSettings: Boolean = false

    override fun isLanguageSupported(language: Language): Boolean {
        return language.isSupportedByComplexityPlugin()
    }
    override fun createConfigurable(settings: NoSettings): ImmediateConfigurable {
        return object : ImmediateConfigurable {
            override fun createComponent(listener: ChangeListener) = JPanel()
        }
    }
}
