package com.github.nikolaikopernik.codecomplexity.ui

import com.github.nikolaikopernik.codecomplexity.core.ComplexitySink
import com.github.nikolaikopernik.codecomplexity.core.LanguageInfoProvider
import com.github.nikolaikopernik.codecomplexity.core.PLUGIN_EP_NAME
import com.github.nikolaikopernik.codecomplexity.core.PointType
import com.github.nikolaikopernik.codecomplexity.settings.SettingsState
import com.github.nikolaikopernik.codecomplexity.settings.getConfiguredIcon
import com.github.nikolaikopernik.codecomplexity.settings.getConfiguredText
import com.intellij.codeInsight.hints.FactoryInlayHintsCollector
import com.intellij.codeInsight.hints.InlayHintsSink
import com.intellij.codeInsight.hints.presentation.InlayPresentation
import com.intellij.codeInsight.hints.presentation.InlayTextMetricsStorage
import com.intellij.codeInsight.hints.presentation.InsetPresentation
import com.intellij.codeInsight.hints.presentation.ScaledIconPresentation
import com.intellij.codeInsight.hints.presentation.SequencePresentation
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiRecursiveElementVisitor
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager

@Suppress("UnstableApiUsage")
class ComplexityFactoryInlayHintsCollector(private val languageInfoProvider: LanguageInfoProvider,
                                           private val editor: Editor) : FactoryInlayHintsCollector(editor) {
    private val setting: SettingsState = SettingsState.INSTANCE

    private fun getClassComplexity(element: PsiElement): ComplexitySink {
        return ComplexitySink().also { sink ->
            element.accept(object : PsiRecursiveElementVisitor() {
                override fun visitElement(element: PsiElement) {
                    if (languageInfoProvider.isClassMember(element)) {
                        sink.increaseComplexity(element.obtainElementComplexity().getComplexity(), PointType.METHOD)
                    } else {
                        super.visitElement(element)
                    }
                }
            })
        }
    }

    /**
     * Main method to go other the editor elements and collect inlay hints.
     * This method makes the class to work as a visitor.
     */
    override fun collect(element: PsiElement, editor: Editor, sink: InlayHintsSink): Boolean {
        val complexityScore = if (languageInfoProvider.isClassWithBody(element)) {
            getClassComplexity(element)
        } else if (languageInfoProvider.isClassMember(element)) {
            element.obtainElementComplexity()
        } else null

        complexityScore?.let { score ->
            applySinkResults(element, score, sink)
        }

        return true
    }

    private fun applySinkResults(element: PsiElement, score: ComplexitySink, sink: InlayHintsSink) {
        getPresentation(element, score).let {
            sink.addInlineElement(
                offset = element.textOffset,
                relatesToPrecedingText = true,
                presentation = it,
                placeAtTheEndOfLine = true
            )
        }
    }

    private fun InlayPresentation.shiftTo(offset: Int, editor: Editor): InlayPresentation {
        val document = editor.document
        val column = offset - document.getLineStartOffset(document.getLineNumber(offset))

        return factory.seq(factory.textSpacePlaceholder(column, true), this)
    }

    private fun getPresentation(element: PsiElement, complexityScore: ComplexitySink): InlayPresentation {
        val insetPresentations = mutableListOf<InsetPresentation>()
        if (setting.showIcon) {
            insetPresentations.add(
                InsetPresentation(
                    ScaledIconPresentation(
                        InlayTextMetricsStorage(editor),
                        true,
                        complexityScore.getConfiguredIcon(),
                        editor.component),
                    top = 6),
            )
        }
        insetPresentations.add(InsetPresentation(getTextPresentation(complexityScore, editor), top = 2))
        return InsetPresentation(SequencePresentation(insetPresentations))
    }

    private fun getTextPresentation(complexity: ComplexitySink, editor: Editor): InlayPresentation =
        InsetPresentation(factory.text(complexity.getConfiguredText()),
                          top = 4, down = 4, left = 6, right = 6)

    override fun equals(other: Any?): Boolean {
        if (other is ComplexityFactoryInlayHintsCollector) {
            return editor == other.editor
        }
        return false
    }

    override fun hashCode(): Int {
        return editor.hashCode()
    }
}

/**
 * Cached version of complexity.
 * Use this one as it speeds up the calculations.
 */
fun PsiElement.obtainElementComplexity(): ComplexitySink {
    return CachedValuesManager.getCachedValue(this) {
        // Search for the first provider with the same language on every recompute,
        // so there is no dependency on the reference to that provider.
            val provider = this.findProviderForElement()
            val sink = ComplexitySink()
            this.accept(provider.getVisitor(sink))
            CachedValueProvider.Result.create(sink, this)
    }
}

private fun PsiElement.findProviderForElement(): LanguageInfoProvider {
    val language = this.language
    val provider = PLUGIN_EP_NAME.findFirstSafe { it.language == language }
    checkNotNull(provider) { "Failed to obtain LanguageInfoProvider for language with id: ${language.id}" }
    return provider
}
