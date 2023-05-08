@file:Suppress("UnstableApiUsage")

package com.github.nikolaikopernik.codecomplexity.core

import com.intellij.codeInsight.hints.NoSettings
import com.intellij.codeInsight.hints.SettingsKey
import com.intellij.lang.Language
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.psi.PsiElement

val PLUGIN_EP_NAME: ExtensionPointName<LanguageInfoProvider> = ExtensionPointName("com.github.nikolaikopernik.codecomplexity.languageInfoProvider")
val PLUGIN_HINT_KEY = SettingsKey<NoSettings>("code.complexity.hint")

interface LanguageInfoProvider {
    fun getVisitor(sink: ComplexitySink): ElementVisitor

    val language: Language

    fun isClassMember(element: PsiElement): Boolean

    fun isClassWithBody(element: PsiElement): Boolean
}
