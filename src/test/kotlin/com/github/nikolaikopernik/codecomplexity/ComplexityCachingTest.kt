package com.github.nikolaikopernik.codecomplexity

import com.github.nikolaikopernik.codecomplexity.java.JavaComplexityInfoProvider
import com.github.nikolaikopernik.codecomplexity.ui.ComplexityFactoryInlayHintsCollector
import com.github.nikolaikopernik.codecomplexity.ui.obtainElementComplexity
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.util.PsiTreeUtil

/**
 * Characterizes what the complexity caches do and do not absorb, which is the measured cause of
 * issue #30 ("editing a very large file consumes a lot of CPU and makes IDEA lag").
 *
 * Two separate problems, both pinned at today's behaviour rather than the behaviour we want:
 *  - [obtainElementComplexity] declares its dependency as `Result.create(sink, this)`, and a bare
 *    PsiElement resolves to file-level granularity, so one keystroke drops every method's score.
 *  - `getClassComplexity` has no caching at all, so every collector pass re-walks the class.
 *
 * `InlayHintsPass` collects the Divider's inside *and* outside lists, so both costs are paid for
 * the whole file on every pass, not just for the visible part.
 */
class ComplexityCachingTest : BaseCorpusTest() {

    fun testCacheIsReusedWhenTheFileIsUntouched() {
        val methods = configureCorpus(METHOD_COUNT)
        val before = methods.mapValues { (_, method) -> method.obtainElementComplexity() }

        val after = methods.mapValues { (_, method) -> method.obtainElementComplexity() }

        assertEquals("an untouched file must reuse every cached sink, else the cache is dead",
                     METHOD_COUNT, after.count { (name, sink) -> sink === before[name] })
    }

    fun testOneKeystrokeDropsEveryCachedScoreInTheFile() {
        val methodsBefore = configureCorpus(METHOD_COUNT)
        val sinksBefore = methodsBefore.mapValues { (_, method) -> method.obtainElementComplexity() }

        // The caret sits in the last method, so this shifts no earlier offsets.
        myFixture.type("a++;")
        PsiDocumentManager.getInstance(project).commitAllDocuments()

        val methodsAfter = findMethodsByName()
        val sinksAfter = methodsAfter.mapValues { (_, method) -> method.obtainElementComplexity() }

        // The incremental reparse keeps every PsiMethod instance, so no miss below is the
        // platform's doing. If this ever drops, the corpus or the reparser changed, and the
        // recompute count stops being attributable to us.
        assertEquals("a one-statement edit in one body must not replace any PsiMethod",
                     METHOD_COUNT, methodsAfter.count { (name, method) -> method === methodsBefore[name] })

        // Only the edited method genuinely needs recomputing, so the target here is 1 and the
        // other METHOD_COUNT - 1 are issue #30. Tighten this number when the cache dependency
        // gets finer; never relax it.
        assertEquals("blast radius of a single keystroke, in methods recomputed",
                     METHOD_COUNT, sinksAfter.count { (name, sink) -> sink !== sinksBefore[name] })
    }

    fun testClassWalkRepeatsWhileItsMembersStayCached() {
        val methods = configureCorpus(METHOD_COUNT)
        val psiClass = PsiTreeUtil.findChildOfType(myFixture.file, PsiClass::class.java)!!
        val collector = ComplexityFactoryInlayHintsCollector(JavaComplexityInfoProvider(), myFixture.editor)

        val first = collector.getClassComplexity(psiClass)
        val membersAfterFirst = methods.mapValues { (_, method) -> method.obtainElementComplexity() }
        val second = collector.getClassComplexity(psiClass)
        val membersAfterSecond = methods.mapValues { (_, method) -> method.obtainElementComplexity() }

        // Repeated walks must agree, whether or not the walk ever gets cached.
        assertEquals("repeated class walks must produce the same score", first.getComplexity(), second.getComplexity())
        assertTrue("class score must be above zero", second.getComplexity() > 0)
        assertEquals("every member must contribute one point, so the walk covers the whole class",
                     METHOD_COUNT, second.getPoints().size)

        // The member cache is what keeps the repeat cheap: the second walk re-visits the tree but
        // must not recompute any member. Losing this would multiply the cost of every pass.
        assertEquals("a repeated class walk must not recompute any member",
                     METHOD_COUNT, membersAfterSecond.count { (name, sink) -> sink === membersAfterFirst[name] })

        // The walk itself is thrown away every time, so each collector pass pays the traversal of
        // the entire class. Target is one shared instance; flip this to assertSame once cached.
        assertNotSame("class-level complexity is recomputed rather than cached", first, second)
    }

    private companion object {
        /** Small enough to stay fast, large enough that whole-file and one-method can't be confused. */
        const val METHOD_COUNT = 40
    }
}
