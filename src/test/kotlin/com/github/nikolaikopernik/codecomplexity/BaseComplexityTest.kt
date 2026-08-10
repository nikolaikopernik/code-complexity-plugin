package com.github.nikolaikopernik.codecomplexity

import com.github.nikolaikopernik.codecomplexity.core.ComplexitySink
import com.github.nikolaikopernik.codecomplexity.core.ElementVisitor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.testFramework.LightPlatformCodeInsightTestCase
import java.io.File

abstract class BaseComplexityTest : LightPlatformCodeInsightTestCase() {
    fun checkAllFilesInFolder(path: String, extension: String, expectedMethodCount: Int) {
        val folder = File(path)
        assertTrue("Test data folder not found: ${folder.absolutePath}", folder.isDirectory)

        val tests = folder.listFiles().orEmpty().filter { it.name.endsWith(extension) }
        assertFalse("No *$extension files in ${folder.absolutePath}", tests.isEmpty())

        val checked = tests.sumOf { "/${it.name}".testAllMethodsInFile() }
        assertEquals("Checked-method count for $path drifted: update expectedMethodCount if the " +
                         "test data changed, otherwise methods are being silently skipped.",
                     expectedMethodCount,
                     checked)
    }

    override fun getTestName(lowercaseFirstLetter: Boolean): String {
        return super.getTestName(lowercaseFirstLetter).trim().replace(' ', '_')
    }

    private fun String.testAllMethodsInFile(): Int {
        println()
        configureByFile(this)
        val methods = parseTestFile(file)
        assertFalse("No annotated methods parsed from ${this.drop(1)}", methods.isEmpty())

        methods.forEach { (element, name, complexity) ->
            print("Checking method '$name()' in file ${this.drop(1)} file... ")
            val sink = ComplexitySink().apply { element.accept(createLanguageElementVisitor(this)) }
            assertEquals("Incorrect complexity calculated for method '$name()' in the file ${this.drop(1)}.\n" +
                             "The following points have been seen by the sink: \n" + sink.getPoints()
                .joinToString { it.toString() + '\n' },
                         complexity,
                         sink.getComplexity()
            )
            assertEquals("Incorrect nesting after processing method '$name()' in the file ${this.drop(1)}.",
                         0,
                         sink.getNesting()
            )
            println("OK")
        }
        return methods.size
    }

    abstract fun createLanguageElementVisitor(sink: ComplexitySink): ElementVisitor

    /**
     * Parse the test file into list of methods.
     * Each method has an element to analyse, name and complexity
     */
    abstract fun parseTestFile(file: PsiFile): List<Triple<PsiElement, String, Int>>
}
