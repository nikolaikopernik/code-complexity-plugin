package com.github.nikolaikopernik.codecomplexity

import com.github.nikolaikopernik.codecomplexity.core.ComplexitySink
import com.github.nikolaikopernik.codecomplexity.core.ElementVisitor
import com.github.nikolaikopernik.codecomplexity.go.GoComplexityInfoProvider
import com.github.nikolaikopernik.codecomplexity.go.GoLanguageVisitor
import com.github.nikolaikopernik.codecomplexity.go.memberFunctionFieldName
import com.github.nikolaikopernik.codecomplexity.go.varFunctionVariableName
import com.goide.GoElementTypeFactorySupplierImpl
import com.goide.GoFileType
import com.goide.GoLanguage
import com.goide.project.GoModuleSettings
import com.goide.project.GoPackageFactory
import com.goide.psi.GoElement
import com.goide.psi.GoFile
import com.goide.psi.GoFunctionDeclaration
import com.goide.psi.GoFunctionLit
import com.goide.psi.GoFunctionOrMethodDeclaration
import com.goide.psi.GoImportList
import com.goide.psi.GoLiteral
import com.goide.psi.GoMethodDeclaration
import com.goide.psi.GoPackageClause
import com.goide.psi.GoPsiTreeChangeProcessor
import com.goide.psi.GoStringLiteral
import com.goide.psi.GoTypeDeclaration
import com.goide.psi.GoValue
import com.goide.psi.GoVarDeclaration
import com.goide.psi.GoVarSpec
import com.goide.psi.impl.GoPackage
import com.goide.psi.impl.GoStringLiteralImpl
import com.goide.psi.impl.manipulator.GoStringManipulator
import com.goide.sdk.GoBasedSdk
import com.goide.sdk.GoBasedSdkVetoer
import com.goide.stubs.index.GoAllPrivateNamesIndex
import com.goide.stubs.index.GoAllPublicNamesIndex
import com.goide.stubs.index.GoFunctionIndex
import com.goide.stubs.index.GoMethodFingerprintIndex
import com.goide.stubs.index.GoMethodIndex
import com.goide.stubs.index.GoMethodSpecFingerprintIndex
import com.goide.stubs.index.GoMethodSpecInheritanceIndex
import com.goide.stubs.index.GoNonPackageLevelNamesIndex
import com.goide.stubs.index.GoPackageLevelPublicElementsIndex
import com.goide.stubs.index.GoPackagesIndex
import com.goide.stubs.index.GoTypeAliasIndex
import com.goide.stubs.index.GoTypeSpecInheritanceIndex
import com.goide.stubs.index.GoTypesIndex
import com.intellij.go.backend.GoBackendParserDefinition
import com.intellij.go.frontback.api.GoElementTypeFactorySupplier
import com.intellij.lang.LanguageParserDefinitions
import com.intellij.openapi.application.Application
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.extensions.ExtensionPoint
import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.extensions.ExtensionsArea
import com.intellij.openapi.fileTypes.ExtensionFileNameMatcher
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ModificationTracker
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.ElementManipulators
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.PsiTreeChangePreprocessor
import com.intellij.psi.impl.file.PsiDirectoryFactory
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StringStubIndexExtension
import com.intellij.psi.stubs.StubIndex
import com.intellij.psi.stubs.StubIndexKey
import com.intellij.psi.tree.StubFileElementType
import com.intellij.psi.util.childrenOfType
import com.intellij.psi.util.descendants
import com.intellij.testFramework.DisposableRule
import com.intellij.testFramework.registerServiceInstance
import com.intellij.testFramework.unregisterService
import com.intellij.util.Processor
import com.intellij.util.indexing.IdFilter
import org.jdom.Element
import org.jetbrains.annotations.NonNls
import org.junit.Rule
import org.junit.Test

private const val GO_TEST_FILES_PATH = "src/test/testData/go"

class GoComplexityCalculationTest : BaseComplexityTest() {

    val goParserDefinition = GoBackendParserDefinition()

    @get:Rule
    val disposableRule = DisposableRule()

