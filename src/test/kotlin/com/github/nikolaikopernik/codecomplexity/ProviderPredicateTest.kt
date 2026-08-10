package com.github.nikolaikopernik.codecomplexity

import com.github.nikolaikopernik.codecomplexity.dart.DartComplexityInfoProvider
import com.github.nikolaikopernik.codecomplexity.java.JavaComplexityInfoProvider
import com.github.nikolaikopernik.codecomplexity.kotlin.KtComplexityInfoProvider
import com.github.nikolaikopernik.codecomplexity.python.PythonComplexityInfoProvider
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassInitializer
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.jetbrains.lang.dart.psi.DartClassDefinition
import com.jetbrains.lang.dart.psi.DartGetterDeclaration
import com.jetbrains.lang.dart.psi.DartMethodDeclaration
import com.jetbrains.lang.dart.psi.DartNamedConstructorDeclaration
import com.jetbrains.python.psi.PyClass
import com.jetbrains.python.psi.PyFunction
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassInitializer
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.jetbrains.kotlin.psi.KtSecondaryConstructor

/**
 * Characterizes the provider predicates the inlay/inspection glue relies on
 * (isComplexitySuitableMember / isClassWithBody / getNameElementFor).
 * Go is covered through its language suite instead: direct tests would need its parser scaffolding.
 */
class ProviderPredicateTest : BasePlatformTestCase() {

    fun testJavaPredicates() {
        val provider = JavaComplexityInfoProvider()
        myFixture.configureByText("A.java", """
            class A {
                int field = 1;
                static { }
                void m() { }
            }
            class B { int x; }
        """.trimIndent())
        val method = PsiTreeUtil.findChildOfType(myFixture.file, PsiMethod::class.java)!!
        val initializer = PsiTreeUtil.findChildOfType(myFixture.file, PsiClassInitializer::class.java)!!
        val field = PsiTreeUtil.findChildOfType(myFixture.file, PsiField::class.java)!!
        val (classA, classB) = PsiTreeUtil.findChildrenOfType(myFixture.file, PsiClass::class.java).toList()

        assertTrue(provider.isComplexitySuitableMember(method))
        assertTrue(provider.isComplexitySuitableMember(initializer))
        assertFalse(provider.isComplexitySuitableMember(field))
        // with methods vs without
        assertTrue(provider.isClassWithBody(classA))
        assertFalse(provider.isClassWithBody(classB))
        assertEquals("m", provider.getNameElementFor(method).text)
        assertEquals("{", provider.getNameElementFor(initializer).text)
    }

    fun testKotlinPredicates() {
        val provider = KtComplexityInfoProvider()
        myFixture.configureByText("a.kt", """
            fun topLevel() {}

            class C {
                init { }
                constructor(x: Int) { }
                fun method() {}
                val p: Int
                    get() = 1
            }

            class D

            object Obj

            fun outer() {
                fun local() {}
                val lam = { }
            }
        """.trimIndent())
        val functions = PsiTreeUtil.findChildrenOfType(myFixture.file, KtNamedFunction::class.java).associateBy { it.name }
        val initializer = PsiTreeUtil.findChildOfType(myFixture.file, KtClassInitializer::class.java)!!
        val secondaryCtor = PsiTreeUtil.findChildOfType(myFixture.file, KtSecondaryConstructor::class.java)!!
        val accessor = PsiTreeUtil.findChildOfType(myFixture.file, KtPropertyAccessor::class.java)!!
        val lambda = PsiTreeUtil.findChildOfType(myFixture.file, KtLambdaExpression::class.java)!!
        val obj = PsiTreeUtil.findChildOfType(myFixture.file, KtObjectDeclaration::class.java)!!
        val (classC, classD) = PsiTreeUtil.findChildrenOfType(myFixture.file, KtClass::class.java).toList()

        assertTrue(provider.isComplexitySuitableMember(functions["topLevel"]!!))
        assertTrue(provider.isComplexitySuitableMember(functions["method"]!!))
        // local functions count as members of their own (unlike Python's nested exclusion)
        assertTrue(provider.isComplexitySuitableMember(functions["local"]!!))
        assertTrue(provider.isComplexitySuitableMember(initializer))
        assertTrue(provider.isComplexitySuitableMember(secondaryCtor))
        assertTrue(provider.isComplexitySuitableMember(accessor))
        assertTrue(provider.isComplexitySuitableMember(obj))
        assertFalse(provider.isComplexitySuitableMember(lambda))
        assertFalse(provider.isComplexitySuitableMember(classC))
        // any body counts, methods not required (unlike Java); no body -> false
        assertTrue(provider.isClassWithBody(classC))
        assertFalse(provider.isClassWithBody(classD))
        assertEquals("topLevel", provider.getNameElementFor(functions["topLevel"]!!).text)
        assertEquals("Obj", provider.getNameElementFor(obj).text)
        assertEquals("{", provider.getNameElementFor(initializer).text)
    }

    fun testPythonPredicates() {
        val provider = PythonComplexityInfoProvider()
        myFixture.configureByText("a.py", """
            def top():
                def nested():
                    pass
                return nested

            class C:
                def method(self):
                    pass

            class Empty:
                x = 1
        """.trimIndent())
        val functions = PsiTreeUtil.findChildrenOfType(myFixture.file, PyFunction::class.java).associateBy { it.name }
        val (classC, classEmpty) = PsiTreeUtil.findChildrenOfType(myFixture.file, PyClass::class.java).toList()

        assertTrue(provider.isComplexitySuitableMember(functions["top"]!!))
        assertTrue(provider.isComplexitySuitableMember(functions["method"]!!))
        // nested functions fold into their enclosing function
        assertFalse(provider.isComplexitySuitableMember(functions["nested"]!!))
        assertTrue(provider.isClassWithBody(classC))
        assertFalse(provider.isClassWithBody(classEmpty))
        assertEquals("top", provider.getNameElementFor(functions["top"]!!).text)
    }

    fun testDartPredicates() {
        val provider = DartComplexityInfoProvider()
        myFixture.configureByText("a.dart", """
            class A {
              int x = 0;
              A(this.x);
              A.trivial() { }
              A.busy() { x = 1; }
              int get g => 1;
              void m() { }
            }
            class B { int y = 0; }
        """.trimIndent())
        val plainCtor = PsiTreeUtil.findChildOfType(myFixture.file, DartMethodDeclaration::class.java)!!
        val namedCtors = PsiTreeUtil.findChildrenOfType(myFixture.file, DartNamedConstructorDeclaration::class.java).toList()
        val getter = PsiTreeUtil.findChildOfType(myFixture.file, DartGetterDeclaration::class.java)!!
        val method = PsiTreeUtil.findChildrenOfType(myFixture.file, DartMethodDeclaration::class.java)
            .first { it.componentName?.text == "m" }
        val (classA, classB) = PsiTreeUtil.findChildrenOfType(myFixture.file, DartClassDefinition::class.java).toList()

        assertTrue(provider.isComplexitySuitableMember(method))
        assertTrue(provider.isComplexitySuitableMember(getter))
        // constructors: plain never suitable, named only with a non-empty body
        assertFalse(provider.isComplexitySuitableMember(plainCtor))
        assertFalse(provider.isComplexitySuitableMember(namedCtors.first { it.componentName?.text == "trivial" }))
        assertTrue(provider.isComplexitySuitableMember(namedCtors.first { it.componentName?.text == "busy" }))
        assertTrue(provider.isClassWithBody(classA))
        assertFalse(provider.isClassWithBody(classB))
        assertEquals("m", provider.getNameElementFor(method).text)
    }
}
