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
import java.lang.Exception
import java.util.*
import java.util.function.Consumer

abstract class GraphTransformation {

    protected var parserObjectStack = Stack<Any>()

    open fun pushToHandleLog(parserObject: Any?) {
        parserObjectStack.push(parserObject)
    }

    open fun popFromHandleLog(parserObject: Any) {
        if (parserObjectStack.contains(parserObject)) {
            while (parserObjectStack.pop() !== parserObject) ;
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

    companion object {

        open fun getTranslationExceptionWithHandledStack(
            gt: GraphTransformation,
            originalException: Exception
        ): TranslationException {
            val componentName = gt.javaClass.simpleName
            val baseErrorName = originalException.javaClass.simpleName
            var customErrorMessage = "$baseErrorName in $componentName \n${gt.getHandlerLogTrace()}"
            return TranslationException(customErrorMessage, originalException)
        }
    }
}