    @Suppress("UnstableApiUsage")
    override fun setUp() {
        super.setUp()
        Registry.getInstance().reset()
        Registry.loadState(registry {
            // to avoid test logger to fail
            "caches.scanningThreadsCount"("1")
            "use.dependencies.cache.service"("false")
            // to enable GoFile constructor to work
            "go.light.ast.stubs.enabled"("true")
            "go.qualified.cache.enabled"("true")
            "go.resolve.cache.enabled"("false")
            "go.unqualified.cache.enabled"("false")
            "go.package.modification.tracker.enabled"("true")
        }, null)
        module.registerServiceInstance(GoModuleSettings::class.java, GoModuleSettings(module))
        LanguageParserDefinitions.INSTANCE.addExplicitExtension(GoLanguage.INSTANCE, goParserDefinition)
        val application = ApplicationManager.getApplication()
        application.runWriteAction {
            application.registerServiceInstance(GoElementTypeFactorySupplier::class.java, GoElementTypeFactorySupplierImpl())
            application.service<FileTypeManager>()?.associate(GoFileType.INSTANCE, ExtensionFileNameMatcher("go"))
        }
        // Add extensions/stub indices for the Go language feature to work
        @Suppress("DEPRECATION")
        val extensionsRoot = Extensions.getRootArea()
        extensionsRoot
            .registerExtensionPoint("com.goide.packageFactory", MockPackageFactory::class.java.canonicalName, ExtensionPoint.Kind.INTERFACE, true)
        extensionsRoot
            .registerExtensionPoint("com.goide.sdk.sdkVetoer", MockSdkVetoer::class.java.canonicalName, ExtensionPoint.Kind.INTERFACE, true)
        extensionsRoot.registerStubIndex(GoMethodIndex())
        extensionsRoot.registerStubIndex(GoPackagesIndex())
        extensionsRoot.registerStubIndex(GoAllPrivateNamesIndex())
        extensionsRoot.registerStubIndex(GoFunctionIndex())
        extensionsRoot.registerStubIndex(GoAllPublicNamesIndex())
        extensionsRoot.registerStubIndex(GoPackageLevelPublicElementsIndex())
        extensionsRoot.registerStubIndex(GoTypesIndex())
        extensionsRoot.registerStubIndex(GoTypeAliasIndex())
        extensionsRoot.registerStubIndex(GoNonPackageLevelNamesIndex())
        extensionsRoot.registerStubIndex(GoMethodFingerprintIndex())
        extensionsRoot.registerStubIndex(GoMethodSpecFingerprintIndex())
        extensionsRoot.registerStubIndex(GoMethodSpecInheritanceIndex())
        extensionsRoot.registerStubIndex(GoTypeSpecInheritanceIndex())

        ElementManipulators.INSTANCE.addExplicitExtension(GoStringLiteralImpl::class.java, GoStringManipulator(), disposableRule.disposable)
        application.register<StubIndex>(MockStubIndex())

        project.extensionArea.getExtensionPoint<PsiTreeChangePreprocessor>("com.intellij.psi.treeChangePreprocessor")
            .registerExtension(GoPsiTreeChangeProcessor(), disposableRule.disposable)
    }

    override fun tearDown() {
        LanguageParserDefinitions.INSTANCE.removeExplicitExtension(GoLanguage.INSTANCE, goParserDefinition)
        val application = ApplicationManager.getApplication()
        application.runWriteAction {
            application.unregisterService(GoElementTypeFactorySupplier::class.java)
            application.service<FileTypeManager>()
                ?.removeAssociation(GoFileType.INSTANCE, ExtensionFileNameMatcher("go"))
            module.unregisterService(GoModuleSettings::class.java)
        }
        super.tearDown()
    }

    @Suppress("JUnitMixedFramework")
    @Test
    fun testGoComplexity() {
        try {
            checkAllFilesInFolder(GO_TEST_FILES_PATH, ".go")
        } catch (e: NoClassDefFoundError) {
            if (e.message != "com/intellij/ultimate/UltimateVerifier") {
                throw e
            }
        }
    }

    override fun getTestDataPath() = GO_TEST_FILES_PATH

    override fun createLanguageElementVisitor(sink: ComplexitySink): ElementVisitor =
        GoComplexityInfoProvider().getVisitor(sink)

    override fun parseTestFile(file: PsiFile): List<Triple<PsiElement, String, Int>> {
        val functionOrMethods = file
            .childrenOfType<GoFunctionOrMethodDeclaration>()
            .asSequence()
            .mapNotNull { functionOrMethod -> functionOrMethod.convertToTest() }
        val memberFunctions = file
            .traverseVariablesOrMembersForTests("members.go")
            .filterIsInstance<GoFunctionLit>()
            .mapNotNull { literal ->
                when {
                    GoLanguageVisitor.util { literal.isMemberFunction() } -> literal.convertToMemberFunctionTest()
                    GoLanguageVisitor.util { literal.isToplevelVarDeclFunction() } -> literal.convertToVarFunctionTest()
                    else -> null
                }
            }
        return (memberFunctions + functionOrMethods).toList()
    }

    fun <P : PsiElement> ExtensionsArea.registerStubIndex(psi: StringStubIndexExtension<P>) {
        val exp = getExtensionPoint<StringStubIndexExtension<P>>("com.intellij.stubIndex")
        exp.registerExtension(psi, disposableRule.disposable)
    }

