package com.github.nikolaikopernik.codecomplexity.core

import com.github.nikolaikopernik.codecomplexity.settings.ComplexityLevel
import com.github.nikolaikopernik.codecomplexity.settings.getConfiguredLevel
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.psi.PsiFile

/**
 * High complexity inspection.
 * Inspects all the methods (methods only for now) with the complexity >= HARD limit (configurable).
 * Uses [ComplexityInfoProvider] so isn't coupled with any language.
 * Uses a bit different algorithm: instead of using a normal Visitor we check the file all at once (should be more
 * quick).
 * All the unknown files are skipped.
 * @see ComplexityInfoProvider
 * @see visitFileFast
 */
class HighCodeComplexityInspection : LocalInspectionTool() {

    /**
     * Check file fast in 2 steps:
     *  - traverse all [PsiElement]s in breadth first order
     *  - once any methods are found - calculate complexity for them
     */
    override fun checkFile(file: PsiFile, manager: InspectionManager, isOnTheFly: Boolean): Array<ProblemDescriptor> {
        val problems = mutableListOf<ProblemDescriptor>()
        if (file.language.isSupportedByComplexityPlugin()) {
            val provider = file.findProviderForElement()
            file.visitFileFast(provider) { complexitySink, element ->
                if (complexitySink.getConfiguredLevel() == ComplexityLevel.HARD) {
                    // here element - is the entire method, if we put it all in problem description
                    // the entire method code block will be highlighted - that's too much
                    // that's why there is a special method to get named declaration only
                    val namedElement = provider.getNameElementFor(element)
                    val problemRef = "'${namedElement.text}()'"
                    problems.add(manager.createProblemDescriptor(namedElement,
                                                                 "fun $problemRef is very complex",
                                                                 isOnTheFly,
                                                                 emptyArray(),
                                                                 ProblemHighlightType.GENERIC_ERROR_OR_WARNING))
                }
            }
        }
        return problems.toTypedArray()
    }
}
