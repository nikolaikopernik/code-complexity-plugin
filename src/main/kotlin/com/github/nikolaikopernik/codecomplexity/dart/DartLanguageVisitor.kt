package com.github.nikolaikopernik.codecomplexity.dart

import com.github.nikolaikopernik.codecomplexity.core.ComplexitySink
import com.github.nikolaikopernik.codecomplexity.core.ElementVisitor
import com.intellij.psi.PsiElement

internal class DartLanguageVisitor(private val sink: ComplexitySink) : ElementVisitor() {

    override fun processElement(element: PsiElement) {
        // TODO: map Dart PSI nodes to complexity points. Reference Campbell rules
        // and the existing visitors (JavaLanguageVisitor, PythonLanguageVisitor).
        //
        // Core nodes to handle (from com.jetbrains.lang.dart.psi.*):
        //   DartIfStatement, DartWhileStatement, DartDoWhileStatement,
        //   DartForStatement, DartSwitchStatement, DartTryStatement,
        //   DartCatchPart, DartBreakStatement, DartContinueStatement,
        //   DartTernaryExpression, DartFunctionExpression,
        //   DartLogicAndExpression, DartLogicOrExpression,
        //   DartCallExpression (recursion).
        //
        // Dart-specific cases worth checking against the Campbell paper:
        //   - null-coalescing `??` and `??=` (DartIfNullExpression) — treat as logical op
        //   - cascade `..` (DartCascadeReferenceExpression) — probably not complexity
        //   - async/sync generators (`async*`, `sync*`) — yield points
        //   - switch expressions (Dart 3) — distinct from DartSwitchStatement
        //   - pattern matching in switch (Dart 3)
        //   - extension/mixin declarations — likely not complexity-bearing themselves
    }

    override fun postProcess(element: PsiElement) {
        // TODO: decrease nesting for any element that increased it in processElement.
    }

    override fun shouldVisitElement(element: PsiElement): Boolean = true
}
