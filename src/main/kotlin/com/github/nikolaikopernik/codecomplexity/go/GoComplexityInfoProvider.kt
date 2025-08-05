package com.github.nikolaikopernik.codecomplexity.go

import com.github.nikolaikopernik.codecomplexity.core.ComplexityInfoProvider
import com.github.nikolaikopernik.codecomplexity.core.ComplexitySink
import com.github.nikolaikopernik.codecomplexity.core.ElementVisitor
import com.github.nikolaikopernik.codecomplexity.go.GoLanguageVisitor.Companion.isMemberFunction
import com.github.nikolaikopernik.codecomplexity.go.GoLanguageVisitor.Companion.isToplevelVarDeclFunction
import com.goide.GoLanguage
import com.goide.psi.GoElement
import com.goide.psi.GoFieldName
import com.goide.psi.GoFunctionLit
import com.goide.psi.GoFunctionOrMethodDeclaration
import com.goide.psi.GoKey
import com.goide.psi.GoValue
import com.goide.psi.GoVarDefinition
import com.goide.psi.GoVarSpec
import com.intellij.lang.Language
import com.intellij.psi.PsiElement

class GoComplexityInfoProvider(override val language: Language = GoLanguage.INSTANCE) : ComplexityInfoProvider {

    override fun getVisitor(sink: ComplexitySink): ElementVisitor = GoLanguageVisitor(sink)

    override fun isComplexitySuitableMember(element: PsiElement): Boolean {
        return element is GoFunctionOrMethodDeclaration || element is GoFunctionLit
    }

    override fun isClassWithBody(element: PsiElement): Boolean {
        return false
    }

    override fun getNameElementFor(element: PsiElement): PsiElement {
        return when (element) {
            is GoFunctionOrMethodDeclaration -> element.nameIdentifier ?: element
            is GoFunctionLit -> when {
                element.isMemberFunction() -> element.memberFieldName ?: element
                element.isToplevelVarDeclFunction() -> element.varDefinition ?: element
                else -> element
            }
            else -> element
        }
    }
}

/**
 * Retrieves the field name of a member function's associated field.
 *
 * This property is used to extract a field name corresponding to a member function within Go code.
 * It navigates the PSI (Program Structure Interface) tree, analyzing the parent hierarchy of the
 * `GoFunctionLit` instance and extracts the field name from the related `GoElement`.
 *
 * [GoFunctionLit] -<parent>-> [GoValue] -<parent>-> [GoElement] -<getKey>-> [GoKey] -<fieldName>-> [GoFieldName]
 *
 * If the field name cannot be resolved (e.g., the structure doesn't match the expected hierarchy),
 * the property returns `null`.
 *
 * @return The trimmed field name of the associated field, or `null` if unavailable.
 */
val GoFunctionLit.memberFieldName: GoFieldName?
    get() {
        val goElement = this.parent?.parent as? GoElement ?: return null
        return goElement.key?.fieldName
    }

val GoFunctionLit.memberFunctionFieldName: String?
    get() {
        val fieldName = this.memberFieldName ?: return null
        return fieldName.text
    }


/**
 * Retrieves the variable name associated with a Go function literal
 * if it is used in the context of a variable declaration.
 *
 * This property queries the parent element of the function literal to determine if it is
 * part of a `GoVarSpec`. If the parent is a variable specification, it extracts the first
 * variable name from the list of variable definitions.
 *
 * [GoFunctionLit] -<parent>-> [GoVarSpec]
 *
 * @return The name of the variable associated with the function literal, or `null` if
 * no variable declaration is found or the name cannot be determined.
 */
val GoFunctionLit.varDefinition: GoVarDefinition?
    get() {
        val goVarSpec = this.parent as? GoVarSpec ?: return null
        return goVarSpec.varDefinitionList.firstOrNull()
    }

val GoFunctionLit.varFunctionVariableName: String?
    get() {
        val first = this.varDefinition ?: return null
        return first.name
    }
