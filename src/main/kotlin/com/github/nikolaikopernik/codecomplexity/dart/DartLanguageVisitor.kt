package com.github.nikolaikopernik.codecomplexity.dart

import com.github.nikolaikopernik.codecomplexity.core.ComplexitySink
import com.github.nikolaikopernik.codecomplexity.core.ElementVisitor
import com.github.nikolaikopernik.codecomplexity.core.PointType
import com.github.nikolaikopernik.codecomplexity.core.PointType.BREAK
import com.github.nikolaikopernik.codecomplexity.core.PointType.CATCH
import com.github.nikolaikopernik.codecomplexity.core.PointType.CONTINUE
import com.github.nikolaikopernik.codecomplexity.core.PointType.ELSE
import com.github.nikolaikopernik.codecomplexity.core.PointType.IF
import com.github.nikolaikopernik.codecomplexity.core.PointType.LOGICAL_AND
import com.github.nikolaikopernik.codecomplexity.core.PointType.LOGICAL_OR
import com.github.nikolaikopernik.codecomplexity.core.PointType.LOOP_FOR
import com.github.nikolaikopernik.codecomplexity.core.PointType.LOOP_WHILE
import com.github.nikolaikopernik.codecomplexity.core.PointType.RECURSION
import com.github.nikolaikopernik.codecomplexity.core.PointType.SWITCH
import com.github.nikolaikopernik.codecomplexity.core.PointType.UNKNOWN
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.tree.IElementType
import com.jetbrains.lang.dart.DartTokenTypes
import com.jetbrains.lang.dart.psi.DartBreakStatement
import com.jetbrains.lang.dart.psi.DartCallExpression
import com.jetbrains.lang.dart.psi.DartContinueStatement
import com.jetbrains.lang.dart.psi.DartDoWhileStatement
import com.jetbrains.lang.dart.psi.DartExpression
import com.jetbrains.lang.dart.psi.DartFormalParameterList
import com.jetbrains.lang.dart.psi.DartForStatement
import com.jetbrains.lang.dart.psi.DartFunctionDeclarationWithBody
import com.jetbrains.lang.dart.psi.DartFunctionDeclarationWithBodyOrNative
import com.jetbrains.lang.dart.psi.DartFunctionExpression
import com.jetbrains.lang.dart.psi.DartIfNullExpression
import com.jetbrains.lang.dart.psi.DartIfStatement
import com.jetbrains.lang.dart.psi.DartLogicAndExpression
import com.jetbrains.lang.dart.psi.DartLogicOrExpression
import com.jetbrains.lang.dart.psi.DartMethodDeclaration
import com.jetbrains.lang.dart.psi.DartOnPart
import com.jetbrains.lang.dart.psi.DartParenthesizedExpression
import com.jetbrains.lang.dart.psi.DartPrefixExpression
import com.jetbrains.lang.dart.psi.DartSwitchExpression
import com.jetbrains.lang.dart.psi.DartSwitchStatement
import com.jetbrains.lang.dart.psi.DartTernaryExpression
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
            is DartFunctionExpression -> sink.increaseNesting()
            is DartTernaryExpression -> {
                sink.increaseComplexityAndNesting(IF)
                element.calculateBinaryComplexity()
            }
            is DartLogicAndExpression,
            is DartLogicOrExpression,
            is DartIfNullExpression -> {
                // Accept only top-level binary expressions; nested ones are walked
                // by the outer's calculateBinaryComplexity with a shared operand list
                // so consecutive same-kind operators score once.
                if (element.parent !is DartExpression) {
                    element.calculateBinaryComplexity()
                }
            }
            is DartCallExpression -> if (element.isRecursion()) sink.increaseComplexity(RECURSION)
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
            is DartOnPart,
            is DartTernaryExpression,
            is DartFunctionExpression -> sink.decreaseNesting()
        }
    }

    override fun shouldVisitElement(element: PsiElement): Boolean = true

    private fun PsiElement.calculateBinaryComplexity(operands: MutableList<IElementType> = mutableListOf()) {
        // Iterate via firstChild/nextSibling — Dart's BNF-generated PSI uses
        // ASTDelegatePsiElement.getChildren() which only returns composite children,
        // omitting leaf tokens like the && / || / ?? operators we need to inspect.
        var child: PsiElement? = firstChild
        while (child != null) {
            when {
                child.isBinaryLogicExpression() -> child.calculateBinaryComplexity(operands)
                child is DartParenthesizedExpression -> {
                    child.calculateBinaryComplexity()
                    operands.clear()
                }
                child is DartPrefixExpression -> {
                    child.calculateBinaryComplexity()
                    operands.clear()
                }
                child.isLogicOperatorToken() -> {
                    val tt = child.node.elementType
                    if (operands.lastOrNull() != tt) {
                        sink.increaseComplexity(tt.toPointType())
                    }
                    operands.add(tt)
                }
            }
            child = child.nextSibling
        }
    }
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

private fun PsiElement.isBinaryLogicExpression(): Boolean =
    this is DartLogicAndExpression ||
        this is DartLogicOrExpression ||
        this is DartIfNullExpression

private fun PsiElement.isLogicOperatorToken(): Boolean {
    val tt = node?.elementType ?: return false
    return tt == DartTokenTypes.AND_AND || tt == DartTokenTypes.OR_OR || tt == DartTokenTypes.QUEST_QUEST
}

private fun IElementType.toPointType(): PointType = when (this) {
    DartTokenTypes.AND_AND -> LOGICAL_AND
    DartTokenTypes.OR_OR -> LOGICAL_OR
    DartTokenTypes.QUEST_QUEST -> LOGICAL_OR
    else -> UNKNOWN
}

private data class EnclosingFunction(val name: String, val paramCount: Int)

private fun DartCallExpression.isRecursion(): Boolean {
    val enclosing = findEnclosingFunction() ?: return false
    if (this.expression?.text != enclosing.name) return false
    return this.argCount() == enclosing.paramCount
}

private fun PsiElement.findEnclosingFunction(): EnclosingFunction? {
    var p: PsiElement? = this.parent
    while (p != null) {
        when (p) {
            is DartMethodDeclaration -> return p.componentName?.text?.let {
                EnclosingFunction(it, p.formalParameterList.countParams())
            }
            is DartFunctionDeclarationWithBody -> return p.componentName.text?.let {
                EnclosingFunction(it, p.formalParameterList.countParams())
            }
            is DartFunctionDeclarationWithBodyOrNative -> return p.componentName.text?.let {
                EnclosingFunction(it, p.formalParameterList.countParams())
            }
        }
        p = p.parent
    }
    return null
}

private fun DartCallExpression.argCount(): Int =
    arguments?.argumentList?.let { it.expressionList.size + it.namedArgumentList.size } ?: 0

private fun DartFormalParameterList.countParams(): Int =
    normalFormalParameterList.size + (optionalFormalParameters?.defaultFormalNamedParameterList?.size ?: 0)
