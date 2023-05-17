package com.github.nikolaikopernik.codecomplexity.python

import com.github.nikolaikopernik.codecomplexity.core.ComplexitySink
import com.github.nikolaikopernik.codecomplexity.core.ElementVisitor
import com.github.nikolaikopernik.codecomplexity.core.LanguageInfoProvider
import com.intellij.lang.Language
import com.intellij.psi.PsiElement
import com.jetbrains.python.PythonLanguage
import com.jetbrains.python.psi.PyClass
import com.jetbrains.python.psi.PyFunction

class PythonLanguageInfoProvider(override val language: Language = PythonLanguage.INSTANCE) : LanguageInfoProvider {

    override fun isClassMember(element: PsiElement): Boolean {
        return element is PyFunction && element.parent?.findCurrentPythonMethod() == null
    }

    override fun isClassWithBody(element: PsiElement): Boolean {
        return element is PyClass && element.methods.isNotEmpty()
    }

    override fun getVisitor(sink: ComplexitySink): ElementVisitor {
        return PythonLanguageVisitor(sink)
    }
}
