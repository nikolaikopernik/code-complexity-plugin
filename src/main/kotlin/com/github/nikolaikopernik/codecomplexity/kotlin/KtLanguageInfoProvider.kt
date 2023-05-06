package com.github.nikolaikopernik.codecomplexity.kotlin

import com.github.nikolaikopernik.codecomplexity.core.ComplexitySink
import com.github.nikolaikopernik.codecomplexity.core.ElementVisitor
import com.github.nikolaikopernik.codecomplexity.core.LanguageInfoProvider
import com.github.nikolaikopernik.codecomplexity.core.PointType
import com.github.nikolaikopernik.codecomplexity.core.PointType.*
import com.intellij.lang.Language
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.lexer.KtToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*

class KtLanguageInfoProvider(override val language: Language = KotlinLanguage.INSTANCE) : LanguageInfoProvider {
    private val classMemberList = listOf(
        KtSecondaryConstructor::class.java,
        KtClassInitializer::class.java,
        KtNamedFunction::class.java,
        KtObjectDeclaration::class.java
    )

    private fun considerPropertyAccessorsComplexity(element: PsiElement): Boolean = element is KtPropertyAccessor

    override fun isClassMember(element: PsiElement): Boolean {
        return element::class.java in classMemberList || considerPropertyAccessorsComplexity(element)
    }

    override fun isClassWithBody(element: PsiElement): Boolean {
        return element is KtClass && element.body != null
    }

    override fun getVisitor(sink: ComplexitySink): ElementVisitor {
        return KtLanguageVisitor(sink)
    }

}
