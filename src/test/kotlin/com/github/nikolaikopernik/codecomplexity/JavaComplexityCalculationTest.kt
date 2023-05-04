package com.github.nikolaikopernik.codecomplexity

import com.github.nikolaikopernik.codecomplexity.core.ComplexitySink
import com.github.nikolaikopernik.codecomplexity.java.JavaLanguageInfoProvider
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.intellij.psi.util.childrenOfType
import com.intellij.testFramework.LightPlatformCodeInsightTestCase
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import org.junit.Test
import java.io.File

private const val JAVA_TEST_FILES_PATH = "src/test/testData/java"

class JavaComplexityCalculationTest : LightPlatformCodeInsightTestCase() {
    /**
     * The actual tests are in the files in [JAVA_TEST_FILES_PATH]
     * The expected complexity is annotated with @Complexity
     */
    @Test
    fun testJavaFiles() {
        val tests = File(JAVA_TEST_FILES_PATH).listFiles().filter { it.name.endsWith(".java") }!!

        tests.map { "/${it.name}" }.forEach {
            it.testAllMethodsInFile()
        }
    }

    override fun getTestDataPath() = JAVA_TEST_FILES_PATH

    override fun getTestName(lowercaseFirstLetter: Boolean): String {
        return super.getTestName(lowercaseFirstLetter).trim().replace(' ', '_')
    }

    private fun String.testAllMethodsInFile() {
        println()

        configureByFile(this)
        val getVisitor = JavaLanguageInfoProvider()::getVisitor
        val methods = requireNotNull(file.getChildOfType<PsiClass>()!!.childrenOfType<PsiMethod>())

        methods.forEach { method ->
            print("Checking method '${method.name}()' in file ${this.drop(1)} file... ")
            val annotation = requireNotNull(
                method.getAnnotation("Complexity") //.firstOrNull { it.qualifiedName.toString() == "Complexity" }
            )
            val expectedComplexity: Int = annotation.parameterList.attributes.first().text.toInt()
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
