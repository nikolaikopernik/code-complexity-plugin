package com.github.nikolaikopernik.codecomplexity.kotlin

import com.github.nikolaikopernik.codecomplexity.core.ComplexitySink
import com.github.nikolaikopernik.codecomplexity.core.ElementVisitor
import com.github.nikolaikopernik.codecomplexity.core.PointType
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.TokenSet
import org.jetbrains.kotlin.lexer.KtToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtBreakExpression
import org.jetbrains.kotlin.psi.KtCatchClause
import org.jetbrains.kotlin.psi.KtContainerNodeForControlStructureBody
import org.jetbrains.kotlin.psi.KtContinueExpression
import org.jetbrains.kotlin.psi.KtDoWhileExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtForExpression
import org.jetbrains.kotlin.psi.KtIfExpression
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtOperationReferenceExpression
import org.jetbrains.kotlin.psi.KtParenthesizedExpression
import org.jetbrains.kotlin.psi.KtPrefixExpression
import org.jetbrains.kotlin.psi.KtStatementExpression
import org.jetbrains.kotlin.psi.KtValueArgumentList
import org.jetbrains.kotlin.psi.KtWhenExpression
import org.jetbrains.kotlin.psi.KtWhileExpression

class KtLanguageVisitor(private val sink: ComplexitySink) : ElementVisitor() {
    override fun processElement(element: PsiElement) {
        when (element) {
            is KtWhileExpression -> sink.increaseComplexityAndNesting(PointType.LOOP_WHILE)
            is KtDoWhileExpression -> sink.increaseComplexityAndNesting(PointType.LOOP_WHILE)
            is KtWhenExpression -> sink.increaseComplexityAndNesting(PointType.SWITCH)
            is KtIfExpression -> processIfExpression(element)
            // `else if`
            is KtContainerNodeForControlStructureBody -> {
                if ((element.expression is KtIfExpression) && (element.firstChild is KtIfExpression)) {
                    sink.decreaseNesting()
                }
            }

            is KtForExpression -> sink.increaseComplexityAndNesting(PointType.LOOP_FOR)
            is KtCatchClause -> sink.increaseComplexityAndNesting(PointType.CATCH)
            is KtBreakExpression -> if (element.labelQualifier != null) sink.increaseComplexity(PointType.BREAK)
            is KtContinueExpression -> if (element.labelQualifier != null) sink.increaseComplexity(PointType.CONTINUE)
            is KtLambdaExpression -> sink.increaseNesting()
            is KtBinaryExpression -> {
                if (element.parent is KtStatementExpression || element.parent !is KtExpression) {
                    element.calculateLogicalComplexity()
                }
            }

            is KtNameReferenceExpression -> if (isRecursiveCall(element)) sink.increaseComplexity(PointType.RECURSION)
        }
    }

    override fun postProcess(element: PsiElement) {
        if (element is KtWhileExpression ||
            element is KtWhenExpression ||
            element is KtDoWhileExpression ||
            element is KtForExpression ||
            element is KtCatchClause ||
            element is KtLambdaExpression ||
            element is KtIfExpression && element.`else` !is KtIfExpression
        ) {
            sink.decreaseNesting()
        }
    }

    override fun shouldVisitElement(element: PsiElement): Boolean = true


    private fun processIfExpression(element: KtIfExpression) {
        // if exists `else` that is not `else if`
        val ktExpression = element.`else`
        if (ktExpression != null && ktExpression !is KtIfExpression) {
            sink.increaseComplexity(PointType.ELSE)
        }

        val parent = element.parent
        if (parent is KtContainerNodeForControlStructureBody
            && parent.expression is KtIfExpression
        ) {
            sink.increaseNesting()
            sink.increaseComplexity(PointType.IF)
        } else {
            sink.increaseComplexityAndNesting(PointType.IF)
        }
    }

    private fun KtExpression.calculateLogicalComplexity(prevToken: KtToken? = null): KtToken? {
        var prevOperand = prevToken
        this.children.forEach { element ->
            when (element) {
                is KtOperationReferenceExpression -> if (element.operationSignTokenType != null && element.operationSignTokenType in (getLogicalOperationsTokens())) {
                    if (prevOperand == null || element.operationSignTokenType != prevOperand) {
                        sink.increaseComplexity(element.operationSignTokenType!!.toPointType())
                    }
                    prevOperand = element.operationSignTokenType
                }

                is KtBinaryExpression -> prevOperand = element.calculateLogicalComplexity(prevOperand)
                is KtPrefixExpression -> prevOperand = element.calculateLogicalComplexity(prevOperand)
                is KtParenthesizedExpression -> {
                    element.calculateLogicalComplexity()
                    prevOperand = null
                }
            }
        }
        return prevOperand
    }

    private fun getLogicalOperationsTokens(): TokenSet {
        return TokenSet.create(
            KtTokens.ANDAND,
            KtTokens.OROR
        )
    }

    /**
     * Checking if recursion is used.
     * Have to do it fast and dirty as it should be fast to avoid exception in IDEA.
     * Basically we check:
     *  - if the found reference expression name matches the direct parent method name. This code won't detect recursion
     *    if more than one method involved (method A calling B and then B calling A)
     *  - if the number of arguments matches the number of parameters
     *
     *  Possible improvements:
     *   - check parameter types as well
     */
    private fun isRecursiveCall(element: KtNameReferenceExpression): Boolean {
        val argumentList = element.nextSibling
        if (argumentList is KtValueArgumentList){
            val parentMethod: KtNamedFunction = element.findCurrentMethod() ?: return false
            if (element.getReferencedName() != parentMethod.nameIdentifier?.text) return false
            if (argumentList.arguments.size != parentMethod.valueParameterList?.parameters?.size) return false
            return true
        }
        return false
    }
}

private fun KtToken.toPointType(): PointType =
    when (this) {
        KtTokens.ANDAND -> PointType.LOGICAL_AND
        KtTokens.OROR -> PointType.LOGICAL_OR
        else -> PointType.UNKNOWN
    }

private fun PsiElement.findCurrentMethod(): KtNamedFunction? {
    var element: PsiElement? = this
    while (element != null && element !is KtNamedFunction) element = element.parent
    return element?.let { it as KtNamedFunction }
}
