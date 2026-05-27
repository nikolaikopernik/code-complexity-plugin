package com.github.nikolaikopernik.codecomplexity

import com.github.nikolaikopernik.codecomplexity.core.ComplexitySink
import com.github.nikolaikopernik.codecomplexity.core.ElementVisitor
import com.github.nikolaikopernik.codecomplexity.dart.DartComplexityInfoProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.lang.dart.psi.DartArguments
import com.jetbrains.lang.dart.psi.DartComponent
import com.jetbrains.lang.dart.psi.DartFactoryConstructorDeclaration
import com.jetbrains.lang.dart.psi.DartFunctionDeclarationWithBody
import com.jetbrains.lang.dart.psi.DartFunctionDeclarationWithBodyOrNative
import com.jetbrains.lang.dart.psi.DartGetterDeclaration
import com.jetbrains.lang.dart.psi.DartMethodDeclaration
import com.jetbrains.lang.dart.psi.DartNamedConstructorDeclaration
import com.jetbrains.lang.dart.psi.DartSetterDeclaration
import org.junit.Test

private const val DART_TEST_FILES_PATH = "src/test/testData/dart"

class DartComplexityCalculationTest : BaseComplexityTest() {
    @Test
    fun testDartFiles() = checkAllFilesInFolder(DART_TEST_FILES_PATH, ".dart")

    override fun getTestDataPath() = DART_TEST_FILES_PATH

    override fun createLanguageElementVisitor(sink: ComplexitySink): ElementVisitor =
        DartComplexityInfoProvider().getVisitor(sink)

    override fun parseTestFile(file: PsiFile): List<Triple<PsiElement, String, Int>> {
        val declarations = PsiTreeUtil.findChildrenOfAnyType(
            file,
            DartMethodDeclaration::class.java,
            DartFunctionDeclarationWithBody::class.java,
            DartFunctionDeclarationWithBodyOrNative::class.java,
            DartGetterDeclaration::class.java,
            DartSetterDeclaration::class.java,
            DartFactoryConstructorDeclaration::class.java,
            DartNamedConstructorDeclaration::class.java
        )
        return declarations.mapNotNull { decl ->
            val component = decl as DartComponent
            val complexity = component.complexityAnnotationValue() ?: return@mapNotNull null
            val name = component.componentName?.text ?: return@mapNotNull null
            Triple(decl as PsiElement, name, complexity)
        }
    }

    private fun DartComponent.complexityAnnotationValue(): Int? {
        val metadata = metadataList.firstOrNull { it.referenceExpression?.text == "complexity" } ?: return null
        val args = PsiTreeUtil.findChildOfType(metadata, DartArguments::class.java) ?: return null
        val firstArg = args.argumentList?.expressionList?.firstOrNull() ?: return null
        return firstArg.text.toIntOrNull()
    }
}
