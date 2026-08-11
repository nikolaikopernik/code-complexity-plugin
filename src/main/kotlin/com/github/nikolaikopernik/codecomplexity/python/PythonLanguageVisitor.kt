// Uses the experimental Python PSI API - no stable equivalent yet.
@file:Suppress("UnstableApiUsage")

package com.github.nikolaikopernik.codecomplexity.python

import com.github.nikolaikopernik.codecomplexity.core.ComplexitySink
import com.github.nikolaikopernik.codecomplexity.core.ElementVisitor
import com.github.nikolaikopernik.codecomplexity.core.PointType
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType
import com.jetbrains.python.PyTokenTypes
import com.jetbrains.python.psi.PyBinaryExpression
import com.jetbrains.python.psi.PyBreakStatement
import com.jetbrains.python.psi.PyCallExpression
import com.jetbrains.python.psi.PyConditionalExpression
import com.jetbrains.python.psi.PyContinueStatement
import com.jetbrains.python.psi.PyElementType
import com.jetbrains.python.psi.PyElsePart
import com.jetbrains.python.psi.PyExceptPart
import com.jetbrains.python.psi.PyExpression
import com.jetbrains.python.psi.PyForStatement
import com.jetbrains.python.psi.PyFunction
import com.jetbrains.python.psi.PyIfPart
import com.jetbrains.python.psi.PyLambdaExpression
import com.jetbrains.python.psi.PyMatchStatement
import com.jetbrains.python.psi.PyParenthesizedExpression
import com.jetbrains.python.psi.PyPrefixExpression
import com.jetbrains.python.psi.PyTryExceptStatement
import com.jetbrains.python.psi.PyWhileStatement

internal class PythonLanguageVisitor(private val sink: ComplexitySink) : ElementVisitor() {
    override fun processElement(element: PsiElement) {
        when (element) {
            is PyWhileStatement -> sink.increaseComplexityAndNesting(PointType.LOOP_WHILE)
            // `match` scores like a switch: one point for the statement, none per `case`
            is PyMatchStatement -> sink.increaseComplexityAndNesting(PointType.SWITCH)
            is PyIfPart -> sink.increaseComplexityAndNesting(PointType.IF)
            is PyElsePart -> sink.increaseComplexity(PointType.ELSE)
            is PyForStatement -> sink.increaseComplexityAndNesting(PointType.LOOP_FOR)
            is PyConditionalExpression -> {
                sink.increaseComplexityAndNesting(PointType.IF)
                // the condition sits inside this expression, so the PyBinaryExpression
                // branch below skips it; walk it here like Java and Dart do
                element.calculateBinaryComplexity()
            }
            is PyExceptPart -> sink.increaseComplexityAndNesting(PointType.CATCH)
            is PyBreakStatement -> if (element.loopStatement != null) sink.increaseComplexity(PointType.BREAK)
            is PyContinueStatement -> if (element.loopStatement != null) sink.increaseComplexity(PointType.CONTINUE)
            is PyLambdaExpression -> sink.increaseNesting()
            is PyBinaryExpression -> {
                if (element.parent !is PyExpression) {
                    element.calculateBinaryComplexity()
                }
            }

            is PyFunction -> if (element.isDecorator()) sink.increaseNesting()
            is PyCallExpression -> if (element.isRecursion()) sink.increaseComplexity(PointType.RECURSION)
        }
    }

    override fun postProcess(element: PsiElement) {
        // No PyTryExceptStatement here: `try` itself never increases nesting, and decreasing
        // for it left every following sibling one level too shallow.
        if (element is PyWhileStatement ||
            element is PyMatchStatement ||
            element is PyConditionalExpression ||
            element is PyIfPart ||
            element is PyForStatement ||
            element is PyExceptPart ||
            element is PyLambdaExpression) {
            sink.decreaseNesting()
        }

        if (element is PyFunction && element.isDecorator()) {
            sink.decreaseNesting()
        }
    }

    override fun shouldVisitElement(element: PsiElement): Boolean = true

    private fun PyExpression.calculateBinaryComplexity(operands: MutableList<PyElementType> = mutableListOf()) {
        val elements = when (this) {
            is PyConditionalExpression -> listOf(this.condition)
            is PyBinaryExpression -> listOf(this.leftExpression, this.operator, this.rightExpression)
            is PyParenthesizedExpression -> listOf(this.containedExpression)
            is PyPrefixExpression -> listOf(this.operand)
            else -> emptyList()
        }

        elements.forEach { element ->
            when (element) {
                is PyElementType -> if (element in listOf(PyTokenTypes.AND_KEYWORD, PyTokenTypes.OR_KEYWORD)) {
                    if (operands.lastOrNull() == null || element != operands.lastOrNull()) {
                        sink.increaseComplexity(element.toPointType())
                    }
                    operands.add(element)
                }

                is PyParenthesizedExpression -> {
                    element.calculateBinaryComplexity()
                    operands.clear()
                }

                is PyPrefixExpression -> {
                    element.calculateBinaryComplexity()
                    operands.clear()
                }

                is PyBinaryExpression -> element.calculateBinaryComplexity(operands)
            }
        }
    }

    private fun PyFunction.isDecorator(): Boolean =
        this.parent.findCurrentPythonMethod() != null && this.prevSibling != null


    private fun PyCallExpression.isRecursion(): Boolean {
        val parentMethod: PyFunction = this.findCurrentPythonMethod() ?: return false
        if (this.callee?.text != parentMethod.name) return false
        if (this.arguments.size != parentMethod.parameterList.parameters.size) return false
        return true
    }

    private fun IElementType.toPointType(): PointType =
        when (this) {
            PyTokenTypes.OR_KEYWORD -> PointType.LOGICAL_OR

            PyTokenTypes.AND_KEYWORD -> PointType.LOGICAL_AND

            else -> PointType.UNKNOWN
        }
}

fun PsiElement.findCurrentPythonMethod(): PyFunction? {
    var element: PsiElement? = this
    while (element != null && element !is PyFunction) element = element.parent
    return element?.let { it as PyFunction }
}
