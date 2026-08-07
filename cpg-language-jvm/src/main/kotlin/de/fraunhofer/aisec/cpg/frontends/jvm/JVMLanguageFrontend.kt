/*
 * Copyright (c) 2024, Fraunhofer AISEC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *                    $$$$$$\  $$$$$$$\   $$$$$$\
 *                   $$  __$$\ $$  __$$\ $$  __$$\
 *                   $$ /  \__|$$ |  $$ |$$ /  \__|
 *                   $$ |      $$$$$$$  |$$ |$$$$\
 *                   $$ |      $$  ____/ $$ |\_$$ |
 *                   $$ |  $$\ $$ |      $$ |  $$ |
 *                   \$$$$$   |$$ |      \$$$$$   |
 *                    \______/ \__|       \______/
 *
 */
package de.fraunhofer.aisec.cpg.frontends.jvm

// import sootup.java.frontend.inputlocation.JavaSourcePathAnalysisInputLocation
import de.fraunhofer.aisec.cpg.TranslationContext
import de.fraunhofer.aisec.cpg.frontends.Language
import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend
import de.fraunhofer.aisec.cpg.frontends.TranslationException
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.Namespace
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnit
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation
import de.fraunhofer.aisec.cpg.sarif.Region
import java.io.File
import java.net.URI
import kotlin.jvm.optionals.getOrNull
import sootup.apk.frontend.ApkAnalysisInputLocation
import sootup.apk.frontend.DexBodyInterceptors
import sootup.apk.frontend.dexpler.DexClassSource
import sootup.apk.frontend.main.AndroidVersionInfo
import sootup.core.cache.provider.LRUCacheProvider
import sootup.core.interceptor.BodyInterceptor
import sootup.core.jimple.common.stmt.Stmt
import sootup.core.model.Body
import sootup.core.model.HasPosition
import sootup.core.model.Position
import sootup.core.model.SootMethod
import sootup.core.model.SourceType
import sootup.core.types.ArrayType
import sootup.core.types.UnknownType
import sootup.core.util.printer.NormalStmtPrinter
import sootup.interceptors.*
import sootup.java.bytecode.frontend.inputlocation.JavaClassPathAnalysisInputLocation
import sootup.java.core.views.JavaView
import sootup.jimple.frontend.JimpleAnalysisInputLocation

typealias SootType = sootup.core.types.Type

