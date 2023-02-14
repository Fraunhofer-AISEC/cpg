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
package de.fraunhofer.aisec.cpg.frontends.python

import de.fraunhofer.aisec.cpg.ScopeManager
import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.frontends.HasShortCircuitOperators
import de.fraunhofer.aisec.cpg.frontends.Language
import de.fraunhofer.aisec.cpg.graph.statements.expressions.BinaryOperator
import de.fraunhofer.aisec.cpg.graph.types.*
import kotlin.reflect.KClass

/** The Python language. */
class PythonLanguage : Language<PythonLanguageFrontend>(), HasShortCircuitOperators {
    override val fileExtensions = listOf("py")
    override val namespaceDelimiter = "."
    override val frontend: KClass<out PythonLanguageFrontend> = PythonLanguageFrontend::class
    override val conjunctiveOperators = listOf("and")
    override val disjunctiveOperators = listOf("or")

    @Transient
    override val simpleTypes =
        mapOf(
            "bool" to IntegerType("bool", 1, this, NumericType.Modifier.NOT_APPLICABLE),
            "int" to
                IntegerType(
                    "int",
                    Integer.MAX_VALUE,
                    this,
                    NumericType.Modifier.NOT_APPLICABLE
                ), // Unlimited precision
            "float" to
                FloatingPointType(
                    "float",
                    32,
                    this,
                    NumericType.Modifier.NOT_APPLICABLE
                ), // This depends on the implementation
            "complex" to
                NumericType(
                    "complex",
                    null,
                    this,
                    NumericType.Modifier.NOT_APPLICABLE
                ), // It's two floats
            "str" to StringType("str", this, listOf())
        )

    override fun newFrontend(
        config: TranslationConfiguration,
        scopeManager: ScopeManager,
    ): PythonLanguageFrontend {
        return PythonLanguageFrontend(this, config, scopeManager)
    }

    override fun propagateTypeOfBinaryOperation(operation: BinaryOperator): Type {
        if (
            operation.operatorCode == "/" &&
                operation.lhs.propagationType is NumericType &&
                operation.rhs.propagationType is NumericType
        ) {
            // In Python, the / operation automatically casts the result to a float
            return simpleTypes["float"]!!
        } else if (
            operation.operatorCode == "//" &&
                operation.lhs.propagationType is NumericType &&
                operation.rhs.propagationType is NumericType
        ) {
            if (
                operation.lhs.propagationType is IntegerType &&
                    operation.rhs.propagationType is IntegerType
            ) {
                // In Python, the // operation keeps the type as an int if both inputs are integers
                // or casts it to a float otherwise.
                return simpleTypes["int"]!!
            } else {
                return simpleTypes["float"]!!
            }
        }

        // The rest behaves like other languages
        return super.propagateTypeOfBinaryOperation(operation)
    }
}