    private companion object {

        /**
         * Converts a [GoFunctionOrMethodDeclaration] into a testable representation if possible.
         *
         * This method extracts relevant information from a Go function or method declaration,
         * specifically the complexity and the function name, and formats them into a `Triple`.
         * The `Triple` contains the original PsiElement, the formatted function name with complexity,
         * and the complexity value.
         *
         * @return A `Triple` containing the PsiElement, formatted function name,
         *         and complexity value, or `null` if the complexity or function
         *         name is not available.
         */
        fun GoFunctionOrMethodDeclaration.convertToTest(): Triple<PsiElement, String, Int>? {
            val complexity = this.complexity ?: return null
            val functionName = this.name ?: return null
            return Triple(this, functionName.replace("()", "($complexity)"), complexity)
        }

        val GoFunctionOrMethodDeclaration.complexity: Int? get() = complexityForFunctionOrMethodDecl(this.prevSibling)

        /**
         * Retrieves the complexity for a given [PsiElement].
         *
         * This function examines the provided PSI (Program Structure Interface) element
         * recursively, checking for specific comments that define complexity. It navigates
         * through previous sibling elements if necessary to locate the relevant definition.
         *
         * @param element The PSI element to analyze. It may represent different components
         *                of a code structure, such as comments or function declarations.
         *                If null, the function will*/
        tailrec fun complexityForFunctionOrMethodDecl(element: PsiElement?): Int? = when (element) {
            null -> null
            is PsiComment -> {
                if (element.text == null) null
                else when (val ret = element.text.complexity()) {
                    null -> complexityForFunctionOrMethodDecl(element.prevSibling)
                    else -> ret
                }
            }

            is GoFunctionDeclaration,
            is GoImportList,
            is GoMethodDeclaration,
            is GoPackageClause,
            is GoTypeDeclaration,
            is GoVarDeclaration -> null

            else -> complexityForFunctionOrMethodDecl(element.prevSibling)
        }

        fun String.complexity(): Int? {
            val words = this.replaceFirst("//", "").trim().split(Regex("\\s+"))
            if (words.size != 3) return null
            if (words[0] != "go:generate") {
                return null
            }
            if (words[1] != "complexity") {
                return null
            }
            if (Regex("^[1-9][0-9]*$").matches(words[2])) return words[2].toInt()
            return null
        }

        fun PsiFile.traverseVariablesOrMembersForTests(vararg fileNames: String): Sequence<PsiElement> =
            if (this.name in fileNames) this.descendants { !(it is GoFunctionLit || it is GoLiteral || it is GoStringLiteral || it is GoFunctionOrMethodDeclaration) }
            else emptySequence()

        /**
         * Converts a [GoFunctionLit] instance into a member function test representation.
         *
         * The conversion is based on the member function's complexity and field name. If either the complexity
         * or the field name is not available, the function will return null.
         *
         * @return a [Triple] containing the [PsiElement] representing the function, the function name as a [String],
         *         and its complexity as an [Int], or null if the function cannot be converted.
         */
        fun GoFunctionLit.convertToMemberFunctionTest(): Triple<PsiElement, String, Int>? {
            val complexity = this.memberFunctionComplexity ?: return null
            val functionName = this.memberFunctionFieldName ?: return null
            return Triple(this, functionName, complexity)
        }

        /**
         * Finds a comment in the given PSI element or its ancestors.
         *
         * This function recursively traverses backward through the siblings of the element
         * until it encounters a [PsiComment] or reaches the root of the tree.
         *
         * @param elem the starting PSI element from which to search for a comment.
         *             Can be null, in which case the function immediately returns null.
         * @return the first `PsiComment` found while traversing the PSI tree,
         *         or null if no comment is found.
         */
        tailrec fun findComment(elem: PsiElement?): PsiComment? =
            when (elem) {
                null -> null
                is PsiComment -> elem
                else -> findComment(elem.prevSibling)
            }

        /**
         * Retrieves the complexity value from an associated comment.
         *
         * This property evaluates the complexity of the member function defined within a
         * `GoFunctionLit` instance. The complexity is retrieved from a special comment
         * associated with the function's parent element.
         *
         * [GoFunctionLit] -<parent>-> [GoValue] -<parent>-> [GoElement] -<prevSibling...(PsiWhiteSpace)>-> [PsiComment] -> complexity
         *
         * The expected format of the comment is:
         * ```text
         * // go:generate complexity <number>
         * ```
         * where `<number>` is a positive integer representing the complexity value.
         *
         * @return The parsed complexity value as an `Int`, or `null` if the comment is
         *         missing, improperly formatted, or contains an invalid complexity value.
         */
        val GoFunctionLit.memberFunctionComplexity: Int?
            get() {
                val goElement = this.parent?.parent as? GoElement ?: return null
                val comment = findComment(goElement) ?: return null
                return comment.text?.complexity()
            }

        /**
         * Converts a `GoFunctionLit` instance into a testable representation if it satisfies certain conditions.
         *
         * The conditions for conversion are:
         * - The variable function complexity is not null.
         * - The variable function has a defined variable name.
         *
         * @return A Triple containing the `GoFunctionLit` element, the variable function's name, and its complexity,
         * or null if the required conditions are not satisfied.
         */
        fun GoFunctionLit.convertToVarFunctionTest(): Triple<PsiElement, String, Int>? {
            val complexity = this.varFunctionComplexity ?: return null
            val functionName = this.varFunctionVariableName ?: return null
            return Triple(this, functionName, complexity)
        }

        /**
         * Retrieves the complexity value from a comment
         * associated with the variable declaration containing the function literal
         * (if such a comment exists and is properly formatted).
         *
         * Returns:
         * - The integer value of the complexity if a valid comment with a specified complexity
         *   is found for the variable declaration.
         * - `null` if no valid complexity comment is found, or the associated variable
         *   declaration does not exist.
         *
         * [GoFunctionLit] -<parent>-> [GoVarSpec] -<prevSibling...(PsiWhiteSpace)>-> [PsiComment] -> complexity
         *
         * The expected format of the comment is:
         * ```text
         * // go:generate complexity <number>
         * ```
         * where `<number>` is a positive integer representing the complexity value.
         */
        val GoFunctionLit.varFunctionComplexity: Int?
            get() {
                val varSpec = this.parent as? GoVarSpec ?: return null
                val comment = findComment(varSpec) ?: return null
                return comment.text?.complexity()
            }

        /**
         * Represents an element container for constructing and managing XML-like registry
         * configurations, allowing for content to be dynamically augmented.
         *
         * @property element The underlying XML element that serves as the base container for registry entries.
         */
        class RegistryElement(private val element: Element) {
            operator fun String.invoke(value: String) {
                val entry = Element("entry")
                entry.setAttribute("key", this)
                entry.setAttribute("value", value)
                element.addContent(entry)
            }
        }

        /**
         * Configures and constructs a registry element using the provided configuration lambda.
         *
         * @param configure A lambda with receiver of type `RegistryElement` that defines the configuration
         * of the registry element.
         * @return An `Element` instance representing the configured registry.
         */
        fun registry(configure: RegistryElement.() -> Unit): Element {
            return Element("registry").apply {
                RegistryElement(this).configure()
            }
        }

        inline fun <reified T : Any> Application.service(): T? {
            return getService(T::class.java)
        }

        inline fun <reified T : Any> Application.register(instance: T) {
            registerServiceInstance(T::class.java, instance)
        }
    }
}

