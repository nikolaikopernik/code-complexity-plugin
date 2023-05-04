package com.github.nikolaikopernik.codecomplexity.core

import com.intellij.codeInsight.hints.NoSettings
import com.intellij.codeInsight.hints.SettingsKey
import com.intellij.lang.Language
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.psi.PsiElement


@Suppress("UnstableApiUsage")
interface LanguageInfoProvider {
    companion object {
        var EP_NAME: ExtensionPointName<LanguageInfoProvider> =
            ExtensionPointName.create("com.github.nikolaikopernik.codecomplexity.languageInfoProvider")
        val myKey = SettingsKey<NoSettings>("code.complexity.hint")
    }

    fun getVisitor(sink: ComplexitySink): ElementVisitor

    val language: Language

    fun isClassMember(element: PsiElement): Boolean

    fun isClassWithBody(element: PsiElement): Boolean
}
