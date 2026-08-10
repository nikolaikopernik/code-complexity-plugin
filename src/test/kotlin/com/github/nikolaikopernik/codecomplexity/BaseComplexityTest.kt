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

        val tests = folder.listFiles().orEmpty().filter { it.name.endsWith(extension) }.sorted()
        assertFalse("No *$extension files in ${folder.absolutePath}", tests.isEmpty())

        val failures = mutableListOf<String>()
        val checked = tests.sumOf { "/${it.name}".checkAllMethodsInFile(failures) }
        if (checked != expectedMethodCount) {
            failures += "Checked-method count for $path drifted: expected $expectedMethodCount, was $checked. " +
                "Update expectedMethodCount if the test data changed, otherwise methods are being silently skipped."
        }
        if (failures.isNotEmpty()) {
            fail("${failures.size} failure(s) in $path:\n" + failures.joinToString("\n"))
        }
    }

    override fun getTestName(lowercaseFirstLetter: Boolean): String {
        return super.getTestName(lowercaseFirstLetter).trim().replace(' ', '_')
    }

    /**
     * Checks every parsed method in the file, appending mismatches to [failures] instead of
     * failing fast, so one run reports them all. Returns the number of methods checked.
     */
    private fun String.checkAllMethodsInFile(failures: MutableList<String>): Int {
        configureByFile(this)
        val methods = parseTestFile(file)
        val fileName = this.drop(1)
        assertFalse("No annotated methods parsed from $fileName", methods.isEmpty())

        methods.forEach { (element, name, expected) ->
            val sink = ComplexitySink().apply { element.accept(createLanguageElementVisitor(this)) }
            if (sink.getComplexity() != expected) {
                failures += "$fileName#$name(): complexity expected $expected, got ${sink.getComplexity()}. Points:\n" +
                    sink.getPoints().joinToString("\n") { "    $it" }.ifEmpty { "    (none)" }
            }
            if (sink.getNesting() != 0) {
                failures += "$fileName#$name(): nesting expected 0, got ${sink.getNesting()}"
            }
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
