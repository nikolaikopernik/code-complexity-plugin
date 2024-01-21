package com.github.nikolaikopernik.codecomplexity

import com.github.nikolaikopernik.codecomplexity.core.ComplexitySink
import com.github.nikolaikopernik.codecomplexity.core.ElementVisitor
import com.github.nikolaikopernik.codecomplexity.kotlin.KtComplexityInfoProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.childrenOfType
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.junit.Test

private const val KOTLIN_TEST_FILES_PATH = "src/test/testData/kotlin"

class KotlinComplexityCalculationTest : BaseComplexityTest() {
    @Test
    fun testKotlinFiles() {
        testAllFiles(KOTLIN_TEST_FILES_PATH, ".kt")
    }

    override fun getTestDataPath() = KOTLIN_TEST_FILES_PATH

    override fun createLanguageElementVisitor(sink: ComplexitySink): ElementVisitor =
        KtComplexityInfoProvider().getVisitor(sink)

    override fun parseTestFile(file: PsiFile): List<Triple<PsiElement, String, Int>> {
        val methods = requireNotNull(file.childrenOfType<KtNamedFunction>())

        return methods.map { method ->
            Triple(first = method,
                   second = method.name!!,
                   third = method.annotationEntries.firstOrNull { it.shortName.toString() == "Complexity" }
                   !!.valueArguments.first().getArgumentExpression()!!.text.toInt()
            )
        }
    }
}
