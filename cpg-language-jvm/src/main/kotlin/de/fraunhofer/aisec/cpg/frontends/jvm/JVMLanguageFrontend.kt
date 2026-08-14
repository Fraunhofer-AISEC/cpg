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
import java.nio.file.Files
import java.nio.file.Path
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
import sootup.core.model.SootClass
import sootup.core.model.SootMethod
import sootup.core.model.SourceType
import sootup.core.types.ArrayType
import sootup.core.types.UnknownType
import sootup.core.util.printer.NormalStmtPrinter
import sootup.interceptors.*
import sootup.java.bytecode.frontend.inputlocation.JavaClassPathAnalysisInputLocation
import sootup.java.core.views.JavaView
import sootup.jimple.frontend.JimpleAnalysisInputLocation
import sootup.jimple.frontend.JimpleTextPositions

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

    /**
     * True while translating a class whose positions come from the reprinted Jimple text (see
     * [JVMFrontendConfiguration.useJimpleTextPositions] and [JimpleTextPositions]). In that mode
     * [classFileName] points at the written `.jimple` file and [locationOf] must reconcile the two
     * line bases the Jimple parser produces: statement positions are 1-based (matching the file),
     * whereas value positions are 0-based. See [locationOf] for how this is handled.
     */
    var currentClassUsesTextPositions: Boolean = false

    /**
     * The successful Jimple-text round-trip of the class currently being translated, or `null` when
     * positions come from the compiled artifact. Method bodies of a reparsed class are built
     * lazily, so a body that does not survive the round-trip only surfaces while translating that
     * method -- long after [JimpleTextPositions.reparse] returned. See [withMethodPositions], which
     * degrades such a method individually instead of losing it (or the whole class) with it.
     */
    private var currentReparse: JimpleTextPositions.Result? = null

    /**
     * The original, compiled class behind [currentReparse]. It provides the fallback method whose
     * body [withMethodPositions] translates for a method that has no text positions.
     */
    private var currentOriginalClass: SootClass? = null

    /**
     * The [classFileName] that refers to the compiled artifact of the class currently being
     * translated (as opposed to the reprinted `.jimple` file). This is what [classFileName] is set
     * to outside text-position mode, and what [withMethodPositions] restores for a single degraded
     * method.
     */
    private var artifactFileName: String? = null

    /**
     * Lazily-created temporary directory into which the reprinted Jimple text of each class is
     * written (only when [JVMFrontendConfiguration.useJimpleTextPositions] is enabled), so that the
     * `.jimple` line numbers reported in positions resolve to a real, readable file.
     */
    val jimpleOutputDir: Path by lazy { Files.createTempDirectory("cpg-jimple-positions-") }

    /** The `.jimple` file already written for a given fully-qualified class name (idempotent). */
    private val jimpleFiles = mutableMapOf<String, Path>()

    /**
     * Lower-cased relative paths already claimed under [jimpleOutputDir], used to keep file names
     * unique on case-insensitive filesystems (macOS/Windows) where two class names differing only
     * in case would otherwise map to the same file and overwrite each other.
     */
    private val usedJimplePaths = mutableSetOf<String>()

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

        for (originalClass in view.classes) {
            // Optionally round-trip the class through its textual Jimple representation so that
            // statements and values get per-line, column-precise positions into the reprinted text
            // instead of the coarse, often collapsed line numbers of the compiled artifact. When
            // this succeeds we translate the *reparsed* class and point locations at the written
            // `.jimple` file; if it fails (a construct that does not round-trip) we fall back to
            // the
            // original class and its compiled-artifact positions. See JimpleTextPositions.
            val reparse =
                if (frontendConfiguration.useJimpleTextPositions) {
                    runCatching { JimpleTextPositions.reparse(originalClass) }
                        .onFailure {
                            log.warn(
                                "Could not reparse class {} through Jimple text; " +
                                    "falling back to compiled-artifact positions",
                                originalClass.type,
                                it,
                            )
                        }
                        .getOrNull()
                } else {
                    null
                }
            val sootClass = reparse?.sootClass ?: originalClass
            currentClassUsesTextPositions = reparse != null
            currentReparse = reparse
            currentOriginalClass = originalClass

            // Create an appropriate namespace, if it does not already exist
            var pkg =
                sootClass.type.packageName?.name?.split(language.namespaceDelimiter)?.fold(null) {
                    previous: Namespace?,
                    path ->
                    val fqn = previous?.name.fqn(path)
                    val innerPkg =
                        packages.computeIfAbsent(fqn.toString()) {
                            val pkg = newNamespace(it)
                            scopeManager.addDeclaration(pkg)
                            val holder = previous ?: tu
                            holder.addDeclaration(pkg)
                            pkg
                        }
                    // Enter namespace scope
                    scopeManager.enterScope(innerPkg)
                    innerPkg
                }

            artifactFileName = artifactFileNameOf(originalClass)
            classFileName =
                if (reparse != null) {
                    // Positions now index into the reprinted Jimple text, so the file name must
                    // refer to that text (not the original source): write it out and use its path,
                    // so "line N" resolves to a real, readable line of the produced `.jimple` file.
                    runCatching { writeJimpleText(sootClass.name, reparse.jimpleText).toString() }
                        .getOrNull() ?: sootClass.name.replace(language.namespaceDelimiter, "/")
                } else {
                    artifactFileName
                }

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
            currentClassUsesTextPositions = false
            currentReparse = null
            currentOriginalClass = null
            artifactFileName = null
        }

        return tu
    }

    override fun setComment(node: Node, astNode: Any) {}

    /**
     * A meaningful file name/path for the compiled artifact [sootClass] was loaded from:
     * - For APK/dex input, the original source file name (e.g. "MainActivity.java") is available
     *   from the dex debug information via `DexClassSource.getSourceFile()`.
     * - Otherwise we fall back to the path from which the class was loaded (the .class file, the
     *   entry inside a jar, or the .jimple file). Note that for plain .class/.jar bytecode SootUp
     *   does not read the `SourceFile` attribute, so the *original* .java name is not available
     *   there -- only the load path is.
     * - As a last resort we use the fully-qualified class name (the original behavior).
     */
    private fun artifactFileNameOf(sootClass: SootClass): String {
        val classSource = sootClass.classSource
        // A dex `SourceFile` entry can be present but blank (e.g. an obfuscator that empties the
        // attribute); reject blank so the fallback chain still applies.
        return (classSource as? DexClassSource)?.sourceFile?.getOrNull()?.takeIf { it.isNotBlank() }
            ?: runCatching { classSource.sourcePath?.toString() }.getOrNull()
            ?: sootClass.name.replace(language.namespaceDelimiter, "/")
    }

    /**
     * Invokes [block] with the [SootMethod] that should actually be translated for [sootMethod],
     * and with [locationOf] configured to match where that method's positions come from.
     *
     * Normally this is just [sootMethod] itself. But in text-position mode a method body is only
     * rebuilt from the reprinted Jimple text when it is first requested, so a construct that does
     * not survive the round-trip fails here -- long after [JimpleTextPositions.reparse] succeeded
     * for the class, and thus outside the class-level fallback in [parse]. Instead of letting one
     * such method cost us the whole class, we degrade *per method*: its counterpart from the
     * original, compiled class is translated instead, with the compiled artifact's positions and
     * file name ([artifactFileName]). Every other method of the class keeps its text positions.
     *
     * The decision is taken before the method declaration is created so that a degraded method is
     * coherent -- its declaration and its statements then all point into the same file. Since it
     * asks for the body, this forces every concrete method's body of a reparsed class to be built
     * (rather than only those that are translated); in text-position mode all bodies are built
     * anyway, because reprinting the class to Jimple already requires them.
     */
    internal fun <T> withMethodPositions(sootMethod: SootMethod, block: (SootMethod) -> T): T {
        val reparse = currentReparse
        if (reparse == null || !sootMethod.isConcrete) {
            // Nothing to degrade: not in text-position mode, or the method has no body to lose (an
            // abstract/native declaration is positioned by the reprinted text just fine).
            return block(sootMethod)
        }
        // `hasTextPositions` handles a failing body itself; the guard is only for a body that fails
        // with something it does not catch (e.g. a StackOverflowError from a huge method).
        if (runCatching { reparse.hasTextPositions(sootMethod) }.getOrDefault(false)) {
            return block(sootMethod)
        }

        val fallback =
            currentOriginalClass?.getMethod(sootMethod.signature.subSignature)?.getOrNull()
        if (fallback == null) {
            // Should not happen: the reparsed class is printed from the original one. Keep the
            // declaration (its header does have a text position); DeclarationHandler will leave it
            // without a body.
            log.warn(
                "Body of {} could not be rebuilt from the reprinted Jimple text and the method has " +
                    "no counterpart in the original class; keeping it without a body",
                sootMethod.signature,
            )
            return block(sootMethod)
        }

        log.warn(
            "Body of {} could not be rebuilt from the reprinted Jimple text; translating this " +
                "method from {} with compiled-artifact positions instead",
            sootMethod.signature,
            artifactFileName,
        )

        val previousUsesTextPositions = currentClassUsesTextPositions
        val previousFileName = classFileName
        currentClassUsesTextPositions = false
        classFileName = artifactFileName
        try {
            return block(fallback)
        } finally {
            currentClassUsesTextPositions = previousUsesTextPositions
            classFileName = previousFileName
        }
    }

    /**
     * Writes the reprinted Jimple [text] of the class with the given fully-qualified [className] to
     * a `.jimple` file under [jimpleOutputDir] and returns its path. The file's line N corresponds
     * to the 1-based line N that statement positions report, so a downstream consumer can open it
     * to inspect "the node on line N".
     *
     * Writing is idempotent per class, and file names are kept unique even on case-insensitive
     * filesystems (see [usedJimplePaths]) so that a location captured for one class never resolves
     * to another class's text.
     */
    private fun writeJimpleText(className: String, text: String): Path {
        jimpleFiles[className]?.let {
            return it
        }

        val base = className.replace(language.namespaceDelimiter, "/")
        var relative = "$base.jimple"
        var suffix = 1
        while (!usedJimplePaths.add(relative.lowercase())) {
            relative = "${base}_${suffix}.jimple"
            suffix++
        }

        val target = jimpleOutputDir.resolve(relative)
        target.parent?.let { Files.createDirectories(it) }
        Files.write(target, text.toByteArray())
        jimpleFiles[className] = target
        return target
    }

    override fun locationOf(astNode: Any): PhysicalLocation? {
        // Reconcile line bases against the written `.jimple` file. The Jimple parser reports
        // statement positions 1-based (so a statement on the first text line is line 1, exactly
        // matching the file) but every other position -- values, but also the class and its fields
        // -- 0-based. When we translate the reprinted text we therefore lift such a node's own line
        // by one so that every node reports the same 1-based line the file uses. Statement
        // positions (and the whole non-reparse path) are already 1-based and left untouched.
        val ownIsZeroBased = currentClassUsesTextPositions && astNode !is Stmt

        // A position is only useful if it actually points at a source line, i.e. at line >= 1 once
        // its line base is accounted for -- a 0-based position of 0 is the first line and thus
        // perfectly usable (this is where the class declaration itself sits), whereas the dummy
        // -1:-1:-1:-1 of NoPositionInformation never is. We prefer the node's own per-occurrence
        // position; if it does not have a usable one -- which is the case for locals as well as
        // `this`/parameter references (SootUp interns and shares those across all their use sites)
        // and occasionally for synthetic expressions -- we fall back to the position of the
        // statement currently being translated, which is 1-based either way. That way e.g. a
        // reference to a local reports the line of the statement that uses it instead of the dummy
        // location.
        fun usable(position: Position?, zeroBased: Boolean = false) =
            position?.takeIf { it.firstLine >= if (zeroBased) 0 else 1 }

        // Prefer the node's own per-occurrence position; otherwise fall back to the enclosing
        // statement's (see above). We remember which one we used because the two disagree on their
        // line base when positions come from the reprinted Jimple text (see above).
        val ownPosition = usable((astNode as? HasPosition)?.position, ownIsZeroBased)
        val position = ownPosition ?: usable(currentStmt?.position) ?: return null

        val lineOffset = if (ownIsZeroBased && ownPosition != null) 1 else 0

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
                    startLine = position.firstLine + lineOffset,
                    endLine = position.lastLine + lineOffset,
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
