package com.github.nikolaikopernik.codecomplexity

import com.github.nikolaikopernik.codecomplexity.core.HighCodeComplexityInspection
import com.github.nikolaikopernik.codecomplexity.ui.obtainElementComplexity
import com.intellij.codeInspection.InspectionManager
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiMethod
import java.io.File
import kotlin.system.measureNanoTime

/**
 * Baseline timings for issue #30. Advisory only: nothing here asserts, because CI runners are too
 * noisy for a timing threshold and there is nowhere to keep a baseline. The gates live in
 * [ComplexityCachingTest], which counts recomputations instead of timing them.
 *
 * Excluded from `test` by a `*Benchmark` name filter; run with `./gradlew test -Pbenchmarks`.
 *
 * Hand-rolled rather than using the platform's `Benchmark` API on purpose. That harness starts a JFR
 * recording per attempt, roughly 30 ms of floor against work that takes 0.4 ms warm: measured
 * side by side it reported a cold/warm ratio of 1.2x where the real one is 63x. Its one attraction,
 * `runAsStressTest()`, only sets the deprecated `ApplicationManagerEx.setInStressTest`.
 *
 * So these run in ordinary test mode with the platform's debug assertions left on, which makes every
 * number an upper bound. That is the right trade for tracking a delta, since before and after are
 * measured identically.
 *
 * Scope note: this times the complexity computation, not a full inlay-hints pass. The platform's
 * traversal cannot be reproduced from a test, so method discovery stays in setup, outside the timing.
 */
class ComplexityBenchmark : BaseCorpusTest() {

    fun testComplexityBaselineBenchmark() {
        var methods: Collection<PsiMethod> = configureCorpus(CORPUS_METHODS).values
        val manager = InspectionManager.getInstance(project)
        val inspection = HighCodeComplexityInspection()

        val rows = listOf(
            // What one keystroke costs today: every score rebuilt from scratch.
            measure("cold-cache recompute", setup = {
                invalidateAllScores()
                methods = findMethodsByName().values
            }) { methods.forEach { it.obtainElementComplexity() } },

            // The floor a finer cache dependency is aiming at: same walk, all hits.
            measure("warm-cache lookup") { methods.forEach { it.obtainElementComplexity() } },

            // Batch Inspect Code, the path opted-in users pay (the inspection ships
            // enabledByDefault="false"). Each file is visited once, so nothing damps the
            // visitors' per-node cost; this is the number that would justify optimising it.
            measure("batch inspection, cold", setup = ::invalidateAllScores) {
                inspection.checkFile(myFixture.file, manager, false)
            },
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

    /**
     * Types a space, which the cache treats as a whole-file change (see [ComplexityCachingTest]), so
     * the next attempt starts cold. A space rather than a statement keeps the corpus shape stable.
     */
    private fun invalidateAllScores() {
        myFixture.type(" ")
        PsiDocumentManager.getInstance(project).commitAllDocuments()
    }

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
        appendLine("Cold/warm ratio: **${"%.0f".format(cold.median / warm.median)}x**. That multiple is what")
        appendLine("a finer cache dependency stands to remove from every keystroke in a file this size.")
        appendLine()
        appendLine("Re-run with `./gradlew test -Pbenchmarks`. Machine-specific: compare only against")
        appendLine("numbers taken on the same machine in the same session.")
    }

    private fun ms(value: Double) = "${"%.2f".format(value)} ms"

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
