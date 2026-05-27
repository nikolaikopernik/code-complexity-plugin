package com.github.nikolaikopernik.codecomplexity.dart

import com.github.nikolaikopernik.codecomplexity.core.ComplexitySink
import com.github.nikolaikopernik.codecomplexity.core.ElementVisitor
import com.github.nikolaikopernik.codecomplexity.core.PointType.BREAK
import com.github.nikolaikopernik.codecomplexity.core.PointType.CATCH
import com.github.nikolaikopernik.codecomplexity.core.PointType.CONTINUE
import com.github.nikolaikopernik.codecomplexity.core.PointType.ELSE
import com.github.nikolaikopernik.codecomplexity.core.PointType.IF
import com.github.nikolaikopernik.codecomplexity.core.PointType.LOOP_FOR
import com.github.nikolaikopernik.codecomplexity.core.PointType.LOOP_WHILE
import com.github.nikolaikopernik.codecomplexity.core.PointType.SWITCH
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.jetbrains.lang.dart.DartTokenTypes
import com.jetbrains.lang.dart.psi.DartBreakStatement
import com.jetbrains.lang.dart.psi.DartContinueStatement
import com.jetbrains.lang.dart.psi.DartDoWhileStatement
import com.jetbrains.lang.dart.psi.DartForStatement
import com.jetbrains.lang.dart.psi.DartIfStatement
import com.jetbrains.lang.dart.psi.DartOnPart
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
            is DartOnPart -> sink.increaseComplexityAndNesting(CATCH)
            is DartBreakStatement -> if (element.referenceExpression != null) sink.increaseComplexity(BREAK)
            is DartContinueStatement -> if (element.referenceExpression != null) sink.increaseComplexity(CONTINUE)
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
            is DartSwitchExpression,
            is DartOnPart -> sink.decreaseNesting()
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
