package com.github.nikolaikopernik.codecomplexity.ui

import com.github.nikolaikopernik.codecomplexity.core.PLUGIN_EP_NAME
import com.intellij.codeInsight.hints.InlayHintsProviderFactory
import com.intellij.codeInsight.hints.ProviderInfo
import com.intellij.openapi.project.Project


@Suppress("UnstableApiUsage")
internal class ComplexityInlayHintsProviderFactory : InlayHintsProviderFactory {
    override fun getProvidersInfo(): List<ProviderInfo<out Any>> {
        return PLUGIN_EP_NAME.extensionList.map { ProviderInfo(it.language, ComplexityInlayHintsProvider(it)) }
    }

    /**
     * For compatibility with builds 222.*
     */
    override fun getProvidersInfo(project: Project): List<ProviderInfo<out Any>> {
        return getProvidersInfo()
    }
}
