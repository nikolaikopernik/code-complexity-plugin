package com.github.nikolaikopernik.codecomplexity.python

import com.github.nikolaikopernik.codecomplexity.core.ComplexitySink
import com.github.nikolaikopernik.codecomplexity.core.ElementVisitor
import com.github.nikolaikopernik.codecomplexity.core.LanguageInfoProvider
import com.intellij.lang.Language
import com.intellij.psi.PsiElement
import com.jetbrains.python.PythonLanguage
import com.jetbrains.python.psi.PyClass
import com.jetbrains.python.psi.PyNamedElementContainer

class PythonLanguageInfoProvider(override val language: Language = PythonLanguage.INSTANCE) : LanguageInfoProvider {
    private val classMemberList = listOf(
        PyNamedElementContainer::class.java
    )

    override fun isClassMember(element: PsiElement): Boolean {
        return element::class.java in classMemberList
    }

    override fun isClassWithBody(element: PsiElement): Boolean {
        return element is PyClass && element.methods.isNotEmpty()
    }

    override fun getVisitor(sink: ComplexitySink): ElementVisitor {
        return PythonLanguageVisitor(sink)
    }
}
