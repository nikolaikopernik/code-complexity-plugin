package com.github.nikolaikopernik.codecomplexity.core

import com.github.nikolaikopernik.codecomplexity.settings.ComplexityLevel
import com.github.nikolaikopernik.codecomplexity.settings.getConfiguredLevel
import com.github.nikolaikopernik.codecomplexity.ui.obtainElementComplexity
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import com.intellij.util.SlowOperations
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.namedFunctionVisitor

class KtHighComplexityInspection : LocalInspectionTool() {
    private val languageInfoProvider = PLUGIN_EP_NAME.findFirstSafe { it.language == KotlinLanguage.INSTANCE }!!

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        holder.file.language
        return namedFunctionVisitor {
            SlowOperations.allowSlowOperations<RuntimeException> {
                val sink = it.obtainElementComplexity()
                if (sink.getConfiguredLevel() == ComplexityLevel.HARD) {
                    val problemRef = "'${it.name}()'"
                    holder.registerProblem(it.nameIdentifier ?: it, "fun $problemRef is very complex")
                }
            }
        }
    }
}
