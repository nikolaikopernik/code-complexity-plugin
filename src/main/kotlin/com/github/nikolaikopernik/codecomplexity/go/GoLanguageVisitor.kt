package com.github.nikolaikopernik.codecomplexity.go

import com.github.nikolaikopernik.codecomplexity.core.ComplexitySink
import com.github.nikolaikopernik.codecomplexity.core.ElementVisitor
import com.github.nikolaikopernik.codecomplexity.core.PointType
import com.goide.inspections.GoInspectionUtil
import com.goide.psi.GoAndExpr
import com.goide.psi.GoAssignmentStatement
import com.goide.psi.GoBinaryExpr
import com.goide.psi.GoBlock
import com.goide.psi.GoBreakStatement
import com.goide.psi.GoCallExpr
import com.goide.psi.GoCallLikeExpr
import com.goide.psi.GoConditionalExpr
import com.goide.psi.GoContinueStatement
import com.goide.psi.GoDeferStatement
import com.goide.psi.GoElement
import com.goide.psi.GoElseStatement
import com.goide.psi.GoFile
import com.goide.psi.GoForStatement
import com.goide.psi.GoFunctionDeclaration
import com.goide.psi.GoFunctionLit
import com.goide.psi.GoFunctionOrMethodDeclaration
import com.goide.psi.GoGoStatement
import com.goide.psi.GoIfStatement
import com.goide.psi.GoLabeledStatement
import com.goide.psi.GoLazyBlock
import com.goide.psi.GoLeftHandExprList
import com.goide.psi.GoMethodDeclaration
import com.goide.psi.GoOrExpr
import com.goide.psi.GoParenthesesExpr
import com.goide.psi.GoReferenceExpression
import com.goide.psi.GoReturnStatement
import com.goide.psi.GoSelectStatement
import com.goide.psi.GoSimpleStatement
import com.goide.psi.GoStatement
import com.goide.psi.GoSwitchStart
import com.goide.psi.GoSwitchStatement
import com.goide.psi.GoUnaryExpr
import com.goide.psi.GoValue
import com.goide.psi.GoVarDeclaration
import com.goide.psi.GoVarSpec
import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

/**
 * Represents a type alias used to pair a [GoStatement] and a [PsiElement].
 * This combination is utilized in the context of analyzing Go language code
 * within a PSI (Program Structure Interface) tree to evaluate complexity or
 * navigate code structure.
 */
private typealias PsiUnit = Pair<GoStatement, PsiElement>

/**
 * A type alias representing a pair of a `GoStatement` and an `ArrayDeque` of `PsiUnit`.
 *
 * The first component, `GoStatement`, typically serves as the root statement or context
 * for a given operation or computation. The second component, an `ArrayDeque` of `PsiUnit`,
 * stores a sequence of elements (e.g., PSI nodes such as statements or expressions) that
 * can be processed in a stack-like manner.
 *
 * This type alias is primarily used to simplify and streamline operations involving
 * traversals or manipulations of PSI tree structures in the context of Go code complexity
 * analysis. It supports various utility functions that enable operations like adding,
 * removing, and processing elements within the deque while keeping track of associated
 * context or root statements.
 */
private typealias PsiUnitDeque = Pair<GoStatement, ArrayDeque<PsiUnit>>

/**
 * Adds the given element to the end of the deque.
 *
 * @param element The PsiElement to be added to the end of the deque.
 */
private fun PsiUnitDeque.addLast(element: PsiElement) {
    val root = this.first
    this.second.addLast(root to element)
}

/**
 * Adds the child elements of the given PsiElement to the deque in reverse order.
 *
 * @param element the PsiElement whose child elements should be added to the deque
 */
fun PsiUnitDeque.addChildren(element: PsiElement) {
    val children = element.children
    children.reversed().forEach { addLast(it) }
}

private fun PsiUnitDeque.isEmpty(): Boolean = this.second.isEmpty()

private fun PsiUnitDeque.removeLast(): PsiUnit = this.second.removeLast()

/**
 * A visitor class for analyzing and processing elements of Go programming language syntax trees.
 * This class traverses the syntax tree of Go code, identifies specific constructs,
 * and evaluates their impact on code complexity and nesting levels.
 *
 * @constructor
 * @param sink an instance of [ComplexitySink] used to track and accumulate complexity and nesting levels.
 */
class GoLanguageVisitor(private val sink: ComplexitySink) : ElementVisitor() {

