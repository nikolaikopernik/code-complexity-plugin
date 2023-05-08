package com.github.nikolaikopernik.codecomplexity.ui

import com.github.nikolaikopernik.codecomplexity.core.ComplexitySink
import com.github.nikolaikopernik.codecomplexity.core.PLUGIN_EP_NAME
import com.github.nikolaikopernik.codecomplexity.core.LanguageInfoProvider
import com.github.nikolaikopernik.codecomplexity.core.PointType
import com.github.nikolaikopernik.codecomplexity.settings.ComplexitySettings
import com.intellij.codeInsight.hints.FactoryInlayHintsCollector
import com.intellij.codeInsight.hints.InlayHintsSink
import com.intellij.codeInsight.hints.presentation.InlayPresentation
import com.intellij.codeInsight.hints.presentation.InsetPresentation
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiRecursiveElementVisitor
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager

@Suppress("UnstableApiUsage")
class ComplexityFactoryInlayHintsCollector(private val languageInfoProvider: LanguageInfoProvider,
                                           private val editor: Editor) : FactoryInlayHintsCollector(editor) {


    private fun getClassComplexity(element: PsiElement): Int {
        return ComplexitySink().also { sink ->
            element.accept(object : PsiRecursiveElementVisitor() {
                override fun visitElement(element: PsiElement) {
                    if (languageInfoProvider.isClassMember(element)) {
                        sink.increaseComplexity(element.obtainElementComplexity(), PointType.METHOD)
                    } else {
                        super.visitElement(element)
                    }
                }
            })
        }.getComplexity()
    }

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

    private fun applySinkResults(element: PsiElement, score: Int, sink: InlayHintsSink) {
        getPresentation(element, score)?.let {
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

    private fun getPresentation(element: PsiElement, complexityScore: Int): InlayPresentation? {
        return getTextPresentation(complexityScore, editor)
            .let {
                InsetPresentation(
                    it,
                    top = 2,
                    down = 0
                )
            }
    }

    private fun getTextPresentation(complexityScore: Int, editor: Editor): InlayPresentation =
        InsetPresentation(factory.text(ComplexitySettings.getText(complexityScore)),
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

    private fun PsiElement.findProviderForElement(): LanguageInfoProvider {
        val language = this.language
        val provider = PLUGIN_EP_NAME.findFirstSafe { it.language == language }
        checkNotNull(provider) { "Failed to obtain LanguageInfoProvider for language with id: ${language.id}" }
        return provider
    }

    private fun PsiElement.obtainElementComplexity(): Int {
        return CachedValuesManager.getCachedValue(this) {
            // Search for the first provider with the same language on every recompute,
            // so there is no dependency on the reference to that provider.
            val provider = this.findProviderForElement()
            val sink = ComplexitySink()
            this.accept(provider.getVisitor(sink))
            val complexity = sink.getComplexity()
            CachedValueProvider.Result.create(complexity, this)
        }
    }
}
