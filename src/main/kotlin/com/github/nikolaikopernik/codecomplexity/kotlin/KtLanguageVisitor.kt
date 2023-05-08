package com.github.nikolaikopernik.codecomplexity.kotlin

import com.github.nikolaikopernik.codecomplexity.core.ComplexitySink
import com.github.nikolaikopernik.codecomplexity.core.ElementVisitor
import com.github.nikolaikopernik.codecomplexity.core.PointType
import com.intellij.psi.NavigatablePsiElement
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.TokenSet
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.inspections.RecursivePropertyAccessorInspection
import org.jetbrains.kotlin.idea.util.getReceiverTargetDescriptor
import org.jetbrains.kotlin.lexer.KtToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.inline.InlineUtil
import org.jetbrains.kotlin.resolve.scopes.receivers.Receiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.types.expressions.OperatorConventions
import org.jetbrains.kotlin.util.OperatorNameConventions

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

            is KtElement -> if (isRecursiveCall(element)) sink.increaseComplexity(PointType.RECURSION)
        }
    }

    override fun postProcess(element: PsiElement) {
        if ((element is KtWhileExpression) ||
            (element is KtWhenExpression) ||
            (element is KtDoWhileExpression) ||
            ((element is KtIfExpression) && (element.`else` !is KtIfExpression)) ||
            (element is KtForExpression) ||
            (element is KtCatchClause) ||
            (element is KtLambdaExpression)
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

    private fun getNegationOperationToken(): KtToken {
        return KtTokens.EXCL
    }

    private fun getTempNegOperationToken(): KtToken {
        return KtTokens.QUEST
    }

    private fun isRecursiveCall(element: KtElement): Boolean {
        if (RecursivePropertyAccessorInspection.isRecursivePropertyAccess(element, false)) return true
        if (RecursivePropertyAccessorInspection.isRecursiveSyntheticPropertyAccess(element)) return true
        // Fast check for names without resolve
        val resolveName = getCallNameFromPsi(element) ?: return false
        val enclosingFunction = getEnclosingFunction(element, false) ?: return false

        val enclosingFunctionName = enclosingFunction.name
        if (enclosingFunctionName != OperatorNameConventions.INVOKE.asString()
            && enclosingFunctionName != resolveName.asString()
        ) return false

        // Check that there were no not-inlined lambdas on the way to enclosing function
        if (enclosingFunction != getEnclosingFunction(element, true)) return false

        val bindingContext = element.analyze()
        val enclosingFunctionDescriptor =
            bindingContext[BindingContext.FUNCTION, enclosingFunction] ?: return false

        val call = bindingContext[BindingContext.CALL, element] ?: return false
        val resolvedCall = bindingContext[BindingContext.RESOLVED_CALL, call] ?: return false

        if (resolvedCall.candidateDescriptor.original != enclosingFunctionDescriptor) return false

        fun isDifferentReceiver(receiver: Receiver?): Boolean {
            if (receiver !is ReceiverValue) return false

            val receiverOwner = receiver.getReceiverTargetDescriptor(bindingContext) ?: return true

            return when (receiverOwner) {
                is SimpleFunctionDescriptor -> receiverOwner != enclosingFunctionDescriptor
                is ClassDescriptor -> receiverOwner != enclosingFunctionDescriptor.containingDeclaration
                else -> return true
            }
        }

        return !isDifferentReceiver(resolvedCall.dispatchReceiver)
    }

    private fun getEnclosingFunction(element: NavigatablePsiElement,
                                     stopOnNonInlinedLambdas: Boolean): KtNamedFunction? {
        for (parent in element.parents) {
            when (parent) {
                is KtFunctionLiteral -> if (stopOnNonInlinedLambdas && !InlineUtil.isInlinedArgument(
                                parent,
                                parent.analyze(),
                                false
                        )
                ) return null

                is KtNamedFunction -> {
                    when (parent.parent) {
                        is KtBlockExpression, is KtClassBody, is KtFile, is KtScript -> return parent
                        else -> if (stopOnNonInlinedLambdas && !InlineUtil.isInlinedArgument(
                                        parent,
                                        parent.analyze(),
                                        false
                                )
                        ) return null
                    }
                }

                is KtClassOrObject -> return null
            }
        }
        return null
    }

    private fun getCallNameFromPsi(element: KtElement): Name? {
        when (element) {
            is KtSimpleNameExpression -> when (val elementParent = element.getParent()) {
                is KtCallExpression -> return Name.identifier(element.getText())
                is KtOperationExpression -> {
                    val operationReference = elementParent.operationReference
                    if (element == operationReference) {
                        val node = operationReference.getReferencedNameElementType()
                        return if (node is KtToken) {
                            val conventionName = if (elementParent is KtPrefixExpression)
                                OperatorConventions.getNameForOperationSymbol(node, true, false)
                            else
                                OperatorConventions.getNameForOperationSymbol(node)

                            conventionName ?: Name.identifier(element.getText())
                        } else {
                            Name.identifier(element.getText())
                        }
                    }
                }
            }

            is KtArrayAccessExpression -> return OperatorNameConventions.GET
            is KtThisExpression -> if (element.getParent() is KtCallExpression) return OperatorNameConventions.INVOKE
        }

        return null
    }
}

private fun KtToken.toPointType(): PointType =
    when (this) {
        KtTokens.ANDAND -> PointType.LOGICAL_AND
        KtTokens.OROR -> PointType.LOGICAL_OR
        else -> PointType.UNKNOWN
    }