private class MockPackageFactory : GoPackageFactory {
    override fun createPackage(goFile: GoFile): GoPackage? =
        when (val parent = goFile.parent) {
            null -> GoPackage.`in`(PsiDirectoryFactory.getInstance(goFile.project)
                                       .createDirectory(goFile.virtualFile.parent), "mock")

            else -> GoPackage.`in`(parent, "mock")
        }

    override fun createPackage(name: String,
                               vararg directories: PsiDirectory?): GoPackage =
        directories.firstNotNullOf { it }.let { GoPackage.`in`(it, name) }
}

@Suppress("UnstableApiUsage")
private class MockStubIndex : StubIndex() {

    override fun <Key : Any?, Psi : PsiElement?> processElements(
        indexKey: StubIndexKey<Key?, Psi?>,
        key: Key & Any,
        project: Project,
        scope: GlobalSearchScope?,
        idFilter: IdFilter?,
        requiredClass: Class<Psi?>,
        processor: Processor<in Psi>): Boolean = true

    override fun <Key : Any?> getAllKeys(p0: StubIndexKey<Key?, *>, p1: Project): Collection<Key?> =
        TODO("Not yet implemented")

    override fun <Key : Any?> getContainingFilesIterator(p0: StubIndexKey<Key?, *>,
                                                         p1: @NonNls Key & Any,
                                                         p2: Project,
                                                         p3: GlobalSearchScope): Iterator<VirtualFile?> =
        TODO("Not yet implemented")

    override fun <Key : Any?> getMaxContainingFileCount(
        p0: StubIndexKey<Key?, *>,
        p1: @NonNls Key & Any,
        p2: Project,
        p3: GlobalSearchScope): Int = TODO("Not yet implemented")

    override fun forceRebuild(p0: Throwable) = TODO("Not yet implemented")

    override fun getPerFileElementTypeModificationTracker(p0: StubFileElementType<*>): ModificationTracker =
        TODO("Not yet implemented")

    override fun getStubIndexModificationTracker(p0: Project): ModificationTracker = TODO("Not yet implemented")
}

private class MockSdkVetoer : GoBasedSdkVetoer {
    override fun isSdkVetoed(p0: GoBasedSdk, p1: Module): Boolean = false
}
