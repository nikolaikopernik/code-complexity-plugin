package com.github.nikolaikopernik.codecomplexity

import com.github.nikolaikopernik.codecomplexity.core.HighCodeComplexityInspection
import com.github.nikolaikopernik.codecomplexity.java.JavaComplexityInfoProvider
import com.github.nikolaikopernik.codecomplexity.ui.ComplexityFactoryInlayHintsCollector
import com.github.nikolaikopernik.codecomplexity.ui.obtainElementComplexity
import com.intellij.codeInspection.InspectionManager
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiMethod
import com.intellij.psi.util.PsiTreeUtil
import java.io.File
import kotlin.system.measureNanoTime

/**
 * Advisory timings for issue #30; nothing here asserts. Gates live in [ComplexityCachingTest].
 * Excluded from `test` by a `*Benchmark` filter; run with `./gradlew test -Pbenchmarks`.
 *
 * Hand-rolled rather than the platform's `Benchmark` API: that harness's per-attempt JFR recording
 * (~30 ms floor) swamped work that takes 0.4 ms warm, reporting a 1.2x ratio where the real one is
 * 63x. Times the complexity computation only, not a full inlay-hints pass.
 */
class ComplexityBenchmark : BaseCorpusTest() {

    fun testComplexityBaselineBenchmark() {
        var methods: Collection<PsiMethod> = configureCorpus(CORPUS_METHODS).values
        val manager = InspectionManager.getInstance(project)
        val inspection = HighCodeComplexityInspection()
        val collector = ComplexityFactoryInlayHintsCollector(JavaComplexityInfoProvider(), myFixture.editor)
        var psiClass = findClass()

        val rows = listOf(
            // What one keystroke costs today: every score rebuilt from scratch.
            measure("cold-cache recompute", setup = {
                invalidateAllScores()
                methods = findMethodsByName().values
            }) { methods.forEach { it.obtainElementComplexity() } },

            // The floor a finer cache dependency is aiming at: same walk, all hits.
            measure("warm-cache lookup") { methods.forEach { it.obtainElementComplexity() } },

            // NOT batch Inspect Code: setup invalidates one member, the rest answer from cache.
            measure("inspection after one edit", setup = ::invalidateAllScores) {
                inspection.checkFile(myFixture.file, manager, false)
            },

            // The class-level aggregate. Repeated collector passes happen on an unchanged file
            // whenever the daemon restarts, so this is the one worth making free.
            measure("class walk, unchanged file") { collector.getClassComplexity(psiClass) },

            measure("class walk, after one edit", setup = {
                invalidateAllScores()
                psiClass = findClass()
            }) { collector.getClassComplexity(psiClass) },
        )

        report(rows).let { text ->
            println(text)
            runCatching {
                File(System.getProperty("user.home"), "Desktop/CCP").mkdirs()
                File(System.getProperty("user.home"), "Desktop/CCP/benchmark-baseline.md").writeText(text)
            }
        }
    }

    private fun measure(name: String, setup: () -> Unit = {}, work: () -> Unit): Row {
        val samples = (1..WARMUPS + ATTEMPTS).map {
            setup()
            measureNanoTime { work() } / 1_000_000.0
        }
        return Row(name, samples.drop(WARMUPS).sorted())
    }

    /** A space, not a statement, so it invalidates without changing the corpus's scores. */
    private fun invalidateAllScores() {
        myFixture.type(" ")
        PsiDocumentManager.getInstance(project).commitAllDocuments()
    }

    private fun findClass(): PsiClass = PsiTreeUtil.findChildOfType(myFixture.file, PsiClass::class.java)!!

    private fun report(rows: List<Row>): String = buildString {
        val cold = rows.first { it.name.startsWith("cold") }
        val warm = rows.first { it.name.startsWith("warm") }

        appendLine()
        appendLine("## Complexity baseline (issue #30)")
        appendLine()
        appendLine("$CORPUS_METHODS methods, ${myFixture.file.text.lines().size} lines. " +
                       "$WARMUPS warmup + $ATTEMPTS timed attempts, warmups discarded.")
        appendLine("Upper bounds: test mode leaves the platform's debug assertions on.")
        appendLine()
        appendLine("| measurement | median | min | max |")
        appendLine("|:--|--:|--:|--:|")
        rows.forEach {
            appendLine("| ${it.name} | ${ms(it.median)} | ${ms(it.min)} | ${ms(it.max)} |")
        }
        appendLine()
        appendLine("Cold/warm ratio: **${"%.1f".format(cold.median / warm.median)}x**. Lower is better here:")
        appendLine("it says how much more a keystroke costs than doing nothing. It was 30x before the")
        appendLine("text-hash guard landed, because every member in the file was recomputed.")
        appendLine()
        appendLine("Re-run with `./gradlew test -Pbenchmarks`. Machine-specific: compare only against")
        appendLine("numbers taken on the same machine in the same session.")
    }

    // "0.00 ms" reads as a broken measurement rather than as free.
    private fun ms(value: Double) = if (value < 0.01) "<0.01 ms" else "${"%.2f".format(value)} ms"

    private class Row(val name: String, private val sorted: List<Double>) {
        val median get() = sorted[sorted.size / 2]
        val min get() = sorted.first()
        val max get() = sorted.last()
    }

    private companion object {
        /** Large enough that the cold number lands well clear of timer noise. */
        const val CORPUS_METHODS = 800
        const val WARMUPS = 3
        const val ATTEMPTS = 10
    }
}
