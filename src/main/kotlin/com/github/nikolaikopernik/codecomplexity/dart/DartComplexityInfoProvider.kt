package com.github.nikolaikopernik.codecomplexity.dart

import com.github.nikolaikopernik.codecomplexity.core.ComplexityInfoProvider
import com.github.nikolaikopernik.codecomplexity.core.ComplexitySink
import com.github.nikolaikopernik.codecomplexity.core.ElementVisitor
import com.intellij.lang.Language
import com.intellij.psi.PsiElement
import com.jetbrains.lang.dart.DartLanguage

class DartComplexityInfoProvider(override val language: Language = DartLanguage.INSTANCE) : ComplexityInfoProvider {

    override fun isComplexitySuitableMember(element: PsiElement): Boolean {
        // TODO: return true for top-level functions and class members. Candidates:
        // DartMethodDeclaration, DartFunctionDeclarationWithBody,
        // DartFunctionDeclarationWithBodyOrNative, DartGetterDeclaration,
        // DartSetterDeclaration, DartFactoryConstructorDeclaration.
        return false
    }

    override fun isClassWithBody(element: PsiElement): Boolean {
        // TODO: return true if element is a DartClassDefinition with at least one member.
        return false
    }

    override fun getVisitor(sink: ComplexitySink): ElementVisitor = DartLanguageVisitor(sink)

    override fun getNameElementFor(element: PsiElement): PsiElement {
        // TODO: resolve the name identifier for the Dart declaration node.
        return element
    }
}