    override fun processElement(element: PsiElement) {
        when (element) {
            is GoForStatement -> sink.increaseComplexityAndNesting(PointType.LOOP_FOR)
            is GoIfStatement -> {
                if (element.parent !is GoElseStatement) {
                    sink.increaseComplexityAndNesting(PointType.IF)
                }
            }

            is GoElseStatement -> {
                sink.decreaseNesting()
                sink.increaseComplexity(1, PointType.ELSE)
                sink.increaseNesting()
            }

            is GoSelectStatement, is GoSwitchStatement -> sink.increaseComplexityAndNesting(element.pointType())

            is GoCallExpr -> {
                // ignore built-in function call
                if (element.isBuiltInFunction()) return
                if (element.isRecursionCall()) sink.increaseComplexity(PointType.RECURSION)
            }

            is GoFunctionLit -> when {
                // Ignore function literals in struct member or in top level var declaration function.
                element.isMemberFunction() || element.isToplevelVarDeclFunction() -> return
                else -> sink.increaseNesting()
            }

            is GoBreakStatement -> {
                element.labelRef ?: return
                sink.increaseComplexity(PointType.BREAK)
            }

            is GoContinueStatement -> {
                val labelRef = element.labelRef
                if (labelRef != null) {
                    sink.increaseComplexity(PointType.CONTINUE)
                }
            }

            is GoStatement -> {
                val parent = element.parent
                when {
                    // calculate a combination of logical operators in if statements
                    element is GoSimpleStatement && parent is GoIfStatement -> calculateIfStatementOperators(parent, element)
                    // calculate a combination of logical operators
                    element is GoSimpleStatement && element.hasChild<GoVarSpec>() -> calculateIfStatementOperators(element, element)
                    // calculate a combination of logical operators in assignment statements
                    element is GoAssignmentStatement && (element.hasChild<GoAndExpr>() || element.hasChild<GoOrExpr>()) -> calculateIfStatementOperators(element, element)
                    // calculate a combination of logical operators in return statements
                    element is GoReturnStatement && (element.hasChild<GoAndExpr>() || element.hasChild<GoOrExpr>()) -> calculateIfStatementOperators(element, element)
                }
            }
        }
    }

    /**
     * Recursively calculates complexity points for "if" statements in Go source code
     * by traversing the abstract syntax tree (AST) using a deque. The method examines
     * boolean logical operations like `&&` and `||`, and other related conditions
     * to increase complexity points appropriately.
     *
     * @param root the starting `GoStatement` element representing the root of the `if` statement or
     *             logical expression from which the calculation begins.
     * @param statement the specific `GoStatement` element that will be processed to determine
     *                  its role in contributing to overall complexity.
     */
    fun calculateIfStatementOperators(root: GoStatement, statement: GoStatement) {
        val deque = root to ArrayDeque<PsiUnit>()
        deque.addChildren(statement)
        deque.calculateIfStatementOperators()
    }

    /**
     * Recursively calculates complexity points for "if" statements in Go source code by traversing
     * a deque of PSI elements. The method processes boolean logical operations like `&&` and `||`,
     * parentheses, and conditions to appropriately increase complexity points.
     *
     * @param foundRoot a flag indicating whether the root condition of an `if` statement or a
     * related logical expression (such as `&&` or `||`) has been successfully located and processed.
     * Default is `false`.
     */
    tailrec fun PsiUnitDeque.calculateIfStatementOperators(foundRoot: Boolean = false) {
        if (isEmpty()) {
            return
        }
        val (root, element) = removeLast()
        val parent = element.parent
        var newFoundRoot = foundRoot
        if (element is GoAndExpr || element is GoOrExpr) {
            val parentCondition = parent?.findParentConditionExpr(element)
            if (!foundRoot && (parentCondition == root || parentCondition is GoAndExpr || parentCondition is GoOrExpr)) {
                sink.increaseComplexity(element.pointType())
                newFoundRoot = true
            } else if (parentCondition != null) {
                sink.increaseComplexity(element.pointType())
            }
        } else if (element is GoParenthesesExpr) {
            val next = element.getNextExpr()
            if (next != null && next.parent != null) {
                val parentCondition = next.parent.findParentConditionExpr(next)
                val pointType = parentCondition?.pointType() ?: element.pointType()
                sink.increaseComplexity(pointType)
            }
        }
        addChildren(element)
        calculateIfStatementOperators(newFoundRoot)
    }

    override fun postProcess(element: PsiElement) {
        when (element) {
            is GoForStatement,
            is GoIfStatement,
            is GoSwitchStatement,
            is GoSelectStatement,
            is GoFunctionLit -> sink.decreaseNesting()
        }
    }

    override fun shouldVisitElement(element: PsiElement): Boolean {
        return element is GoFunctionDeclaration ||
            element is GoMethodDeclaration ||
            element is GoBlock ||
            element is GoIfStatement ||
            element is GoElseStatement ||
            element is GoForStatement ||
            element is GoLabeledStatement ||
            element is GoReturnStatement ||
            element is GoGoStatement ||
            element is GoDeferStatement ||
            (element is GoCallLikeExpr && element.parent is GoGoStatement) ||
            element is GoSelectStatement ||
            element is GoSwitchStatement ||
            element is GoCallExpr ||
            element is GoFunctionLit ||
            element is GoBinaryExpr ||
            (element is GoSimpleStatement && element.parent is GoIfStatement) ||
            (element is GoLeftHandExprList && element.parent is GoSimpleStatement) ||
            element is GoLazyBlock
    }

