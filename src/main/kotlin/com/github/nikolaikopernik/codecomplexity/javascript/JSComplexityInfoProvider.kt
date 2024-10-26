package com.github.nikolaikopernik.codecomplexity.javascript

import com.github.nikolaikopernik.codecomplexity.core.ComplexityInfoProvider
import com.github.nikolaikopernik.codecomplexity.core.ComplexitySink
import com.github.nikolaikopernik.codecomplexity.core.ElementVisitor
import com.intellij.lang.Language
import com.intellij.lang.javascript.JavascriptLanguage
import com.intellij.lang.javascript.dialects.ECMA6LanguageDialect
import com.intellij.lang.javascript.dialects.ECMAL4LanguageDialect
import com.intellij.lang.javascript.dialects.FlowJSLanguageDialect
import com.intellij.lang.javascript.dialects.TypeScriptJSXLanguageDialect
import com.intellij.lang.javascript.dialects.TypeScriptLanguageDialect
import com.intellij.lang.javascript.psi.JSFunction
import com.intellij.lang.javascript.psi.ecma6.TypeScriptTypeAlias
import com.intellij.lang.javascript.psi.ecma6.TypeScriptTypeMember
import com.intellij.lang.javascript.psi.ecmal4.JSClass
import com.intellij.psi.PsiElement

class JSComplexityInfoProvider : ComplexityInfoProvider {
    // FIXME: language with dialects
    override val language: Language = listOf(
        TypeScriptLanguageDialect.getInstance(),
        TypeScriptJSXLanguageDialect.getInstance(),
        JavascriptLanguage.INSTANCE,
        ECMA6LanguageDialect.getInstance(),
        ECMAL4LanguageDialect.getInstance(),
        FlowJSLanguageDialect.getInstance(),
    ).first()

    override fun getVisitor(sink: ComplexitySink): ElementVisitor = JSLanguageVisitor(sink)

    override fun isComplexitySuitableMember(element: PsiElement): Boolean = when {
        element !is JSFunction -> false
        element is TypeScriptTypeMember -> false
        element.isShorthandArrowFunction -> false
        element.block == null -> false
        else -> true
    }

    override fun isClassWithBody(element: PsiElement): Boolean = when {
        element !is JSClass -> false
        element.isInterface -> false
        element is TypeScriptTypeAlias -> false
        element.members.iterator().hasNext().not() -> false
        else -> true
    }

    override fun getNameElementFor(element: PsiElement): PsiElement = when (element) {
        is JSFunction -> element.nameIdentifier ?: element
        is JSClass -> element.members.firstOrNull() ?: element
        else -> element
    }
}
