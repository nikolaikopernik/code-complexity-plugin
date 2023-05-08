package com.github.nikolaikopernik.codecomplexity.ui

import com.github.nikolaikopernik.codecomplexity.core.LanguageInfoProvider
import com.github.nikolaikopernik.codecomplexity.core.PLUGIN_HINT_KEY
import com.intellij.codeInsight.hints.ChangeListener
import com.intellij.codeInsight.hints.ImmediateConfigurable
import com.intellij.codeInsight.hints.InlayHintsCollector
import com.intellij.codeInsight.hints.InlayHintsProvider
import com.intellij.codeInsight.hints.InlayHintsSink
import com.intellij.codeInsight.hints.NoSettings
import com.intellij.codeInsight.hints.SettingsKey
import com.intellij.lang.Language
import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiFile
import com.jetbrains.python.PythonLanguage
import org.jetbrains.kotlin.idea.KotlinLanguage
import javax.swing.JPanel

val SUPPORTED_LANGUAGES = setOf(JavaLanguage.INSTANCE, KotlinLanguage.INSTANCE, PythonLanguage.INSTANCE)

@Suppress("UnstableApiUsage")
class ComplexityInlayHintsProvider(private val languageInfoProvider: LanguageInfoProvider) : InlayHintsProvider<NoSettings> {
    override fun getCollectorFor(file: PsiFile,
                                 editor: Editor,
                                 settings: NoSettings,
                                 sink: InlayHintsSink): InlayHintsCollector {
        return ComplexityFactoryInlayHintsCollector(languageInfoProvider, editor)
    }

    override fun createSettings() = NoSettings()

    override val key: SettingsKey<NoSettings> = PLUGIN_HINT_KEY

    override val name: String = "ComplexityInlayProvider"

    override val previewText = "ComplexityInlayProvider"

    override val isVisibleInSettings: Boolean = false

    override fun isLanguageSupported(language: Language): Boolean {
        return language in SUPPORTED_LANGUAGES
    }

    override fun createConfigurable(settings: NoSettings): ImmediateConfigurable {
        return object : ImmediateConfigurable {
            override fun createComponent(listener: ChangeListener) = JPanel()
        }
    }
}
