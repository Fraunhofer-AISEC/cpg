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
import de.fraunhofer.aisec.cpg.helpers.Util.errorWithFileLocation
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.util.function.Supplier
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
abstract class Handler<ResultNode : Node?, HandlerNode, L : LanguageFrontend<in HandlerNode, *>>(
    protected val configConstructor: Supplier<ResultNode>,
    /** Returns the frontend which used this handler. */
    override val frontend: L,
) :
    LanguageProvider by frontend,
    ContextProvider by frontend,
    CodeAndLocationProvider<HandlerNode> by frontend,
    ScopeProvider by frontend,
    NamespaceProvider by frontend,
    FrontendProvider<L>,
    RawNodeTypeProvider<HandlerNode> {
    protected val map = HashMap<Class<out HandlerNode>, HandlerInterface<ResultNode, HandlerNode>>()
    private val typeOfT: Class<*>?

    /**
     * This property contains the last node this handler has successfully processed. It is safe to
     * call, even when parsing multiple TUs in parallel, since for each TU, a dedicated
     * [LanguageFrontend] is spawned, and for each frontend, a dedicated set of [Handler]s is
     * created. Within one TU, the processing is sequential in the AST order.
     */
    var lastNode: ResultNode? = null

    /**
     * Searches for a handler matching the most specific superclass of [HandlerNode]. The created
     * map should thus contain a handler for every semantically different AST node and can reuse
     * handler code as long as the handled AST nodes have a common ancestor.
     *
     * @param ctx The AST node, whose handler is matched with respect to the AST node class.
     * @return most specific handler.
     */
    open fun handle(ctx: HandlerNode): ResultNode? {
        var ret: ResultNode?
        if (ctx == null) {
            log.error(
                "ctx is NULL. This can happen when ast children are optional in ${this.javaClass}. Called by ${Thread.currentThread().stackTrace[2]}"
            )
            return null
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
                errorWithFileLocation(
                    frontend,
                    ctx,
                    log,
                    "No handler for type ${ctx.javaClass}, resolving for its superclass $toHandle.",
                )
            }
            if (toHandle == typeOfT || typeOfT != null && !typeOfT.isAssignableFrom(toHandle)) {
                break
            }
        }
        if (handler != null) {
            val s = handler.handle(ctx)
            if (s != null) {
                frontend.setComment(s, ctx)
            }
            ret = s
        } else {
            errorWithFileLocation(
                frontend,
                ctx,
                log,
                "Parsing of type ${ctx.javaClass} is not supported (yet)",
            )
            ret = configConstructor.get()
            if (ret is ProblemNode) {
                val problem = ret
                problem.problem =
                    String.format("Parsing of type ${ctx.javaClass} is not supported (yet)")
            }
        }

        // In case the node is empty, we report a problem
        if (ret == null) {
            errorWithFileLocation(
                frontend,
                ctx,
                log,
                "Parsing of type ${ctx.javaClass} did not produce a proper CPG node",
            )
            ret = configConstructor.get()
        }

        if (ret != null) {
            frontend.process(ctx, ret)
            lastNode = ret
        }

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

    companion object {
        @JvmStatic protected val log: Logger = LoggerFactory.getLogger(Handler::class.java)
    }

    init {
        typeOfT = retrieveTypeParameter()
    }
}
