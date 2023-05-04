package com.github.nikolaikopernik.codecomplexity.java

import com.github.nikolaikopernik.codecomplexity.core.ComplexitySink
import com.github.nikolaikopernik.codecomplexity.core.ElementVisitor
import com.github.nikolaikopernik.codecomplexity.core.LanguageInfoProvider
import com.intellij.lang.Language
import com.intellij.lang.java.JavaLanguage
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassInitializer
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod

class JavaLanguageInfoProvider(override val language: Language = JavaLanguage.INSTANCE) : LanguageInfoProvider {
    override fun getVisitor(sink: ComplexitySink): ElementVisitor {
        return JavaLanguageVisitor(sink)
    }

    override fun isClassMember(element: PsiElement): Boolean {
        return element is PsiMethod ||
            element is PsiClassInitializer
    }

    override fun isClassWithBody(element: PsiElement): Boolean {
        return element is PsiClass && element.methods.isNotEmpty()
    }
}
