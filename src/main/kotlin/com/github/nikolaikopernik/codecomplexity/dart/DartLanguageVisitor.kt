package com.github.nikolaikopernik.codecomplexity.dart

import com.github.nikolaikopernik.codecomplexity.core.ComplexitySink
import com.github.nikolaikopernik.codecomplexity.core.ElementVisitor
import com.github.nikolaikopernik.codecomplexity.core.PointType.ELSE
import com.github.nikolaikopernik.codecomplexity.core.PointType.IF
import com.github.nikolaikopernik.codecomplexity.core.PointType.LOOP_FOR
import com.github.nikolaikopernik.codecomplexity.core.PointType.LOOP_WHILE
import com.github.nikolaikopernik.codecomplexity.core.PointType.SWITCH
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.jetbrains.lang.dart.DartTokenTypes
import com.jetbrains.lang.dart.psi.DartDoWhileStatement
import com.jetbrains.lang.dart.psi.DartForStatement
import com.jetbrains.lang.dart.psi.DartIfStatement
import com.jetbrains.lang.dart.psi.DartSwitchExpression
import com.jetbrains.lang.dart.psi.DartSwitchStatement
import com.jetbrains.lang.dart.psi.DartWhileStatement

internal class DartLanguageVisitor(private val sink: ComplexitySink) : ElementVisitor() {

    override fun processElement(element: PsiElement) {
        when (element) {
            is DartIfStatement -> if (!element.isElseIf()) sink.increaseComplexityAndNesting(IF)
            is DartForStatement -> sink.increaseComplexityAndNesting(LOOP_FOR)
            is DartWhileStatement -> sink.increaseComplexityAndNesting(LOOP_WHILE)
            is DartDoWhileStatement -> sink.increaseComplexityAndNesting(LOOP_WHILE)
            is DartSwitchStatement -> sink.increaseComplexityAndNesting(SWITCH)
            is DartSwitchExpression -> sink.increaseComplexityAndNesting(SWITCH)
        }
        if (element.isElseKeyword()) {
            sink.increaseComplexity(ELSE)
        }
    }

    override fun postProcess(element: PsiElement) {
        when (element) {
            is DartIfStatement -> if (!element.isElseIf()) sink.decreaseNesting()
            is DartForStatement,
            is DartWhileStatement,
            is DartDoWhileStatement,
            is DartSwitchStatement,
            is DartSwitchExpression -> sink.decreaseNesting()
        }
    }

    override fun shouldVisitElement(element: PsiElement): Boolean = true
}

private fun PsiElement.isElseKeyword(): Boolean =
    node?.elementType == DartTokenTypes.ELSE && parent is DartIfStatement

private fun DartIfStatement.isElseIf(): Boolean =
    prevNotWhitespace()?.node?.elementType == DartTokenTypes.ELSE

private fun DartIfStatement.prevNotWhitespace(): PsiElement? {
    var prev: PsiElement = this
    while (prev.prevSibling != null) {
        prev = prev.prevSibling
        if (prev !is PsiWhiteSpace) return prev
    }
    return null
}
