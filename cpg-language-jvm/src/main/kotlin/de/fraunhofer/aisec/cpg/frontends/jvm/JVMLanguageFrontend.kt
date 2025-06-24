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

import de.fraunhofer.aisec.cpg.TranslationContext
import de.fraunhofer.aisec.cpg.frontends.Language
import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend
import de.fraunhofer.aisec.cpg.frontends.TranslationException
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.NamespaceDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation
import java.io.File
import sootup.apk.frontend.ApkAnalysisInputLocation
import sootup.apk.frontend.DexBodyInterceptors
import sootup.core.model.Body
import sootup.core.model.SootMethod
import sootup.core.model.SourceType
import sootup.core.types.ArrayType
import sootup.core.types.UnknownType
import sootup.core.util.printer.NormalStmtPrinter
import sootup.interceptors.Aggregator
import sootup.interceptors.CastAndReturnInliner
import sootup.interceptors.CopyPropagator
import sootup.interceptors.EmptySwitchEliminator
import sootup.interceptors.LocalNameStandardizer
import sootup.interceptors.NopEliminator
import sootup.interceptors.TypeAssigner
import sootup.interceptors.UnreachableCodeEliminator
import sootup.java.bytecode.frontend.inputlocation.JavaClassPathAnalysisInputLocation
import sootup.java.core.views.JavaView
import sootup.java.frontend.inputlocation.JavaSourcePathAnalysisInputLocation
import sootup.jimple.frontend.JimpleAnalysisInputLocation
import sootup.jimple.frontend.JimpleView

typealias SootType = sootup.core.types.Type

class JVMLanguageFrontend(
    ctx: TranslationContext,
    language: Language<out LanguageFrontend<Any, SootType>>,
) : LanguageFrontend<Any, SootType>(ctx, language) {

    val declarationHandler = DeclarationHandler(this)
    val statementHandler = StatementHandler(this)
    val expressionHandler = ExpressionHandler(this)

    lateinit var view: JavaView

    var body: Body? = null

    var printer: NormalStmtPrinter? = null

    /**
     * Because of a limitation in SootUp, we can only specify the whole classpath for soot to parse.
     * But in the CPG we need to specify one file. In this case, we take the
     * [TranslationConfiguration.topLevel] and hand it over to soot, which parses all appropriate
     * files within this folder/classpath. This means that the returned [TranslationUnitDeclaration]
     * will contain not just the content of one file but the whole directory.
     */
    override fun parse(file: File): TranslationUnitDeclaration {
        val view =
            when (file.extension) {
                "class" -> {
                    JavaView(
                        JavaClassPathAnalysisInputLocation(
                            ctx.currentComponent?.topLevel()?.path!!,
                            SourceType.Library,
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
                            ),
                        )
                    )
                }
                "jar" -> {
                    JavaView(
                        JavaClassPathAnalysisInputLocation(
                            file.path,
                            SourceType.Library,
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
                            ),
                        )
                    )
                }
                "java" -> {
                    JavaView(
                        JavaSourcePathAnalysisInputLocation(
                            ctx.currentComponent?.topLevel()?.path!!
                        )
                    )
                }
                "apk" -> {
                    val apkAnalysis =
                        ApkAnalysisInputLocation(
                            file.toPath(),
                            "",
                            DexBodyInterceptors.Default.bodyInterceptors(),
                        )
                    JavaView(apkAnalysis)
                }
                "jimple" -> {
                    JimpleView(
                        JimpleAnalysisInputLocation(ctx.currentComponent?.topLevel()?.toPath()!!)
                    )
                }
                else -> {
                    throw TranslationException("unsupported file")
                }
            }
        // This contains the whole directory
        val tu = newTranslationUnitDeclaration(file.parent)
        scopeManager.resetToGlobal(tu)

        val packages = mutableMapOf<String, NamespaceDeclaration>()

        for (sootClass in view.classes) {
            // Create an appropriate namespace, if it does not already exist
            val pkg =
                packages.computeIfAbsent(sootClass.type.packageName.name) {
                    val pkg = newNamespaceDeclaration(it)
                    scopeManager.addDeclaration(pkg)
                    tu.addDeclaration(pkg)
                    pkg
                }

            // Enter namespace scope
            scopeManager.enterScope(pkg)

            val decl = declarationHandler.handle(sootClass)
            if (decl != null) {
                scopeManager.addDeclaration(decl)
                pkg.addDeclaration(decl)
            }

            // Leave namespace scope
            scopeManager.leaveScope(pkg)

            // We need to clear the processed because they need to be per-file and we only have one
            // frontend for all files
            clearProcessed()
        }

        return tu
    }

    override fun setComment(node: Node, astNode: Any) {}

    override fun locationOf(astNode: Any): PhysicalLocation? {
        // We do not really have a location anyway. maybe in jimple?
        return null
    }

    override fun codeOf(astNode: Any): String? {

        if (astNode is SootMethod && astNode.isConcrete) {
            try {
                return astNode.body.toString()
            } catch (_: IllegalArgumentException) {
                // Do nothing
            }
        }
        // We do not really have a source anyway. maybe in jimple?
        return ""
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
