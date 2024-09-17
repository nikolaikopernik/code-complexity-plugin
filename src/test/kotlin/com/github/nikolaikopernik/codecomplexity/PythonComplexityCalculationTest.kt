package com.github.nikolaikopernik.codecomplexity

import com.github.nikolaikopernik.codecomplexity.core.ComplexitySink
import com.github.nikolaikopernik.codecomplexity.core.ElementVisitor
import com.github.nikolaikopernik.codecomplexity.python.PythonComplexityInfoProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.jetbrains.python.psi.PyFunction
import org.jetbrains.kotlin.psi.psiUtil.getChildrenOfType
import org.junit.Test

private const val PYTHON_TEST_FILES_PATH = "src/test/testData/python"

class PythonComplexityCalculationTest : BaseComplexityTest() {
    @Test
    fun testPythonFiles() {
        checkAllFilesInFolder(PYTHON_TEST_FILES_PATH, ".py")
    }

    override fun getTestDataPath() = PYTHON_TEST_FILES_PATH

    override fun createLanguageElementVisitor(sink: ComplexitySink): ElementVisitor = PythonComplexityInfoProvider().getVisitor(sink)

    override fun parseTestFile(file: PsiFile): List<Triple<PsiElement, String, Int>> {
        val methods: List<PyFunction> = requireNotNull(file.getChildrenOfType<PyFunction>()).toList()

        return methods.map { method ->
            Triple(first = method,
                   second = method.name!!,
                   third = requireNotNull(method.decoratorList?.findDecorator("complexity"))
                       .argumentList!!.arguments.first().text.toInt()
            )
        }
    }
}
