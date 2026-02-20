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

import de.fraunhofer.aisec.cpg.frontends.HasClasses
import de.fraunhofer.aisec.cpg.frontends.HasFunctionOverloading
import de.fraunhofer.aisec.cpg.frontends.Language
import de.fraunhofer.aisec.cpg.graph.declarations.Declaration
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Reference
import de.fraunhofer.aisec.cpg.graph.types.*
import kotlin.reflect.KClass

open class JVMLanguage : Language<JVMLanguageFrontend>(), HasClasses, HasFunctionOverloading {
    override val fileExtensions: List<String> = listOf("class", "java", "jimple", "jar", "apk")

    override val namespaceDelimiter: String = "."

    override val frontend: KClass<out JVMLanguageFrontend> = JVMLanguageFrontend::class

    override val builtInTypes: Map<String, Type> =
        mapOf(
            "float" to FloatingPointType("float", 32, this),
            "double" to FloatingPointType("double", 64, this),
            "char" to IntegerType("char", 8, this, NumericType.Modifier.UNSIGNED),
            "boolean" to BooleanType("boolean", 1, this),
            "byte" to IntegerType("byte", 8, this),
            "short" to IntegerType("short", 16, this),
            "int" to IntegerType("int", 32, this),
            "long" to IntegerType("long", 64, this),
            "java.lang.String" to StringType("java.lang.String", this),
            "java.lang.Class" to ObjectType("java.lang.Class", listOf(), true, this),
        )

    override val compoundAssignmentOperators: Set<String> = setOf()

    /**
     * This function handles some specifics of the Java language when choosing a reference target
     * before invoking [Language.bestViableReferenceCandidate].
     */
    override fun bestViableReferenceCandidate(ref: Reference): Declaration? {
        // Java allows to have "ambiguous" symbol when importing static fields and methods.
        // Therefore, it can be that we both import a field and a method with the same name. We
        // therefore do some additional filtering of the candidates here, before handling it.
        if (ref.candidates.size > 1) {
            if (ref.resolutionHelper is CallExpression) {
                val functionDecls = ref.candidates.filterIsInstance<FunctionDeclaration>()
                // We can also check if the signature matches to account for overloading.
                val targetType = ref.type as? FunctionType
                val filteredFunctions =
                    functionDecls
                        .filter { it.parameters.map { it.type } == targetType?.parameters }
                        .toSet()
                if (filteredFunctions.isNotEmpty()) ref.candidates = filteredFunctions
                else ref.candidates = functionDecls.toSet()
            } else {
                ref.candidates = ref.candidates.filter { it is VariableDeclaration }.toSet()
            }
        }

        return super.bestViableReferenceCandidate(ref)
    }
}
