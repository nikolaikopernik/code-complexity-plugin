package com.github.nikolaikopernik.codecomplexity.ui

import com.github.nikolaikopernik.codecomplexity.core.LanguageInfoProvider
import com.intellij.codeInsight.hints.InlayHintsProviderFactory
import com.intellij.codeInsight.hints.ProviderInfo
import com.intellij.openapi.project.Project


@Suppress("UnstableApiUsage")
internal class ComplexityInlayHintsProviderFactory : InlayHintsProviderFactory {
    override fun getProvidersInfo(project: Project): List<ProviderInfo<out Any>> {
        return LanguageInfoProvider.EP_NAME.extensionList
            .map { ProviderInfo(it.language, ComplexityInlayHintsProvider(it)) }
    }
}
