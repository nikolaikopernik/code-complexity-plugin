package com.github.nikolaikopernik.codecomplexity

import com.github.nikolaikopernik.codecomplexity.core.ComplexitySink
import com.github.nikolaikopernik.codecomplexity.java.JavaLanguageInfoProvider
import com.github.nikolaikopernik.codecomplexity.python.PythonLanguageInfoProvider
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.intellij.psi.util.childrenOfType
import com.intellij.testFramework.LightPlatformCodeInsightTestCase
import com.jetbrains.python.psi.PyDecorator
import com.jetbrains.python.psi.PyFunction
import org.jetbrains.kotlin.idea.completion.argList
import org.jetbrains.kotlin.psi.psiUtil.allChildren
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import org.jetbrains.kotlin.psi.psiUtil.getChildrenOfType
import org.junit.Test
import java.io.File

private const val PYTHON_TEST_FILES_PATH = "src/test/testData/python"

class PythonComplexityCalculationTest : LightPlatformCodeInsightTestCase() {
    /**
     * The actual tests are in the files in [PYTHON_TEST_FILES_PATH]
     * The expected complexity is annotated with @Complexity
     */
    @Test
    fun testPythonFiles() {
        val tests = File(PYTHON_TEST_FILES_PATH).listFiles().filter { it.name.endsWith(".py") }

        tests.map { "/${it.name}" }.forEach {
            it.testAllMethodsInFile()
        }
    }

    override fun getTestDataPath() = PYTHON_TEST_FILES_PATH

    override fun getTestName(lowercaseFirstLetter: Boolean): String {
        return super.getTestName(lowercaseFirstLetter).trim().replace(' ', '_')
    }

    private fun String.testAllMethodsInFile() {
        println()

        configureByFile(this)
        val getVisitor = PythonLanguageInfoProvider()::getVisitor
        val methods: Array<PyFunction> = requireNotNull(file.getChildrenOfType<PyFunction>())

        methods.forEach { method ->
            print("Checking method '${method.name}()' in file ${this.drop(1)} file... ")
            val annotation: PyDecorator = requireNotNull(
                method.decoratorList?.findDecorator("complexity")
            )
            val expectedComplexity: Int = annotation.argumentList!!.arguments.first().text.toInt()
            val sink = ComplexitySink().apply { method.accept(getVisitor(this)) }
            assertEquals("Incorrect complexity calculated for method '${method.name}()' in the file ${this.drop(1)}.",
                         expectedComplexity,
                         sink.getComplexity()
            )
            assertEquals("Incorrect nesting after processing method '${method.name}()' in the file ${this.drop(1)}.",
                         0,
                         sink.getNesting()
            )
            println("OK")
        }
    }
}
