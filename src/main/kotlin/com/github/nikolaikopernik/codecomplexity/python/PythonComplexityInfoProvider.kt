package com.github.nikolaikopernik.codecomplexity.python

import com.github.nikolaikopernik.codecomplexity.core.ComplexityInfoProvider
import com.github.nikolaikopernik.codecomplexity.core.ComplexitySink
import com.github.nikolaikopernik.codecomplexity.core.ElementVisitor
import com.intellij.lang.Language
import com.intellij.psi.PsiElement
import com.jetbrains.python.PythonLanguage
import com.jetbrains.python.psi.PyClass
import com.jetbrains.python.psi.PyFunction

class PythonComplexityInfoProvider(override val language: Language = PythonLanguage.INSTANCE) : ComplexityInfoProvider {

    override fun isComplexitySuitableMember(element: PsiElement): Boolean {
        return element is PyFunction && element.parent?.findCurrentPythonMethod() == null
    }

    override fun isClassWithBody(element: PsiElement): Boolean {
        return element is PyClass && element.methods.isNotEmpty()
    }

    override fun getVisitor(sink: ComplexitySink): ElementVisitor {
        return PythonLanguageVisitor(sink)
    }

    override fun getNameElementFor(element: PsiElement): PsiElement {
        if (element is PyFunction) return element.nameIdentifier ?: element
        return element
    }
}
