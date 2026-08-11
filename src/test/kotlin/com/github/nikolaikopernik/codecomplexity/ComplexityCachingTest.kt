package com.github.nikolaikopernik.codecomplexity

import com.github.nikolaikopernik.codecomplexity.java.JavaComplexityInfoProvider
import com.github.nikolaikopernik.codecomplexity.ui.ComplexityFactoryInlayHintsCollector
import com.github.nikolaikopernik.codecomplexity.ui.obtainElementComplexity
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.util.PsiTreeUtil

/**
 * Guards the complexity caches against regressing back to file-level invalidation (issue #30:
 * editing a large file made IDEA lag).
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

        // Confirms any miss below is ours, not the incremental reparser's.
        assertEquals("a one-statement edit in one body must not replace any PsiMethod",
                     METHOD_COUNT, methodsAfter.count { (name, method) -> method === methodsBefore[name] })

        // Was METHOD_COUNT before the text hash landed (issue #30). Never relax this back up.
        assertEquals("blast radius of a single keystroke, in methods recomputed",
                     1, sinksAfter.count { (name, sink) -> sink !== sinksBefore[name] })
        assertTrue("the recomputed method must be the edited one",
                   sinksAfter.entries.single { (name, sink) -> sink !== sinksBefore[name] }
                       .key == methodsBefore.keys.last())
    }

    /** Guards against the hash missing a real change and serving a stale score forever. */
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

    fun testClassWalkIsReusedWhileTheFileIsUnchanged() {
        val methods = configureCorpus(METHOD_COUNT)
        val collector = collector()
        val psiClass = findClass()

        val first = collector.getClassComplexity(psiClass)
        val membersAfterFirst = methods.mapValues { (_, method) -> method.obtainElementComplexity() }
        val second = collector.getClassComplexity(psiClass)
        val membersAfterSecond = methods.mapValues { (_, method) -> method.obtainElementComplexity() }

        assertTrue("class score must be above zero", second.getComplexity() > 0)
        assertEquals("every member must contribute one point, so the walk covers the whole class",
                     METHOD_COUNT, second.getPoints().size)

        // The member cache keeps a walk cheap even when one does happen. Losing this would
        // multiply the cost of every pass by the member count.
        assertEquals("a class walk must not recompute any member whose text is unchanged",
                     METHOD_COUNT, membersAfterSecond.count { (name, sink) -> sink === membersAfterFirst[name] })

        // Every collector pass used to re-walk the whole class. Daemon restarts make that happen on
        // files nobody edited, so the repeat is now free. Never loosen this back to assertNotSame.
        assertSame("an unchanged file must reuse the class-level sink", first, second)
    }

    /** The class score is a sum over members, so it has to move when a member's does. */
    fun testClassScoreUpdatesAfterAnEdit() {
        configureCorpus(METHOD_COUNT)
        val collector = collector()
        val before = collector.getClassComplexity(findClass()).getComplexity()

        myFixture.type("if (a > 0) { }")
        PsiDocumentManager.getInstance(project).commitAllDocuments()

        assertEquals("one more point in one member must show up in the class total",
                     before + 1, collector.getClassComplexity(findClass()).getComplexity())
    }

    private fun collector() =
        ComplexityFactoryInlayHintsCollector(JavaComplexityInfoProvider(), myFixture.editor)

    private fun findClass() = PsiTreeUtil.findChildOfType(myFixture.file, PsiClass::class.java)!!

    private companion object {
        /** Small enough to stay fast, large enough that whole-file and one-method can't be confused. */
        const val METHOD_COUNT = 40
    }
}
