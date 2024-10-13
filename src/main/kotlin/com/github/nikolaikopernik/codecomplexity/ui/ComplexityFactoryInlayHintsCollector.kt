package com.github.nikolaikopernik.codecomplexity.ui

import com.github.nikolaikopernik.codecomplexity.core.ComplexityInfoProvider
import com.github.nikolaikopernik.codecomplexity.core.ComplexitySink
import com.github.nikolaikopernik.codecomplexity.core.PointType
import com.github.nikolaikopernik.codecomplexity.core.findProviderForElement
import com.github.nikolaikopernik.codecomplexity.settings.SettingsState
import com.github.nikolaikopernik.codecomplexity.settings.getConfiguredIcon
import com.github.nikolaikopernik.codecomplexity.settings.getConfiguredText
import com.intellij.codeInsight.hints.FactoryInlayHintsCollector
import com.intellij.codeInsight.hints.InlayHintsSink
import com.intellij.codeInsight.hints.presentation.InlayPresentation
import com.intellij.codeInsight.hints.presentation.WithAttributesPresentation
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors.INLAY_TEXT_WITHOUT_BACKGROUND
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiRecursiveElementVisitor
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager

@Suppress("UnstableApiUsage")
class ComplexityFactoryInlayHintsCollector(private val complexityInfoProvider: ComplexityInfoProvider,
                                           private val editor: Editor) : FactoryInlayHintsCollector(editor) {
    private val setting: SettingsState = SettingsState.INSTANCE

    private fun getClassComplexity(element: PsiElement): ComplexitySink {
        return ComplexitySink().also { sink ->
            element.accept(object : PsiRecursiveElementVisitor() {
                override fun visitElement(element: PsiElement) {
                    if (complexityInfoProvider.isComplexitySuitableMember(element)) {
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
        val complexityScore = if (complexityInfoProvider.isClassWithBody(element)) {
            getClassComplexity(element)
        } else if (complexityInfoProvider.isComplexitySuitableMember(element)) {
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

    private fun getPresentation(element: PsiElement, complexityScore: ComplexitySink): InlayPresentation {
        val text = factory.inset(factory.offsetFromTopForSmallText(getTextPresentation(complexityScore, editor)))
        if (setting.showIcon) {
            return factory.seq(
                factory.offsetFromTopForSmallText(
                    factory.scaledIcon(
                        complexityScore.getConfiguredIcon(),
                        1.0f)),
                text)
        }
        return text
    }

    /**
     * For some reason INLAY_DEFAULT (which is used in [com.intellij.codeInsight.hints.InlayPresentationFactory.smallText])
     * doesn't work nicely in HighContrast theme.
     */
    private fun correctTextColour(base: InlayPresentation): InlayPresentation {
        return WithAttributesPresentation(base,
                                          INLAY_TEXT_WITHOUT_BACKGROUND,
                                          editor,
                                          WithAttributesPresentation.AttributesFlags().withIsDefault(true))
    }

    private fun getTextPresentation(complexity: ComplexitySink, editor: Editor): InlayPresentation =
        correctTextColour(
            factory.inset(
                factory.smallText(complexity.getConfiguredText()),
                left = 2, right = 2))

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
fun PsiElement.obtainElementComplexity(givenProvider: ComplexityInfoProvider? = null): ComplexitySink {
    return CachedValuesManager.getCachedValue(this) {
        // Search for the first provider with the same language on every recompute,
        // so there is no dependency on the reference to that provider.
        val provider = givenProvider ?: this.findProviderForElement()
        val sink = ComplexitySink()
        this.accept(provider.getVisitor(sink))
        CachedValueProvider.Result.create(sink, this)
    }
}