class JVMLanguageFrontend(
    ctx: TranslationContext,
    language: Language<out LanguageFrontend<Any, SootType>>,
) : LanguageFrontend<Any, SootType>(ctx, language) {

    val declarationHandler = DeclarationHandler(this)
    val statementHandler = StatementHandler(this)
    val expressionHandler = ExpressionHandler(this)

    lateinit var view: JavaView

    var classFileName: String? = null

    override val frontendConfiguration: JVMFrontendConfiguration by lazy {
        (this.ctx.config.frontendConfigurations[this::class] as? JVMFrontendConfiguration)
            ?: JVMFrontendConfiguration()
    }

    var body: Body? = null

    var printer: NormalStmtPrinter? = null

    /**
     * The statement that is currently being translated. This is used by [locationOf] to give a
     * source position to values that do not carry a position of their own. In SootUp, locals as
     * well as `this` and parameter references are interned/shared flyweights (a single object is
     * reused at every def/use site), so they never carry a per-occurrence position. For those we
     * fall back to the position of the enclosing statement (which does have one, coming e.g. from
     * the bytecode `LineNumberTable`).
     */
    var currentStmt: Stmt? = null

    val bodyInterceptors: List<BodyInterceptor> =
        listOf(
            NopEliminator(),
            CastAndReturnInliner(),
            UnreachableCodeEliminator(),
            Aggregator(),
            CopyPropagator(),
            // ConditionalBranchFolder(),
            EmptySwitchEliminator(),
            TypeAssigner(),
            LocalNameStandardizer(),
        )

    /**
     * Because of a limitation in SootUp, we can only specify the whole classpath for soot to parse.
     * But in the CPG we need to specify one file. In this case, we take the
     * [TranslationConfiguration.topLevel] and hand it over to soot, which parses all appropriate
     * files within this folder/classpath. This means that the returned [TranslationUnit] will
     * contain not just the content of one file but the whole directory.
     */
    override fun parse(file: File): TranslationUnit {
        val view =
            when {
                file.extension == "class" -> {
                    JavaView(
                        JavaClassPathAnalysisInputLocation(
                            ctx.currentComponent?.topLevel()?.path!!,
                            SourceType.Library,
                            bodyInterceptors,
                        )
                    )
                }
                file.isApk() -> {
                    val apkAnalysis =
                        ApkAnalysisInputLocation(
                            file.toPath(),
                            AndroidVersionInfo(
                                file.toPath(),
                                "",
                            ), // TODO: Add the android.jar path and files
                            DexBodyInterceptors.Default.bodyInterceptors(),
                        )

                    JavaView(listOf(apkAnalysis), LRUCacheProvider(2))
                }
                file.isJar() -> {
                    JavaView(
                        JavaClassPathAnalysisInputLocation(
                            file.path,
                            SourceType.Library,
                            bodyInterceptors,
                        )
                    )
                }
                file.extension == "jimple" -> {
                    JavaView(
                        JimpleAnalysisInputLocation(ctx.currentComponent?.topLevel()?.toPath()!!)
                    )
                }
                else -> {
                    throw TranslationException("unsupported file")
                }
            }
        // This contains the whole directory
        val tu = newTranslationUnit(file.parent)
        scopeManager.resetToGlobal(tu)

        val packages = mutableMapOf<String, Namespace>()

        for (sootClass in view.classes) {
            // Create an appropriate namespace, if it does not already exist
            var pkg =
                sootClass.type.packageName?.name?.split(language.namespaceDelimiter)?.fold(null) {
                    previous: Namespace?,
                    path ->
                    val fqn = previous?.name.fqn(path)
                    val innerPkg =
                        packages.computeIfAbsent(fqn.toString()) {
                            newNamespace(it, holder = previous ?: tu)
                        }
                    // Enter namespace scope
                    scopeManager.enterScope(innerPkg)
                    innerPkg
                }

            // Try to obtain a meaningful file name/path for this class:
            // - For APK/dex input, the original source file name (e.g. "MainActivity.java") is
            //   available from the dex debug information via DexClassSource.getSourceFile().
            // - Otherwise we fall back to the path from which the class was loaded (the .class
            //   file, the entry inside a jar, or the .jimple file). Note that for plain .class/.jar
            //   bytecode SootUp does not read the `SourceFile` attribute, so the *original* .java
            //   name is not available there -- only the load path is.
            // - As a last resort we use the fully-qualified class name (the previous behavior).
            val classSource = sootClass.classSource
            classFileName =
                // A dex `SourceFile` entry can be present but blank (e.g. an obfuscator that
                // empties
                // the attribute); reject blank so the fallback chain still applies.
                (classSource as? DexClassSource)?.sourceFile?.getOrNull()?.takeIf {
                    it.isNotBlank()
                }
                    ?: runCatching { classSource.sourcePath?.toString() }.getOrNull()
                    ?: sootClass.name.replace(language.namespaceDelimiter, "/")

            val decl = declarationHandler.handle(sootClass)
            scopeManager.addDeclaration(decl)
            pkg?.addDeclaration(decl)

            // Leave namespace scope
            while (pkg is Namespace) {
                scopeManager.leaveScope(pkg)
                pkg = pkg.astParent as? Namespace
            }

            // We need to clear the processed because they need to be per-file and we only have one
            // frontend for all files
            clearProcessed()
        }

        return tu
    }

    override fun setComment(node: Node, astNode: Any) {}

    override fun locationOf(astNode: Any): PhysicalLocation? {
        // A position is only useful if it actually points at a source line (>= 1). We prefer the
        // node's own per-occurrence position; if it does not have a usable one -- which is the case
        // for locals as well as `this`/parameter references (SootUp interns and shares those across
        // all their use sites) and occasionally for synthetic expressions -- we fall back to the
        // position of the statement currently being translated. That way e.g. a reference to a
        // local reports the line of the statement that uses it instead of the dummy location
        // -1:-1:-1:-1 (which is what NoPositionInformation represents).
        fun usable(position: Position?) = position?.takeIf { it.firstLine >= 1 }

        val position =
            usable((astNode as? HasPosition)?.position)
                ?: usable(currentStmt?.position)
                ?: return null

        // Build a URI from the (best-effort) file name. The three-argument URI constructor properly
        // encodes spaces/special characters and yields a hierarchical (path-bearing) URI for the
        // usual inputs (a relative dex source name like "MainActivity.kt", or a POSIX load path).
        // A Windows drive path such as "C:\\proj\\Foo.class", however, is parsed with "C:" as a
        // scheme and produces an OPAQUE, path-less URI -- which would later NPE in PhysicalLocation
        // (uri.path.substring(...)). For that case we fall back to File.toURI(); and we reject any
        // still-opaque/path-less result so a bad name degrades to a null location URI instead of
        // crashing the whole class's translation.
        val uri =
            classFileName
                ?.takeIf { it.isNotBlank() }
                ?.let { name ->
                    val encoded = runCatching { URI(null, name, null) }.getOrNull()
                    if (encoded != null && !encoded.isOpaque && encoded.path != null) {
                        encoded
                    } else {
                        runCatching { File(name).toURI() }
                            .getOrNull()
                            ?.takeIf { !it.isOpaque && it.path != null }
                    }
                }

        return PhysicalLocation(
            uri = uri,
            region =
                Region(
                    startLine = position.firstLine,
                    endLine = position.lastLine,
                    startColumn = position.firstCol,
                    endColumn = position.lastCol,
                ),
        )
    }

    override fun codeOf(astNode: Any): String? {
        if (astNode is SootMethod && astNode.isConcrete) {
            try {
                return astNode.body.toString()
            } catch (e: IllegalArgumentException) {
                log.error("Could not retrieve the code of $astNode", e)
            }
        } else {
            try {
                return astNode.toString()
            } catch (e: Exception) {
                log.error("Could not retrieve the code of $astNode", e)
            }
        }
        return null
    }

    override fun typeOf(type: SootType): Type {
        return when (type) {
            is UnknownType -> {
                unknownType()
            }
            is ArrayType -> {
                typeOf(type.baseType).array()
            }
            else -> {
                // TODO(oxisto): primitive types
                val out = objectType(type.toString())

                out
            }
        }
    }
}
