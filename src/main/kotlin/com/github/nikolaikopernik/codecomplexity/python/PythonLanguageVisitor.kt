package com.github.nikolaikopernik.codecomplexity.python

import com.github.nikolaikopernik.codecomplexity.core.ComplexitySink
import com.github.nikolaikopernik.codecomplexity.core.ElementVisitor
import com.github.nikolaikopernik.codecomplexity.core.PointType
import com.intellij.psi.PsiElement
import com.jetbrains.python.psi.PyBinaryExpression
import com.jetbrains.python.psi.PyBreakStatement
import com.jetbrains.python.psi.PyContinueStatement
import com.jetbrains.python.psi.PyElsePart
import com.jetbrains.python.psi.PyForStatement
import com.jetbrains.python.psi.PyIfStatement
import com.jetbrains.python.psi.PyLambdaExpression
import com.jetbrains.python.psi.PyTryExceptStatement
import com.jetbrains.python.psi.PyWhileStatement
import org.jetbrains.kotlin.psi.KtContainerNodeForControlStructureBody
import org.jetbrains.kotlin.psi.KtIfExpression

internal class PythonLanguageVisitor(private val sink: ComplexitySink) : ElementVisitor() {
    override fun processElement(element: PsiElement) {
        when (element) {
            is PyWhileStatement -> sink.increaseComplexityAndNesting(PointType.LOOP_WHILE)
            is PyIfStatement -> processIfExpression(element)
            is PyElsePart ->  sink.increaseComplexity(PointType.ELSE)
            is PyForStatement -> sink.increaseComplexityAndNesting(PointType.LOOP_FOR)
            is PyTryExceptStatement -> sink.increaseComplexityAndNesting(PointType.CATCH)
            is PyBreakStatement -> if (element.loopStatement != null) sink.increaseComplexity(PointType.BREAK)
            is PyContinueStatement -> if (element.loopStatement != null) sink.increaseComplexity(PointType.CONTINUE)
            is PyLambdaExpression -> sink.increaseNesting()
            is PyBinaryExpression -> {
//                if (element.parent is KtStatementExpression || element.parent !is KtExpression) {
//                    element.calculateLogicalComplexity()
//                }
            }

//            is KtElement -> if (isRecursiveCall(element)) sink.increaseComplexity(PointType.RECURSION)
        }
    }

    override fun postProcess(element: PsiElement) {
        if (element is PyWhileStatement ||
            element is PyIfStatement ||
            element is PyForStatement ||
            element is PyTryExceptStatement ||
            element is PyLambdaExpression) {
            sink.decreaseNesting()
        }
    }

    override fun shouldVisitElement(element: PsiElement): Boolean = true


    private fun processIfExpression(element: PyIfStatement) {
        sink.increaseComplexityAndNesting(PointType.IF)
    }
}
