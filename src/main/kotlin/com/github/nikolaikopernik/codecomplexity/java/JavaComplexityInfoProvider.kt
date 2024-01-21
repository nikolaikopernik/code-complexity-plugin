package com.github.nikolaikopernik.codecomplexity.java

import com.github.nikolaikopernik.codecomplexity.core.ComplexityInfoProvider
import com.github.nikolaikopernik.codecomplexity.core.ComplexitySink
import com.github.nikolaikopernik.codecomplexity.core.ElementVisitor
import com.intellij.lang.Language
import com.intellij.lang.java.JavaLanguage
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassInitializer
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod

class JavaComplexityInfoProvider(override val language: Language = JavaLanguage.INSTANCE) : ComplexityInfoProvider {
    override fun getVisitor(sink: ComplexitySink): ElementVisitor {
        return JavaLanguageVisitor(sink)
    }

    override fun isComplexitySuitableMember(element: PsiElement): Boolean {
        return element is PsiMethod ||
            element is PsiClassInitializer
    }

    override fun isClassWithBody(element: PsiElement): Boolean {
        return element is PsiClass && element.methods.isNotEmpty()
    }

    override fun getNameElementFor(element: PsiElement): PsiElement {
        if(element is PsiMethod){
            return element.nameIdentifier ?: element
        } else if (element is PsiClassInitializer ) {
            return element.body.lBrace ?: element
        }
        return element
    }
}
