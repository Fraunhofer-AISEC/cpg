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

import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend
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
        parserObjectStack.forEach(
            Consumer { parserObject: Any ->
                LanguageFrontend.log.error(
                    "Translating {} from Location {}",
                    parserObject.javaClass.name,
                    getLocationString(parserObject)
                )
            }
        )
    }
}
