package com.github.nikolaikopernik.codecomplexity

import com.github.nikolaikopernik.codecomplexity.core.ComplexitySink
import com.github.nikolaikopernik.codecomplexity.core.ElementVisitor
import com.github.nikolaikopernik.codecomplexity.java.JavaLanguageInfoProvider
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMethod
import com.intellij.psi.util.childrenOfType
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import org.junit.Test

private const val JAVA_TEST_FILES_PATH = "src/test/testData/java"

class JavaComplexityCalculationTest : BaseComplexityTest() {
    @Test
    fun testJavaFiles() {
        testAllFiles(JAVA_TEST_FILES_PATH, ".java")
    }

    override fun getTestDataPath() = JAVA_TEST_FILES_PATH

    override fun createLanguageElementVisitor(sink: ComplexitySink): ElementVisitor =
        JavaLanguageInfoProvider().getVisitor(sink)

    override fun parseTestFile(file: PsiFile): List<Triple<PsiElement, String, Int>> {
        val methods = requireNotNull(file.getChildOfType<PsiClass>()!!.childrenOfType<PsiMethod>())

        return methods.map { method ->
            Triple(first = method,
                   second = method.name,
                   third = method.getAnnotation("Complexity")!!.parameterList.attributes.first().text.toInt()
            )
        }
    }
}
