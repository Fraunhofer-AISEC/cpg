/*
 * Copyright (c) 2019, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.frontends

import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.newCallExpression
import de.fraunhofer.aisec.cpg.helpers.Util.errorWithFileLocation
import de.fraunhofer.aisec.cpg.passes.scopes.Scope
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.util.function.Supplier
import org.eclipse.cdt.internal.core.dom.parser.ASTNode
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * A [Handler] is an abstract base class for a class that translates AST nodes from a raw ast type,
 * usually supplied by a language parser into our generic CPG nodes.
 *
 * It implements at least one [MetadataProvider], so that node builder extension functions (e.g.,
 * [newCallExpression] can be used directly to create appropriate nodes.
 *
 * @param <S> the result node or a collection of nodes
 * @param <T> the raw ast node specific to the parser
 * @param <L> the language frontend </L></T></S>
 */
abstract class Handler<S : Node, T, L : LanguageFrontend>(
    protected val configConstructor: Supplier<S>,
    /** Returns the frontend which used this handler. */
    var frontend: L
) : LanguageProvider, CodeAndLocationProvider, ScopeProvider, NamespaceProvider {
    protected val map = HashMap<Class<out T>, HandlerInterface<S, T>>()
    private val typeOfT: Class<*>?

    /**
     * Searches for a handler matching the most specific superclass of [T]. The created map should
     * thus contain a handler for every semantically different AST node and can reuse handler code
     * as long as the handled AST nodes have a common ancestor.
     *
     * @param ctx The AST node, whose handler is matched with respect to the AST node class.
     * @return most specific handler.
     */
    open fun handle(ctx: T): S? {
        var ret: S?
        if (ctx == null) {
            log.error(
                "ctx is NULL. This can happen when ast children are optional in {}. Called by {}",
                this.javaClass,
                Thread.currentThread().stackTrace[2]
            )
            return null
        }

        // If we do not want to load includes into the CPG and the current fileLocation was included
        if (!frontend.config.loadIncludes && ctx is ASTNode) {
            val astNode = ctx as ASTNode
            if (
                astNode.fileLocation != null &&
                    astNode.fileLocation.contextInclusionStatement != null
            ) {
                log.debug("Skip parsing include file {}", astNode.containingFilename)
                return null
            }
        }
        var toHandle: Class<*> = ctx.javaClass
        var handler = map[toHandle]
        while (handler == null) {
            toHandle = toHandle.superclass
            handler = map[toHandle]
            if (
                handler != null && // always ok to handle as generic literal expr
                !ctx.javaClass.simpleName.contains("LiteralExpr")
            ) {
                errorWithFileLocation<T>(
                    frontend,
                    ctx,
                    log,
                    "No handler for type {}, resolving for its superclass {}.",
                    ctx.javaClass,
                    toHandle
                )
            }
            if (toHandle == typeOfT || typeOfT != null && !typeOfT.isAssignableFrom(toHandle)) {
                break
            }
        }
        if (handler != null) {
            val s = handler.handle(ctx)
            if (s != null) {
                // The language frontend might set a location, which we should respect. Otherwise,
                // we will
                // set the location here.
                if (s.location == null) {
                    frontend.setCodeAndLocation<S, T>(s, ctx)
                }
                frontend.setComment<S, T>(s, ctx)
            }
            ret = s
        } else {
            errorWithFileLocation<T>(
                frontend,
                ctx,
                log,
                "Parsing of type {} is not supported (yet)",
                ctx.javaClass
            )
            ret = configConstructor.get()
            if (ret is ProblemNode) {
                val problem = ret
                problem.problem =
                    String.format("Parsing of type {} is not supported (yet)", ctx.javaClass)
            }
        }

        // In case the node is empty, we report a problem
        if (ret == null) {
            errorWithFileLocation<T>(
                frontend,
                ctx,
                log,
                "Parsing of type {} did not produce a proper CPG node",
                ctx.javaClass
            )
            ret = configConstructor.get()
        }
        frontend.process(ctx, ret)
        return ret
    }

    private fun retrieveTypeParameter(): Class<*>? {
        var clazz: Class<*> = this.javaClass
        while (clazz.superclass != null && clazz.superclass != Handler::class.java) {
            clazz = clazz.superclass
        }
        val type = clazz.genericSuperclass
        if (type is ParameterizedType) {
            val rawType = type.actualTypeArguments[1]
            return getBaseClass(rawType)
        }
        log.error("Could not determine generic type of raw AST node in handler")
        return null
    }

    private fun getBaseClass(type: Type): Class<*>? {
        if (type is Class<*>) {
            return type
        } else if (type is ParameterizedType) {
            return getBaseClass(type.rawType)
        }
        return null
    }

    /** Returns the language which this handler is parsing. */
    override val language: Language<L>
        get() = frontend.language as Language<L>

    override fun <N, S> setCodeAndLocation(cpgNode: N, astNode: S?) {
        frontend.setCodeAndLocation<N, S>(cpgNode, astNode)
    }

    override val scope: Scope?
        get() = frontend.scope
    override val namespace: Name?
        get() = frontend.namespace

    companion object {
        @JvmStatic protected val log: Logger = LoggerFactory.getLogger(Handler::class.java)
    }

    init {
        typeOfT = retrieveTypeParameter()
    }
}
