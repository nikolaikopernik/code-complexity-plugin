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
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiRecursiveElementVisitor
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import org.jetbrains.annotations.VisibleForTesting
import java.lang.ref.SoftReference

@Suppress("UnstableApiUsage")
class ComplexityFactoryInlayHintsCollector(private val complexityInfoProvider: ComplexityInfoProvider,
                                           private val editor: Editor) : FactoryInlayHintsCollector(editor) {
    private val setting: SettingsState = SettingsState.INSTANCE

    /**
     * File-stamp cache, unlike [obtainElementComplexity]'s text hash: a class's score depends on
     * all its members, so hashing it would cost as much as the walk it saves.
     */
    @VisibleForTesting
    internal fun getClassComplexity(element: PsiElement): ComplexitySink {
        return CachedValuesManager.getCachedValue(element) {
            val sink = ComplexitySink()
            element.accept(object : PsiRecursiveElementVisitor() {
                override fun visitElement(element: PsiElement) {
                    if (complexityInfoProvider.isComplexitySuitableMember(element)) {
                        sink.increaseComplexity(element.obtainElementComplexity().getComplexity(), PointType.METHOD)
                    } else {
                        super.visitElement(element)
                    }
                }
            })
            CachedValueProvider.Result.create(sink, element)
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
        getPresentation(score).let {
            sink.addInlineElement(
                offset = element.textOffset,
                relatesToPrecedingText = true,
                presentation = it,
                placeAtTheEndOfLine = true
            )
        }
    }

    private fun getPresentation(complexityScore: ComplexitySink): InlayPresentation {
        val text = factory.inset(factory.offsetFromTopForSmallText(getTextPresentation(complexityScore)))
        if (setting.showIcon) {
            return factory.seq(
                factory.offsetFromTopForSmallText(
                    factory.smallScaledIcon(complexityScore.getConfiguredIcon())),
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

    private fun getTextPresentation(complexity: ComplexitySink): InlayPresentation =
        correctTextColour(
            factory.inset(
                factory.smallText(complexity.getConfiguredText()),
                left = 2, right = 2))

    override fun equals(other: Any?): Boolean =
        other is ComplexityFactoryInlayHintsCollector && editor == other.editor

    override fun hashCode(): Int {
        return editor.hashCode()
    }
}

private val COMPLEXITY_KEY = Key.create<SoftReference<CachedComplexity>>("code.complexity.cachedSink")

private class CachedComplexity(val textHash: Long, val sink: ComplexitySink)

/**
 * Cached version of complexity, keyed to a hash of the element's own text rather than the file's
 * modification stamp: a member's score only depends on its own text, and file-stamp caching made
 * one keystroke recompute every member in the file (issue #30).
 */
fun PsiElement.obtainElementComplexity(givenProvider: ComplexityInfoProvider? = null): ComplexitySink {
    val textHash = hashOfCommittedText()
    if (textHash != null) {
        getUserData(COMPLEXITY_KEY)?.get()?.let { if (it.textHash == textHash) return it.sink }
    }

    // Search for the first provider with the same language on every recompute,
    // so there is no dependency on the reference to that provider.
    val provider = givenProvider ?: this.findProviderForElement()
    val sink = ComplexitySink()
    this.accept(provider.getVisitor(sink))

    // Soft, which is what CachedValuesManager did with it, so these stay reclaimable.
    if (textHash != null) putUserData(COMPLEXITY_KEY, SoftReference(CachedComplexity(textHash, sink)))
    return sink
}

/**
 * FNV-1a over this element's characters, read straight from the document to avoid building a string.
 * Null if the document isn't committed yet, so the caller recomputes instead of hashing stale text.
 */
private fun PsiElement.hashOfCommittedText(): Long? {
    val document = containingFile?.viewProvider?.document ?: return null
    if (!PsiDocumentManager.getInstance(project).isCommitted(document)) return null
    val range = textRange
    if (range == null || range.endOffset > document.textLength) return null

    val chars = document.charsSequence
    var hash = FNV_BASIS
    for (i in range.startOffset until range.endOffset) {
        hash = (hash xor chars[i].code.toLong()) * FNV_PRIME
    }
    return (hash xor range.length.toLong()) * FNV_PRIME
}

private const val FNV_BASIS = -0x340d631b7bdddcdbL
private const val FNV_PRIME = 0x100000001b3L
