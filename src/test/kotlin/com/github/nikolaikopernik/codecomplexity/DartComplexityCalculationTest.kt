package com.github.nikolaikopernik.codecomplexity

import com.github.nikolaikopernik.codecomplexity.core.ComplexitySink
import com.github.nikolaikopernik.codecomplexity.core.ElementVisitor
import com.github.nikolaikopernik.codecomplexity.dart.DartComplexityInfoProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.lang.dart.psi.DartArguments
import com.jetbrains.lang.dart.psi.DartComponent

private const val DART_TEST_FILES_PATH = "src/test/testData/dart"

class DartComplexityCalculationTest : BaseComplexityTest() {
    private val provider = DartComplexityInfoProvider()

    fun testDartFiles() = checkAllFilesInFolder(DART_TEST_FILES_PATH, ".dart", expectedMethodCount = 44)

    override fun getTestDataPath() = DART_TEST_FILES_PATH

    override fun createLanguageElementVisitor(sink: ComplexitySink): ElementVisitor =
        provider.getVisitor(sink)

    override fun parseTestFile(file: PsiFile): List<Triple<PsiElement, String, Int>> {
        // Declarations come from the provider's own predicate, so the expectedMethodCount
        // guard keeps the test aligned with what the plugin actually shows hints for.
        return PsiTreeUtil.collectElements(file) { provider.isComplexitySuitableMember(it) }
            .mapNotNull { decl ->
                val component = decl as DartComponent
                val complexity = component.complexityAnnotationValue() ?: return@mapNotNull null
                val name = component.componentName?.text ?: return@mapNotNull null
                Triple(decl as PsiElement, name, complexity)
            }
    }

    private fun DartComponent.complexityAnnotationValue(): Int? {
        val metadata = metadataList.firstOrNull { it.referenceExpression.text == "complexity" } ?: return null
        val args = PsiTreeUtil.findChildOfType(metadata, DartArguments::class.java) ?: return null
        val firstArg = args.argumentList?.expressionList?.firstOrNull() ?: return null
        return firstArg.text.toIntOrNull()
    }
}
