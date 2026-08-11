package com.github.nikolaikopernik.codecomplexity

import com.github.nikolaikopernik.codecomplexity.core.PointType
import com.github.nikolaikopernik.codecomplexity.java.JavaComplexityInfoProvider
import com.github.nikolaikopernik.codecomplexity.ui.ComplexityFactoryInlayHintsCollector
import com.intellij.psi.PsiClass
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * Characterizes class-level aggregation (the inlay collector's METHOD-point path):
 * one point per member at nesting 0, member scores summed, nested members not double-counted.
 */
class ClassComplexityAggregationTest : BasePlatformTestCase() {

    fun testClassComplexitySumsMembersAsMethodPoints() {
        myFixture.configureByText("Sums.java", """
            class Sums {
                void a(boolean x) { if (x) { if (x) { } } }

                void b(boolean x) { if (x) { } }

                void empty() { }

                Runnable r() {
                    return new Runnable() {
                        public void run() { if (true) { } }
                    };
                }
            }
        """.trimIndent())
        val psiClass = PsiTreeUtil.findChildOfType(myFixture.file, PsiClass::class.java)!!
        val collector = ComplexityFactoryInlayHintsCollector(JavaComplexityInfoProvider(), myFixture.editor)

        val sink = collector.getClassComplexity(psiClass)

        // a=3 (nested ifs), b=1, empty=0, r=1 (run()'s if folds into r, not a separate point)
        assertEquals(listOf(3, 1, 0, 1), sink.getPoints().map { it.complexity })
        assertTrue(sink.getPoints().all { it.type == PointType.METHOD && it.nesting == 0 })
        assertEquals(5, sink.getComplexity())
    }
}
