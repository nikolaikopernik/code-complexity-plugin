package com.github.nikolaikopernik.codecomplexity

import com.github.nikolaikopernik.codecomplexity.ui.obtainElementComplexity
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * Generates the Java corpus shared by the caching tests and the benchmarks: one class of
 * uniformly-shaped, uniquely-named methods, with a caret in the last one so typing there shifts no
 * earlier offsets.
 *
 * Generated rather than committed, which keeps the size a parameter and leaves `src/test/testData`
 * matching the `inputs.dir` declaration in build.gradle.kts.
 */
abstract class BaseCorpusTest : BasePlatformTestCase() {

    /**
     * Configures a corpus of [methodCount] methods and returns them by name. Names survive a
     * reparse, so they are what lets a before/after comparison line up.
     */
    protected fun configureCorpus(methodCount: Int): Map<String, PsiMethod> {
        myFixture.configureByText("Big.java", generateClass(methodCount))

        val errors = PsiTreeUtil.findChildrenOfType(myFixture.file, PsiErrorElement::class.java)
        assertTrue("generated corpus must parse cleanly: ${errors.map { it.errorDescription }}", errors.isEmpty())

        val methods = findMethodsByName()
        assertEquals("generated methods must all be found and uniquely named", methodCount, methods.size)

        // A degenerate corpus would let a caller's assertions pass without measuring anything.
        val scores = methods.values.map { it.obtainElementComplexity().getComplexity() }.distinct()
        assertEquals("generated methods must all score alike, got $scores", 1, scores.size)
        assertTrue("generated methods must score above zero", scores.single() > 0)

        return methods
    }

    protected fun findMethodsByName(): Map<String, PsiMethod> =
        PsiTreeUtil.findChildrenOfType(myFixture.file, PsiMethod::class.java).associateBy { it.name }

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
