package com.github.nikolaikopernik.codecomplexity

import com.github.nikolaikopernik.codecomplexity.core.ComplexitySink
import com.github.nikolaikopernik.codecomplexity.java.JavaLanguageVisitor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * Generates the Java corpus shared by the caching tests and benchmarks: one class of
 * uniformly-shaped, uniquely-named methods, caret in the last one so edits shift no earlier offsets.
 */
abstract class BaseCorpusTest : BasePlatformTestCase() {

    /** Configures a corpus of [methodCount] methods, returned by name so before/after can line up. */
    protected fun configureCorpus(methodCount: Int): Map<String, PsiMethod> {
        myFixture.configureByText("Big.java", generateClass(methodCount))

        val errors = PsiTreeUtil.findChildrenOfType(myFixture.file, PsiErrorElement::class.java)
        assertTrue("generated corpus must parse cleanly: ${errors.map { it.errorDescription }}", errors.isEmpty())

        val methods = findMethodsByName()
        assertEquals("generated methods must all be found and uniquely named", methodCount, methods.size)

        // A degenerate corpus would let a caller's assertions pass without measuring anything.
        // Scored through the visitor directly, so this guard stays independent of the cache under
        // test and leaves it cold.
        val scores = methods.values.map { visitorScoreOf(it) }.distinct()
        assertEquals("generated methods must all score alike, got $scores", 1, scores.size)
        assertTrue("generated methods must score above zero", scores.single() > 0)

        return methods
    }

    protected fun findMethodsByName(): Map<String, PsiMethod> =
        PsiTreeUtil.findChildrenOfType(myFixture.file, PsiMethod::class.java).associateBy { it.name }

    /** Uncached score, straight through the visitor. */
    protected fun visitorScoreOf(element: PsiElement): Int =
        ComplexitySink().also { element.accept(JavaLanguageVisitor(it)) }.getComplexity()

    private fun generateClass(methodCount: Int): String = buildString {
        appendLine("class Big {")
        (1..methodCount).forEach { i ->
            appendLine("    int ${"method%03d".format(i)}(int a, int b) {")
            appendLine("        if (a > b && b > 0) {")
            appendLine("            for (int i = 0; i < a; i++) {")
            appendLine("                a += i;")
            appendLine("            }")
            appendLine("        }")
            if (i == methodCount) appendLine("        <caret>")
            appendLine("        return a;")
            appendLine("    }")
        }
        appendLine("}")
    }
}
