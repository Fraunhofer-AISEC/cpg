/*
 * Copyright (c) 2022, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg

import de.fraunhofer.aisec.cpg.frontends.TranslationException
import de.fraunhofer.aisec.cpg.graph.Node
import java.lang.Exception
import java.net.URLEncoder
import java.util.*
import java.util.function.Consumer

/**
 * A generic superclass for components that change the graph. This includes language frontends as the component that does
 * the initial translation into the CPG-AST, as well as graph enhancing passes. The purpose of this class is to capture all
 * behavior that we need for this components, such as tracking of handled nodes.
 */
abstract class GraphTransformation {

    protected var parserObjectStack = Stack<Any>()

    open fun pushToHandleLog(parserObject: Any?) {
        parserObject?.let { parserObjectStack.push(parserObject) }
    }

    open fun popFromHandleLog(parserObject: Any?) {
        parserObject?.let {
            if (parserObjectStack.contains(parserObject)) {
                while (parserObjectStack.pop() !== parserObject) ;
            }
        }
    }

    protected abstract fun getLocationString(a: Any): String

    open fun printHandlerLogTrace() {
        val stackString: String = getHandlerLogTrace()
        System.err.println(stackString)
    }

    open fun getHandlerLogTrace(): String {
        val stackString: StringBuilder = StringBuilder()
        parserObjectStack.forEach(
            Consumer { logObject: Any ->
                stackString.insert(
                    0,
                    "\n\tWhen handling ${logObject.javaClass.name} from ${getLocationString(logObject)}"
                )
            }
        )
        stackString.insert(0, "${this.javaClass} encountered and exception:")
        return stackString.toString()
    }

    fun withNodeInLog(node: Node, f: Runnable) = withNodeInLog(node) { f.run() }

    fun withNodeInLog(node: Node, block: () -> Unit) {
        pushToHandleLog(node)
        block()
        popFromHandleLog(node)
    }

    fun <T, S> withNodeInLogReturning(node: S, block: () -> T): T {
        pushToHandleLog(node)
        val ret = block()
        popFromHandleLog(node)
        return ret
    }

    fun <T, S> replaceWithReturningNodeInLog(node: S, block: () -> T): T {
        popFromHandleLog(node)
        val ret = block()
        pushToHandleLog(ret)
        return ret
    }

    fun <S> replaceNodeInLog(old: S, new: S) {
        popFromHandleLog(old)
        pushToHandleLog(new)
    }

    companion object {

        val githubIssueGuide: String =
            "\tTo report this Issue visit https://github.com/Fraunhofer-AISEC/cpg/issues/new?&template=bugreport-from-cpg-traces.md&title=%s\n" +
                "\tIf possible: \n" +
                "\t\t* paste this message and stack trace for us to locate the issue.\n" +
                "\t\t* past the parsed code that cause the issue from your source, the location is referenced by the lines 'at processing of ... in ...'\n" +
                "\t\t* tell us if you used the default passes and language frontends, or made any changes to the TranslationConfiguration, e.g. registered new passes or frontends, or deactivated any."

        open fun getTranslationExceptionWithHandledStack(
            gt: GraphTransformation,
            originalException: Exception
        ): TranslationException {
            val componentName = gt.javaClass.simpleName
            val baseErrorName = originalException.javaClass.simpleName
            val exceptionMessage = "$baseErrorName in $componentName"
            var customErrorMessage =
                "$exceptionMessage\n\n${githubIssueGuide.format(URLEncoder.encode(exceptionMessage, "utf-8"))}\n"
            val size = gt.parserObjectStack.size
            val stackTrace =
                Array<StackTraceElement>(size) { i ->
                    StackTraceElement(
                        "processing of ${gt.parserObjectStack[size - 1 - i].javaClass.name} located in ${gt.getLocationString(gt.parserObjectStack[size - 1 - i])}",
                        "",
                        "",
                        0
                    )
                }
            val te = TranslationException(customErrorMessage, originalException)
            te.stackTrace = stackTrace
            return te
        }
    }
}
