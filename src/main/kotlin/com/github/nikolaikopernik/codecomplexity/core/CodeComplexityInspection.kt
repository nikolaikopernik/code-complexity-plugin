package com.github.nikolaikopernik.codecomplexity.core

import com.github.nikolaikopernik.codecomplexity.settings.ComplexityLevel
import com.github.nikolaikopernik.codecomplexity.settings.getConfiguredLevel
import com.github.nikolaikopernik.codecomplexity.ui.obtainElementComplexity
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.codeinsight.api.classic.inspections.AbstractKotlinInspection
import org.jetbrains.kotlin.idea.testIntegration.framework.KotlinPsiBasedTestFramework.Companion.asKtNamedFunction

class KtHighComplexityInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return KtInspVisitor(holder)
    }
}

class KtInspVisitor(val problemsHolder: ProblemsHolder) : PsiElementVisitor() {
    private val languageInfoProvider = PLUGIN_EP_NAME.findFirstSafe { it.language == KotlinLanguage.INSTANCE }!!

    override fun visitElement(element: PsiElement) {
        if (languageInfoProvider.isClassMember(element)) {
            val sink = element.obtainElementComplexity()
            if (sink.getConfiguredLevel() == ComplexityLevel.HARD) {
                val namedElement = element.asKtNamedFunction()
                val problemRef = namedElement?.let { "'${namedElement.name}()'" } ?: ""
                problemsHolder.registerProblem(namedElement?.nameIdentifier ?: element,
                                               "fun $problemRef is very complex")
            }
        }
    }
}
