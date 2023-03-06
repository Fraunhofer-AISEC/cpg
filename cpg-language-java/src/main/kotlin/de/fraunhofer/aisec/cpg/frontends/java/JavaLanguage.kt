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
package de.fraunhofer.aisec.cpg.frontends.java

import com.fasterxml.jackson.annotation.JsonIgnore
import de.fraunhofer.aisec.cpg.ScopeManager
import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.frontends.*
import de.fraunhofer.aisec.cpg.graph.Name
import de.fraunhofer.aisec.cpg.graph.declarations.RecordDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.expressions.BinaryOperator
import de.fraunhofer.aisec.cpg.graph.statements.expressions.MemberExpression
import de.fraunhofer.aisec.cpg.graph.types.*
import de.fraunhofer.aisec.cpg.passes.JavaCallResolverHelper
import kotlin.reflect.KClass
import org.neo4j.ogm.annotation.Transient

/** The Java language. */
open class JavaLanguage :
    Language<JavaLanguageFrontend>(),
    // HasComplexCallResolution,
    HasClasses,
    HasSuperClasses,
    HasGenerics,
    HasQualifier,
    HasUnknownType,
    HasShortCircuitOperators {
    override val fileExtensions = listOf("java")
    override val namespaceDelimiter = "."
    @Transient override val frontend: KClass<out JavaLanguageFrontend> = JavaLanguageFrontend::class
    override val superClassKeyword = "super"

    override val qualifiers = listOf("final", "volatile")
    override val unknownTypeString = listOf("var")
    override val conjunctiveOperators = listOf("&&")
    override val disjunctiveOperators = listOf("||")

    /**
     * See
     * [Java Language Specification](https://docs.oracle.com/javase/specs/jls/se19/html/jls-4.html).
     */
    @Transient
    @JsonIgnore
    override val builtInTypes =
        mapOf(
            // Boolean Types:
            // https://docs.oracle.com/javase/specs/jls/se19/html/jls-4.html#jls-4.2.5
            "boolean" to BooleanType("boolean", language = this),
            "Boolean" to BooleanType("java.lang.Boolean", language = this),
            "java.lang.Boolean" to BooleanType("java.lang.Boolean", language = this),

            // Integral Types:
            // https://docs.oracle.com/javase/specs/jls/se19/html/jls-4.html#jls-4.2.1
            "byte" to IntegerType("byte", 8, this, NumericType.Modifier.SIGNED),
            "char" to IntegerType("char", 16, this, NumericType.Modifier.UNSIGNED),
            "short" to IntegerType("short", 16, this, NumericType.Modifier.SIGNED),
            "int" to IntegerType("int", 32, this, NumericType.Modifier.SIGNED),
            "long" to IntegerType("long", 64, this, NumericType.Modifier.SIGNED),

            // Floating-Point Types:
            // https://docs.oracle.com/javase/specs/jls/se19/html/jls-4.html#jls-4.2.3
            "float" to FloatingPointType("float", 32, this, NumericType.Modifier.SIGNED),
            "double" to FloatingPointType("double", 64, this, NumericType.Modifier.SIGNED),

            // String: https://docs.oracle.com/javase/specs/jls/se19/html/jls-4.html#jls-4.3.3
            "String" to StringType("java.lang.String", this),
            "java.lang.String" to StringType("java.lang.String", this)
        )

    override fun propagateTypeOfBinaryOperation(operation: BinaryOperator): Type {
        return if (
            operation.operatorCode == "+" &&
                (operation.lhs.type as? IntegerType)?.name?.localName?.equals("char") == true &&
                (operation.rhs.type as? IntegerType)?.name?.localName?.equals("char") == true
        ) {
            return getSimpleTypeOf("int")!!
        } else super.propagateTypeOfBinaryOperation(operation)
    }

    override fun newFrontend(
        config: TranslationConfiguration,
        scopeManager: ScopeManager,
    ): JavaLanguageFrontend {
        return JavaLanguageFrontend(this, config, scopeManager)
    }
    override fun handleSuperCall(
        callee: MemberExpression,
        curClass: RecordDeclaration,
        scopeManager: ScopeManager,
        recordMap: Map<Name, RecordDeclaration>
    ) = JavaCallResolverHelper.handleSuperCall(callee, curClass, scopeManager, recordMap)

    override val startCharacter = '<'
    override val endCharacter = '>'
}
