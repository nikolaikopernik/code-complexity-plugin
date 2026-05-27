package com.github.nikolaikopernik.codecomplexity.dart

import com.github.nikolaikopernik.codecomplexity.core.ComplexityInfoProvider
import com.github.nikolaikopernik.codecomplexity.core.ComplexitySink
import com.github.nikolaikopernik.codecomplexity.core.ElementVisitor
import com.intellij.lang.Language
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.lang.dart.DartLanguage
import com.jetbrains.lang.dart.psi.DartClassDefinition
import com.jetbrains.lang.dart.psi.DartComponent
import com.jetbrains.lang.dart.psi.DartFactoryConstructorDeclaration
import com.jetbrains.lang.dart.psi.DartFunctionDeclarationWithBody
import com.jetbrains.lang.dart.psi.DartFunctionDeclarationWithBodyOrNative
import com.jetbrains.lang.dart.psi.DartGetterDeclaration
import com.jetbrains.lang.dart.psi.DartMethodDeclaration
import com.jetbrains.lang.dart.psi.DartNamedConstructorDeclaration
import com.jetbrains.lang.dart.psi.DartSetterDeclaration

class DartComplexityInfoProvider(override val language: Language = DartLanguage.INSTANCE) : ComplexityInfoProvider {

    override fun isComplexitySuitableMember(element: PsiElement): Boolean = when (element) {
        is DartFunctionDeclarationWithBody,
        is DartFunctionDeclarationWithBodyOrNative,
        is DartGetterDeclaration,
        is DartSetterDeclaration,
        is DartFactoryConstructorDeclaration -> true
        is DartMethodDeclaration -> !element.isConstructor
        is DartNamedConstructorDeclaration -> element.hasNonTrivialBody()
        else -> false
    }

    override fun isClassWithBody(element: PsiElement): Boolean {
        if (element !is DartClassDefinition) return false
        return PsiTreeUtil.findChildOfAnyType(
            element,
            DartMethodDeclaration::class.java,
            DartGetterDeclaration::class.java,
            DartSetterDeclaration::class.java,
            DartFactoryConstructorDeclaration::class.java,
            DartNamedConstructorDeclaration::class.java
        ) != null
    }

    override fun getVisitor(sink: ComplexitySink): ElementVisitor = DartLanguageVisitor(sink)

    override fun getNameElementFor(element: PsiElement): PsiElement =
        (element as? DartComponent)?.componentName ?: element
}

private fun DartNamedConstructorDeclaration.hasNonTrivialBody(): Boolean {
    val block = functionBody?.block ?: return false
    val statements = block.statements ?: return false
    return statements.children.isNotEmpty()
}
