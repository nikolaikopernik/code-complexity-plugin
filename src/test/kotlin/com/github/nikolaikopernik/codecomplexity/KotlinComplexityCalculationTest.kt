package com.github.nikolaikopernik.codecomplexity

import com.github.nikolaikopernik.codecomplexity.core.ComplexitySink
import com.github.nikolaikopernik.codecomplexity.kotlin.KtLanguageInfoProvider
import com.intellij.psi.util.childrenOfType
import com.intellij.testFramework.LightPlatformCodeInsightTestCase
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.junit.Test
import java.io.File

private const val KOTLIN_TEST_FILES_PATH = "src/test/testData/kotlin"

class KotlinComplexityCalculationTest : LightPlatformCodeInsightTestCase() {
    /**
     * The actual tests are in the files in [KOTLIN_TEST_FILES_PATH]
     * The expected complexity is annotated with @Complexity
     */
    @Test
    fun testKotlinFiles() {
        val tests = File(KOTLIN_TEST_FILES_PATH).listFiles().filter { it.name.endsWith(".kt") }!!

        tests.map { "/${it.name}" }.forEach {
            it.testAllMethodsInFile()
        }
    }

    override fun getTestDataPath() = KOTLIN_TEST_FILES_PATH

    override fun getTestName(lowercaseFirstLetter: Boolean): String {
        return super.getTestName(lowercaseFirstLetter).trim().replace(' ', '_')
    }

    private fun String.testAllMethodsInFile() {
        println()
        configureByFile(this)
        val getVisitor = KtLanguageInfoProvider()::getVisitor
        val methods = requireNotNull(file.childrenOfType<KtNamedFunction>())

        methods.forEach { method ->
            print("Checking method '${method.name}()' in file ${this.drop(1)} file... ")
            val annotation = requireNotNull(
                method.annotationEntries.firstOrNull { it.shortName.toString() == "Complexity" }
            )
            val expectedComplexity: Int = annotation.valueArguments.first().getArgumentExpression()!!.text.toInt()
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
