package com.github.nikolaikopernik.codecomplexity.kotlin

import com.github.nikolaikopernik.codecomplexity.core.ComplexityInfoProvider
import com.github.nikolaikopernik.codecomplexity.core.ComplexitySink
import com.github.nikolaikopernik.codecomplexity.core.ElementVisitor
import com.intellij.lang.Language
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassInitializer
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.jetbrains.kotlin.psi.KtSecondaryConstructor

class KtComplexityInfoProvider(override val language: Language = KotlinLanguage.INSTANCE) : ComplexityInfoProvider {
    private val classMemberList = listOf(
        KtSecondaryConstructor::class.java,
        KtClassInitializer::class.java,
        KtNamedFunction::class.java,
        KtObjectDeclaration::class.java
    )

    private fun considerPropertyAccessorsComplexity(element: PsiElement): Boolean = element is KtPropertyAccessor

    override fun isComplexitySuitableMember(element: PsiElement): Boolean {
        return element::class.java in classMemberList || considerPropertyAccessorsComplexity(element)
    }

    override fun isClassWithBody(element: PsiElement): Boolean {
        return element is KtClass && element.body != null
    }

    override fun getVisitor(sink: ComplexitySink): ElementVisitor {
        return KtLanguageVisitor(sink)
    }

    override fun getNameElementFor(element: PsiElement): PsiElement =
        when(element){
            is KtNamedFunction -> element.nameIdentifier ?: element
            is KtClassInitializer -> element.body?.firstChild ?: element
            is KtObjectDeclaration -> element.nameIdentifier ?: element
            is KtSecondaryConstructor -> element.bodyExpression?.firstChild ?: element
            else -> element
        }

}