    companion object {

        /**
         * Determines if the current GoStatement instance contains a child element of the specified type.
         *
         * @return true if the GoStatement has a child element of type E; false otherwise.
         */
        inline fun <reified E : PsiElement> GoStatement.hasChild(): Boolean {
            return this.children.any { element -> element is E }
        }

        /**
         * A list of predefined names representing the Go language's built-in functions.
         *
         * These function names can be used to identify whether certain function calls
         * in Go code represent standard library operations provided by the language.
         *
         * The list includes commonly used functions, such as...
         *
         * - "print", "println": For printing output to the console.
         * - "len", "cap": For measuring length and capacity of collections.
         * - "make", "new": For creating slices, maps, and channels.
         * - Mathematical utilities like "min", "max", and "complex".
         * - Error handling and recovery utilities like "panic" and "recover".
         */
        val builtinFunctionNames: List<String> = listOf(
            "print", "println",
            "make", "new",
            "len", "cap",
            "delete", "copy",
            "complex", "imag", "real",
            "panic", "recover",
            "min", "max",
        )

        /**
         * Determines if the provided Go call expression corresponds to a built-in function.
         *
         * The function checks if the first child of type [GoReferenceExpression] in the
         * call expression matches any known built-in function names.
         *
         * @return true if the call expression represents a built-in function, false otherwise.
         */
        fun GoCallExpr.isBuiltInFunction(): Boolean {
            val text = this.children.firstOrNull { it is GoReferenceExpression }?.text
            return text in builtinFunctionNames
        }

        /**
         * Determines whether a given Go call expression is a recursive call.
         * A call is considered recursive if the function or method being called
         * is the same as the one that contains the call.
         *
         * @return true if the call is recursive, false otherwise
         */
        fun GoCallExpr.isRecursionCall(): Boolean {
            try {
                val resolvedSignatureOwner = GoInspectionUtil.resolveCall(this)
                val rootFunctionOrMethodDeclaration = this.findFunctionOrMethodDeclaration()
                return resolvedSignatureOwner == rootFunctionOrMethodDeclaration
            } catch (e: Throwable) {
                logger.warn("error while processing GoCallExpr: ${this.javaClass.simpleName}(${this.text})", e)
                return false
            }
        }

        /**
         * Recursively searches for the enclosing Go function or method declaration in the PSI (Program Structure Interface) tree.
         * The search continues upward through the tree until a `GoFunctionOrMethodDeclaration` is found or the root element is reached.
         *
         * @return the enclosing `GoFunctionOrMethodDeclaration` if found, or `null` if no such declaration exists.
         */
        tailrec fun PsiElement.findFunctionOrMethodDeclaration(): GoFunctionOrMethodDeclaration? {
            return when (this) {
                is GoFunctionOrMethodDeclaration -> this
                is GoFile, is PsiFile -> null
                else -> when (this.parent) {
                    null -> null
                    else -> this.parent.findFunctionOrMethodDeclaration()
                }
            }
        }

        /**
         * Determines the type of PSI element and maps it to the corresponding `PointType` enumeration value.
         *
         * This method checks the type of the current PSI element and assigns a `PointType` based on its
         * characteristics or role in a Go language program. If no specific type can be determined, the method
         * defaults to returning `PointType.UNKNOWN`.
         *
         * @return the `PointType` corresponding to the type of the current PSI element. Possible return
         * values include `PointType.LOGICAL_AND`, `PointType.LOGICAL_OR`, `PointType.IF`, `PointType.LOOP_FOR`,
         * `PointType.SWITCH`, `PointType.ELSE`, `PointType.BREAK`, `PointType.CONTINUE`, `PointType.METHOD`, and
         * `PointType.UNKNOWN`.
         */
        fun PsiElement.pointType(): PointType = when (this) {
            is GoAndExpr -> PointType.LOGICAL_AND
            is GoOrExpr -> PointType.LOGICAL_OR
            is GoIfStatement -> PointType.IF
            is GoForStatement -> PointType.LOOP_FOR
            is GoSwitchStatement -> PointType.SWITCH
            is GoSwitchStart -> PointType.SWITCH
            is GoSelectStatement -> PointType.SWITCH
            is GoElseStatement -> PointType.ELSE
            is GoBreakStatement -> PointType.BREAK
            is GoContinueStatement -> PointType.CONTINUE
            is GoCallExpr -> PointType.METHOD
            is GoCallLikeExpr -> PointType.METHOD
            is GoGoStatement -> PointType.METHOD
            is GoDeferStatement -> PointType.METHOD
            is GoParenthesesExpr -> {
                val candidate = children.filter { c ->
                    c is GoAndExpr ||
                        c is GoOrExpr ||
                        c is GoCallExpr ||
                        c is GoCallLikeExpr
                }.map { c -> c.pointType() }
                    .firstOrNull { c -> c != PointType.UNKNOWN }
                candidate ?: PointType.UNKNOWN
            }

            else -> PointType.UNKNOWN
        }


        /**
         * Recursively finds a parent condition or expression in the PSI tree that matches specific types.
         *
         * @param start the initial element used to avoid redundant processing of the same expression type.
         * @return the parent PsiElement if it matches the expected condition or expression types; otherwise, null.
         */
        tailrec fun PsiElement.findParentConditionExpr(start: PsiElement): PsiElement? {
            return when (this) {
                is GoIfStatement, is GoForStatement -> this // If or For
                is GoReturnStatement, is GoAssignmentStatement, is GoVarSpec -> this
                is GoAndExpr, is GoOrExpr, is GoConditionalExpr -> {
                    if (this.javaClass == start.javaClass) {
                        null
                    } else {
                        this
                    }
                }

                is GoParenthesesExpr -> when (start) {
                    is GoAndExpr, is GoOrExpr, is GoConditionalExpr -> this
                    else -> null
                }

                else -> when (val parent = this.parent) {
                    null -> null
                    else -> parent.findParentConditionExpr(start)
                }
            }
        }

        /**
         * Recursively retrieves the next relevant expression in the PSI tree based on
         * specific patterns of Go expression elements ([GoAndExpr], [GoOrExpr], [GoUnaryExpr]).
         * This method traverses through the parent-child relationships in the PSI tree
         * to locate the proper next expression.
         *
         * @return the next expression as a PsiElement if it exists; otherwise, null if no next
         * expression can be determined or the traversal ends.
         */
        // GoAndExpr(=&&)[GoExpr(left=THIS),GoExpr(right)] -> GoExpr(right)
        // GoOrExpr(=||)[GoAndExpr[GoExpr(1-1),GoExpr(1-2=THIS)],GoExpr(2)] -> GoExpr(2)
        // GoAndExpr[GoUnaryExpr(=!)[GoExpr(THIS)],GoExpr(right)] -> GoExpr(right)
        tailrec fun PsiElement.getNextExpr(): PsiElement? {
            return when (val parent = this.parent) {
                is GoAndExpr, is GoOrExpr -> when (val next = this.next()) {
                    null -> parent.next()
                    else -> next
                }

                is GoUnaryExpr -> parent.getNextExpr()
                else -> null
            }
        }

        /**
         * Retrieves the next sibling element in the PSI tree that follows this element
         * within the same parent. The elements are evaluated in the order of their
         * appearance as children of the parent.
         *
         * @return the next sibling element if it exists; otherwise, null if this is
         *         the last child or if the parent is null.
         */
        fun PsiElement.next(): PsiElement? {
            val parent = this.parent ?: return null
            val children = parent.children
            for ((index, element) in children.withIndex()) {
                if (element == this && index < children.lastIndex) {
                    return children[index + 1]
                }
            }
            return null
        }

        /**
         * Executes a specified operation on the companion object and returns the result.
         *
         * @param operation A lambda function that defines the operation to be executed on the companion object.
         * @return The result of the operation performed on the companion object.
         */
        fun <T : Any> util(operation: Companion.() -> T): T = operation()

        /**
         * Determines whether the current function literal is a member function.
         *
         * A function literal is considered a member function if its direct parent
         * is a `GoValue` and the parent's parent is a `GoElement`.
         *
         * [GoFunctionLit] -<parent>-> [GoValue] -<parent>-> [GoElement]
         *
         * @return `true` if the function literal is a member function, otherwise `false`
         */
        fun GoFunctionLit.isMemberFunction(): Boolean {
            val value = this.parent as? GoValue ?: return false
            return value.parent is GoElement
        }

        /**
         * Determines if the function literal is declared as part of a top-level variable declaration.
         * A function literal is considered to be part of a top-level variable declaration if its
         * ancestry chain includes the following elements in order:
         *
         * [GoFunctionLit] -<parent>-> [GoVarSpec] -<parent>-> [GoVarDeclaration] -<parent>-> [GoFile]
         *
         * @return true if the function literal is part of a top-level variable declaration, false otherwise
         */
        fun GoFunctionLit.isToplevelVarDeclFunction(): Boolean {
            val varSpec = this.parent as? GoVarSpec ?: return false
            val declaration = varSpec.parent as? GoVarDeclaration ?: return false
            return declaration.parent is GoFile
        }

        val logger = Logger.getInstance(GoLanguageVisitor::class.java.simpleName)
    }
}
