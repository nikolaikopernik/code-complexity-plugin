@file:Suppress("UnstableApiUsage")

package com.github.nikolaikopernik.codecomplexity.core

import com.intellij.codeInsight.hints.NoSettings
import com.intellij.codeInsight.hints.SettingsKey
import com.intellij.lang.Language
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.psi.PsiElement

val PLUGIN_EP_NAME: ExtensionPointName<ComplexityInfoProvider> = ExtensionPointName("com.github.nikolaikopernik.codecomplexity.languageInfoProvider")
val PLUGIN_HINT_KEY = SettingsKey<NoSettings>("code.complexity.hint")

val SUPPORTED_LANGUAGES = setOf("java", "kotlin", "python", "go")

/**
 * Main interface to calculate complexity for different languages.
 * All those work via this interface because supported languages can differ. If some language is not supported
 * then any code in the plugin shouldn't load any class from that language package.
 */
interface ComplexityInfoProvider {
    /**
     * Create a visitor that can go other elements and calculate complexity for a particular language
     */
    fun getVisitor(sink: ComplexitySink): ElementVisitor

    /**
     * Supported language
     */
    val language: Language

    /**
     * Check if complexity needs to be calculated for the given [PsiElement].
     * If this method returns true for some element - then a hint will be showed next to it in the editor.
     * That can also differ from language to language
     */
    fun isComplexitySuitableMember(element: PsiElement): Boolean

    /**
     * If it's a class element and class-level complexity need to be calculated for this element
     */
    fun isClassWithBody(element: PsiElement): Boolean

    /**
     * Special case.
     * For the given method-related element the name declararion element need to be found.
     * Probably it's possible to find it in common psi elements, but it's way easier to cast it
     * for specific language and find it in specific elements.
     */
    fun getNameElementFor(element: PsiElement): PsiElement
}

/**
 * Get complexity provider for the given psi element.
 */
fun PsiElement.findProviderForElement(): ComplexityInfoProvider {
    val language = this.language
    val provider = PLUGIN_EP_NAME.findFirstSafe { it.language == language }
    checkNotNull(provider) { "Failed to obtain ComplexityInfoProvider for language with id: ${language.id}" }
    return provider
}


fun Language.isSupportedByComplexityPlugin(): Boolean {
    return this.id.lowercase() in SUPPORTED_LANGUAGES
}
