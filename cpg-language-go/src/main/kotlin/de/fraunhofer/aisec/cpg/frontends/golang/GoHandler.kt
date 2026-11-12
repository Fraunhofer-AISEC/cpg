/*
 * Copyright (c) 2023, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.frontends.golang

import de.fraunhofer.aisec.cpg.frontends.Handler
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.ProblemNode
import de.fraunhofer.aisec.cpg.graph.declarations.Namespace
import de.fraunhofer.aisec.cpg.graph.expressions.Literal
import de.fraunhofer.aisec.cpg.graph.scopes.NamespaceScope
import de.fraunhofer.aisec.cpg.helpers.Util
import java.util.function.Supplier

abstract class GoHandler<ResultNode : Node?, HandlerNode : GoStandardLibrary.Ast.Node>(
    configConstructor: Supplier<ResultNode?>,
    lang: GoLanguageFrontend,
) : Handler<ResultNode?, HandlerNode, GoLanguageFrontend>(configConstructor, lang) {
    /**
     * We intentionally override the logic of [Handler.handle] because we do not want the map-based
     * logic, but rather want to make use of the Kotlin-when syntax.
     *
     * We also want to support nullable results for some handlers, e.g., when ignoring private
     * declarations in dependencies.
     */
    override fun handle(ctx: HandlerNode): ResultNode {
        val node = handleNode(ctx)
        if (node == null) {
            return node
        }

        frontend.setComment(node, ctx)
        frontend.process(ctx, node)

        lastNode = node

        return node
    }

    abstract fun handleNode(node: HandlerNode): ResultNode

    /**
     * This function should be called by classes that derive from [GoHandler] to denote, that the
     * supplied node (type) is not supported.
     */
    protected fun handleNotSupported(node: HandlerNode, name: String): ResultNode {
        Util.errorWithFileLocation(
            frontend,
            node,
            log,
            "Parsing of type $name is not supported (yet)",
        )

        val cpgNode = this.configConstructor.get()
        if (cpgNode is ProblemNode) {
            cpgNode.problem = "Parsing of type $name is not supported (yet)"
        }

        return cpgNode!!
    }

    /**
     * This virtual property returns the name of the import package when imported without any
     * aliases.
     */
    val GoStandardLibrary.Ast.ImportSpec.importName: String
        get() {
            // We set the filename of the include declaration to the package path, i.e., its full
            // path including any module identifiers. This way we can match the include declaration
            // back to the namespace's path and name
            val filename = path.value.removeSurrounding("\"").removeSurrounding("`")

            // While it is convention that the respective Go package is named after the last path in
            // the import name, this is purely a convention. We therefore need to find out the real
            // package name. Currently, we do not support parallel parsing, so the order of imports
            // might be suitable to look up the specific package in the scope manager. In the
            // future, we might need a better solution.
            val namespace =
                frontend.scopeManager
                    .filterScopes {
                        it is NamespaceScope && (it.astNode as? Namespace)?.path == filename
                    }
                    .firstOrNull()
                    ?.astNode as? Namespace

            if (namespace != null) {
                // We can directly take the "real" namespace name then
                return namespace.name.localName
            }

            // The namespace is only in the scope manager once the imported package has been
            // parsed, which holds when the files are parsed one after another in dependency
            // order, but not when they are parsed concurrently. Read the package clause from the
            // imported package's own source instead, which gives the same answer regardless of
            // the order in which anything is parsed. gopkg.in/alecthomas/kingpin.v2 is the
            // typical case: the path ends in `kingpin.v2` while the package is called `kingpin`,
            // and guessing from the path turns every `kingpin.X` into a member access on an
            // unknown base.
            packageNameFromSource(filename)?.let {
                return it
            }

            run {
                val path = frontend.expressionHandler.handle(this.path) as? Literal<*>
                val paths = (path?.value as? String)?.split("/") ?: listOf()

                // Return the last name in the path as the import name. However, if the last name is
                // a module version (e.g., v5), then we need to return the second-to-last
                var last = paths.lastOrNull()
                last =
                    if (last != null && last.length >= 2 && last[0] == 'v' && last[1].isDigit()) {
                        paths.getOrNull(paths.size - 2)
                    } else {
                        last
                    }

                return last ?: ""
            }
        }

    /**
     * Determines the name a package declares for itself, by reading the `package` clause of one of
     * its source files. Returns `null` if the package's directory cannot be located or holds no
     * readable Go file.
     *
     * The answer only depends on the files on disk, so it is stable no matter in which order -- or
     * on how many threads -- the project is parsed. Results are cached per import path, because the
     * same package is imported many times across a project.
     */
    private fun packageNameFromSource(importPath: String): String? {
        return packageNameCache
            .computeIfAbsent(importPath) { path ->
                val dir = locatePackageDirectory(path) ?: return@computeIfAbsent ""
                val file =
                    dir.listFiles { f: java.io.File ->
                            f.isFile && f.name.endsWith(".go") && !f.name.endsWith("_test.go")
                        }
                        ?.minByOrNull { it.name } ?: return@computeIfAbsent ""
                val clause =
                    try {
                        file.useLines { lines ->
                            lines.firstNotNullOfOrNull { line ->
                                PACKAGE_CLAUSE.matchEntire(line.trim())?.groupValues?.get(1)
                            }
                        }
                    } catch (e: Exception) {
                        null
                    }
                clause ?: ""
            }
            .ifEmpty { null }
    }

    /**
     * Finds the directory holding the package [importPath], by walking up from the file currently
     * being parsed and looking in each `vendor` directory, and finally in the Go standard library.
     */
    private fun locatePackageDirectory(importPath: String): java.io.File? {
        var dir = frontend.currentFilePath?.absoluteFile?.parentFile
        while (dir != null) {
            val vendored = java.io.File(java.io.File(dir, "vendor"), importPath)
            if (vendored.isDirectory) {
                return vendored
            }
            dir = dir.parentFile
        }

        val goRoot = System.getenv("GOROOT") ?: "/usr/local/go"
        val inStdlib = java.io.File(java.io.File(goRoot, "src"), importPath)
        return if (inStdlib.isDirectory) inStdlib else null
    }

    companion object {
        private val PACKAGE_CLAUSE = Regex("""package\s+([A-Za-z_][A-Za-z0-9_]*).*""")

        /** Package name per import path; the same package is imported many times. */
        private val packageNameCache = java.util.concurrent.ConcurrentHashMap<String, String>()
    }
}
