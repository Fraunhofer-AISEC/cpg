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
package de.fraunhofer.aisec.cpg.rules

import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression

class Util {
    companion object {
        fun isPathLike(arg: Expression): Boolean {
            return arg.type.name.localName.contains(Regex("str|char", RegexOption.IGNORE_CASE)) &&
                (arg.name.localName.contains(
                    Regex("path|file|dir|directory|url|uri", RegexOption.IGNORE_CASE)
                ) ||
                    arg.code?.contains(
                        Regex("path|file|dir|directory|url|uri", RegexOption.IGNORE_CASE)
                    ) ?: false ||
                    arg.code?.contains('/') ?: false)
        }

        fun isUserInput(arg: CallExpression): Boolean {
            val userInputRegex =
                Regex("user|input|param|arg|argument|request|query", RegexOption.IGNORE_CASE)
            return arg.name.localName.contains(userInputRegex) ||
                arg.code?.contains(userInputRegex) ?: false
        }

        fun isValidationFunction(arg: CallExpression): Boolean {
            val validationFunctionRegex =
                Regex("val|validate|check|san|sanitize|clean", RegexOption.IGNORE_CASE)
            return arg.name.localName.contains(validationFunctionRegex) ||
                arg.code?.contains(validationFunctionRegex) ?: false
        }
    }
}
