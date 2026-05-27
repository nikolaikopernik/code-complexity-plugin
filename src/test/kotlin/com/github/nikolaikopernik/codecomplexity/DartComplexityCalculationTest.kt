package com.github.nikolaikopernik.codecomplexity

import com.github.nikolaikopernik.codecomplexity.core.ComplexitySink
import com.github.nikolaikopernik.codecomplexity.core.ElementVisitor
import com.github.nikolaikopernik.codecomplexity.dart.DartComplexityInfoProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

private const val DART_TEST_FILES_PATH = "src/test/testData/dart"

class DartComplexityCalculationTest : BaseComplexityTest() {
    // TODO: enable once DartLanguageVisitor is implemented and testData/dart/ has fixtures.
    // @Test fun testDartFiles() = checkAllFilesInFolder(DART_TEST_FILES_PATH, ".dart")

    override fun getTestDataPath() = DART_TEST_FILES_PATH

    override fun createLanguageElementVisitor(sink: ComplexitySink): ElementVisitor =
        DartComplexityInfoProvider().getVisitor(sink)

    override fun parseTestFile(file: PsiFile): List<Triple<PsiElement, String, Int>> =
        TODO("Extract Dart methods and their expected @complexity(N) annotation values")
}
