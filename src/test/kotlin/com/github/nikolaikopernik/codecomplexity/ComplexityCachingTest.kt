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
 * [obtainElementComplexity] is fixed: it hashes the member's text, so a keystroke now recomputes only
 * the member that changed. `getClassComplexity` still has no caching at all, so every collector pass
 * re-walks the class, and that number stays pinned at today's behaviour.
 *
 * `InlayHintsPass` collects the Divider's inside *and* outside lists, so whatever remains uncached is
 * paid for the whole file on every pass, not just for the visible part.
 */
class ComplexityCachingTest : BaseCorpusTest() {

    fun testCacheIsReusedWhenTheFileIsUntouched() {
        val methods = configureCorpus(METHOD_COUNT)
        val before = methods.mapValues { (_, method) -> method.obtainElementComplexity() }

        val after = methods.mapValues { (_, method) -> method.obtainElementComplexity() }

        assertEquals("an untouched file must reuse every cached sink, else the cache is dead",
                     METHOD_COUNT, after.count { (name, sink) -> sink === before[name] })
    }

    fun testOneKeystrokeOnlyRecomputesTheEditedMethod() {
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

        // Only the edited method's text changed, so only its score may be rebuilt. This was
        // METHOD_COUNT before the text hash landed, which is what made issue #30 bite. Never
        // relax it: a larger number means invalidation went coarse again.
        assertEquals("blast radius of a single keystroke, in methods recomputed",
                     1, sinksAfter.count { (name, sink) -> sink !== sinksBefore[name] })
        assertTrue("the recomputed method must be the edited one",
                   sinksAfter.entries.single { (name, sink) -> sink !== sinksBefore[name] }
                       .key == methodsBefore.keys.last())
    }

    /**
     * The risk the text hash introduces: if it missed a change, the edited method would keep serving
     * its old score for good. So assert the new value, not just that something was recomputed.
     */
    fun testEditedMethodPicksUpItsNewScore() {
        val methodsBefore = configureCorpus(METHOD_COUNT)
        val editedName = methodsBefore.keys.last()
        val scoresBefore = methodsBefore.mapValues { (_, m) -> m.obtainElementComplexity().getComplexity() }

        // One more top-level `if` in the last method, worth exactly one point at nesting 0.
        myFixture.type("if (a > 0) { }")
        PsiDocumentManager.getInstance(project).commitAllDocuments()

        val scoresAfter = findMethodsByName().mapValues { (_, m) -> m.obtainElementComplexity().getComplexity() }

        assertEquals("the edited method must score one higher",
                     scoresBefore.getValue(editedName) + 1, scoresAfter.getValue(editedName))
        assertEquals("every untouched method must keep the score it had",
                     scoresBefore - editedName, scoresAfter - editedName)
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
