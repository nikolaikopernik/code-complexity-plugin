package com.github.nikolaikopernik.codecomplexity.javascript

import com.github.nikolaikopernik.codecomplexity.core.ComplexitySink
import com.github.nikolaikopernik.codecomplexity.core.ElementVisitor
import com.github.nikolaikopernik.codecomplexity.core.PointType
import com.github.nikolaikopernik.codecomplexity.core.PointType.BREAK
import com.github.nikolaikopernik.codecomplexity.core.PointType.CATCH
import com.github.nikolaikopernik.codecomplexity.core.PointType.CONTINUE
import com.github.nikolaikopernik.codecomplexity.core.PointType.IF
import com.github.nikolaikopernik.codecomplexity.core.PointType.LOGICAL_AND
import com.github.nikolaikopernik.codecomplexity.core.PointType.LOGICAL_OR
import com.github.nikolaikopernik.codecomplexity.core.PointType.LOOP_FOR
import com.github.nikolaikopernik.codecomplexity.core.PointType.LOOP_WHILE
import com.github.nikolaikopernik.codecomplexity.core.PointType.RECURSION
import com.github.nikolaikopernik.codecomplexity.core.PointType.SWITCH
import com.github.nikolaikopernik.codecomplexity.core.PointType.UNKNOWN
import com.intellij.lang.javascript.JSElementType
import com.intellij.lang.javascript.JSTokenTypes
import com.intellij.lang.javascript.psi.JSBinaryExpression
import com.intellij.lang.javascript.psi.JSBreakStatement
import com.intellij.lang.javascript.psi.JSCallExpression
import com.intellij.lang.javascript.psi.JSCatchBlock
import com.intellij.lang.javascript.psi.JSConditionalExpression
import com.intellij.lang.javascript.psi.JSContinueStatement
import com.intellij.lang.javascript.psi.JSDoWhileStatement
import com.intellij.lang.javascript.psi.JSExpression
import com.intellij.lang.javascript.psi.JSForInStatement
import com.intellij.lang.javascript.psi.JSForStatement
import com.intellij.lang.javascript.psi.JSFunction
import com.intellij.lang.javascript.psi.JSFunctionExpression
import com.intellij.lang.javascript.psi.JSIfStatement
import com.intellij.lang.javascript.psi.JSParenthesizedExpression
import com.intellij.lang.javascript.psi.JSPrefixExpression
import com.intellij.lang.javascript.psi.JSSwitchStatement
import com.intellij.lang.javascript.psi.JSWhileStatement
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiKeyword
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.tree.IElementType

class JSLanguageVisitor(private val sink: ComplexitySink) : ElementVisitor() {
    override fun processElement(element: PsiElement) {
        when (element) {
            is JSWhileStatement -> sink.increaseComplexityAndNesting(LOOP_WHILE)
            is JSDoWhileStatement -> sink.increaseComplexityAndNesting(LOOP_WHILE)
            is JSForStatement -> sink.increaseComplexityAndNesting(LOOP_FOR)
            is JSForInStatement -> sink.increaseComplexityAndNesting(LOOP_FOR)
            is JSIfStatement -> element.processIfExpression()
            is JSSwitchStatement -> sink.increaseComplexityAndNesting(SWITCH)
            is JSConditionalExpression -> {
                sink.increaseComplexityAndNesting(IF)
                element.calculateBinaryComplexity()
            }

            is JSCatchBlock -> sink.increaseComplexityAndNesting(CATCH)
            is JSBreakStatement -> if (element.labelIdentifier != null) sink.increaseComplexity(BREAK)
            is JSContinueStatement -> if (element.labelIdentifier != null) sink.increaseComplexity(CONTINUE)
            is JSFunctionExpression -> sink.increaseNesting()
            is JSCallExpression -> if (element.isRecursion()) sink.increaseComplexity(RECURSION)
        }
    }

    override fun postProcess(element: PsiElement) {
        if (element is JSWhileStatement ||
            element is JSDoWhileStatement ||
            element is JSForStatement ||
            element is JSForInStatement ||
            element is JSCatchBlock ||
            element is JSFunctionExpression ||
            element is JSIfStatement && element.elseBranch !is JSIfStatement
        ) {
            sink.decreaseNesting()
        }
    }

    override fun shouldVisitElement(element: PsiElement) = true

    private fun JSIfStatement.processIfExpression() {
        if (this.isElseIf()) {
            return
        }
        sink.increaseComplexityAndNesting(IF)
    }

    private fun JSExpression.calculateBinaryComplexity(operands: MutableList<JSElementType> = mutableListOf()) {
        val elements = when (this) {
            is JSBinaryExpression -> listOf(this.lOperand, this.operationSign, this.rOperand)
            is JSParenthesizedExpression -> listOf(this.innerExpression)
            is JSPrefixExpression -> listOf(this.operationSign)
            else -> emptyList()
        }

        elements.forEach { element ->
            when (element) {
                is JSElementType -> if (element in listOf(JSTokenTypes.AND, JSTokenTypes.OR)) {
                    if (operands.lastOrNull() == null || element != operands.lastOrNull()) {
                        sink.increaseComplexity(element.toPointType())
                    }
                    operands.add(element)
                }

                is JSParenthesizedExpression -> {
                    element.calculateBinaryComplexity()
                    operands.clear()
                }

                is JSPrefixExpression -> {
                    element.calculateBinaryComplexity()
                    operands.clear()
                }

                is JSBinaryExpression -> element.calculateBinaryComplexity(operands)
            }
        }
    }
}

private fun JSCallExpression.isRecursion(): Boolean {
    val parentMethod: JSFunction = this.findCurrentJSFunction() ?: return false
    if (this.methodExpression?.text != parentMethod.name) return false
    if (this.arguments.size != parentMethod.parameterList?.parameters?.size) return false
    return true
}

private fun PsiElement.findCurrentJSFunction(): JSFunction? {
    var element: PsiElement? = this
    while (element != null && element !is JSFunction) element = element.parent
    return element?.let { it as JSFunction }
}

private fun JSIfStatement.isElseIf(): Boolean = this.prevNotWhitespace().isElse()

private fun PsiElement?.isElse(): Boolean = this?.let {
    it is PsiKeyword && it.text == PsiKeyword.ELSE
} ?: false

private fun JSIfStatement.prevNotWhitespace(): PsiElement? {
    var prev: PsiElement = this
    while (prev.prevSibling != null) {
        prev = prev.prevSibling
        if (prev !is PsiWhiteSpace) {
            return prev
        }
    }
    return null
}

private fun IElementType.toPointType(): PointType = when (this) {
    JSTokenTypes.OROR -> LOGICAL_OR
    JSTokenTypes.ANDAND -> LOGICAL_AND
    else -> UNKNOWN
}
