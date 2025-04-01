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
import de.fraunhofer.aisec.cpg.frontends.*
import de.fraunhofer.aisec.cpg.graph.declarations.Declaration
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.RecordDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.expressions.BinaryOperator
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.MemberExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Reference
import de.fraunhofer.aisec.cpg.graph.types.*
import de.fraunhofer.aisec.cpg.passes.SymbolResolver
import kotlin.reflect.KClass
import org.neo4j.ogm.annotation.Transient

/** The Java language. */
open class JavaLanguage :
    Language<JavaLanguageFrontend>(),
    HasClasses,
    HasSuperClasses,
    HasGenerics,
    HasQualifier,
    HasUnknownType,
    HasShortCircuitOperators,
    HasFunctionOverloading,
    HasImplicitReceiver {
    override val fileExtensions = listOf("java")
    override val namespaceDelimiter = "."
    @Transient override val frontend: KClass<out JavaLanguageFrontend> = JavaLanguageFrontend::class
    override val superClassKeyword = "super"

    override val qualifiers = listOf("final", "volatile")
    override val unknownTypeString = listOf("var")
    override val conjunctiveOperators = listOf("&&")
    override val disjunctiveOperators = listOf("||")

    /**
     * All operators which perform and assignment and an operation using lhs and rhs. See
     * https://docs.oracle.com/javase/tutorial/java/nutsandbolts/operators.html
     */
    override val compoundAssignmentOperators =
        setOf("+=", "-=", "*=", "/=", "%=", "<<=", ">>=", ">>>=", "&=", "|=", "^=")

    /**
     * See
     * [Java Language Specification](https://docs.oracle.com/javase/specs/jls/se19/html/jls-4.html).
     */
    @Transient
    @JsonIgnore
    override val builtInTypes =
        mapOf(
            "void" to IncompleteType(language = this),
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
            "java.lang.Integer" to
                IntegerType("java.lang.Integer", 32, this, NumericType.Modifier.SIGNED),
            "long" to IntegerType("long", 64, this, NumericType.Modifier.SIGNED),

            // Floating-Point Types:
            // https://docs.oracle.com/javase/specs/jls/se19/html/jls-4.html#jls-4.2.3
            "float" to FloatingPointType("float", 32, this, NumericType.Modifier.SIGNED),
            "double" to FloatingPointType("double", 64, this, NumericType.Modifier.SIGNED),

            // String: https://docs.oracle.com/javase/specs/jls/se19/html/jls-4.html#jls-4.3.3
            "String" to StringType("java.lang.String", this),
            "java.lang.String" to StringType("java.lang.String", this),
        )

    override fun propagateTypeOfBinaryOperation(
        operatorCode: String?,
        lhsType: Type,
        rhsType: Type,
        hint: BinaryOperator?,
    ): Type {
        return if (
            operatorCode == "+" &&
                (lhsType as? IntegerType)?.name?.localName?.equals("char") == true &&
                (rhsType as? IntegerType)?.name?.localName?.equals("char") == true
        ) {
            getSimpleTypeOf("int") ?: UnknownType.getUnknownType(this)
        } else super.propagateTypeOfBinaryOperation(operatorCode, lhsType, rhsType, hint)
    }

    override fun SymbolResolver.handleSuperExpression(
        memberExpression: MemberExpression,
        curClass: RecordDeclaration,
    ) = handleSuperExpressionHelper(memberExpression, curClass)

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
                ref.candidates = ref.candidates.filter { it is FunctionDeclaration }.toSet()
            } else {
                ref.candidates = ref.candidates.filter { it is VariableDeclaration }.toSet()
            }
        }

        return super.bestViableReferenceCandidate(ref)
    }

    override val startCharacter = '<'
    override val endCharacter = '>'
    override val receiverName: String
        get() = "this"
}
