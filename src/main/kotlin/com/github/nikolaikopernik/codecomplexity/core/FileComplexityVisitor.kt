package com.github.nikolaikopernik.codecomplexity.core

import com.github.nikolaikopernik.codecomplexity.ui.obtainElementComplexity
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import java.util.*

/**
 * Fast visitor for the entire file.
 * Goes over all psi elements in the file in breadth first order and search for all methods.
 * Once found, a complexity score is calculated for every method and the callback code block is called.
 */
fun PsiFile.visitFileFast(complexityInfoProvider: ComplexityInfoProvider,
                          block: (ComplexitySink, PsiElement) -> Unit) {
    val elementsToVisit: Queue<PsiElement> = LinkedList()
    elementsToVisit.addAll(this.children)
    while (elementsToVisit.isNotEmpty()) {
        val next = elementsToVisit.poll()
        if (complexityInfoProvider.isComplexitySuitableMember(next)) {
            val complexity = next.obtainElementComplexity()
            block.invoke(complexity, next)
        } else {
            elementsToVisit.addAll(next.children)
        }
    }
}
