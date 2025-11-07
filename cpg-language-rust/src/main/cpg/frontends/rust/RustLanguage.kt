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
package de.fraunhofer.aisec.cpg.frontends.rust

import de.fraunhofer.aisec.cpg.evaluation.ValueEvaluator
import de.fraunhofer.aisec.cpg.frontends.*
import de.fraunhofer.aisec.cpg.graph.HasOverloadedOperation
import de.fraunhofer.aisec.cpg.graph.Name
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.declarations.ParameterDeclaration
import de.fraunhofer.aisec.cpg.graph.primitiveType
import de.fraunhofer.aisec.cpg.graph.scopes.Symbol
import de.fraunhofer.aisec.cpg.graph.statements.expressions.BinaryOperator
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Reference
import de.fraunhofer.aisec.cpg.graph.statements.expressions.UnaryOperator
import de.fraunhofer.aisec.cpg.graph.types.*
import de.fraunhofer.aisec.cpg.helpers.Util.warnWithFileLocation
import de.fraunhofer.aisec.cpg.helpers.neo4j.SimpleNameConverter
import de.fraunhofer.aisec.cpg.persistence.DoNotPersist
import java.io.File
import kotlin.reflect.KClass
import org.neo4j.ogm.annotation.Transient
import org.neo4j.ogm.annotation.typeconversion.Convert

/** The Rust language. */
class RustLanguage :
    Language<RustLanguageFrontend>(),
    // HasShortCircuitOperators,
    //HasOperatorOverloading,
    //HasFunctionStyleConstruction,
    //HasMemberExpressionAmbiguity,
    //HasBuiltins,
    //HasDefaultArguments
{
    override val fileExtensions = listOf("rs")
    override val namespaceDelimiter = "."
    @Convert(value = SimpleNameConverter::class)
    //override val builtinsNamespace: Name = Name("")
    //override val builtinsFileCandidates = nameToLanguageFiles(builtinsNamespace)

    @Transient
    override val frontend: KClass<out RustLanguageFrontend> = RustLanguageFrontend::class
    override val conjunctiveOperators = listOf("and")
    override val disjunctiveOperators = listOf("or")


    override val simpleAssignmentOperators: Set<String>
        get() = setOf("=", ":=")


    override val compoundAssignmentOperators =
        setOf("+=", "-=", "*=", "**=", "/=", "//=", "%=", "<<=", ">>=", "&=", "|=", "^=", "@=")


    @Transient // Todo
    override val overloadedOperatorNames:
        Map<Pair<KClass<out HasOverloadedOperation>, String>, Symbol> =
        mapOf(
            BinaryOperator::class of "<" to "gt",
        )

    /** See [Documentation](https://doc.rust-lang.org/stable/std/index.html#primitives). */ // Todo
    @Transient
    override val builtInTypes =
        mapOf(
            "bool" to BooleanType(typeName = "bool", language = this),
            "int" to
                IntegerType(
                    typeName = "int",
                    bitWidth = Integer.MAX_VALUE,
                    language = this,
                    modifier = NumericType.Modifier.NOT_APPLICABLE,
                ), // Unlimited precision
            "float" to
                FloatingPointType(
                    typeName = "float",
                    bitWidth = 32,
                    language = this,
                    modifier = NumericType.Modifier.NOT_APPLICABLE,
                ), // This depends on the implementation
            "complex" to
                NumericType(
                    typeName = "complex",
                    bitWidth = null,
                    language = this,
                    modifier = NumericType.Modifier.NOT_APPLICABLE,
                ), // It's two floats
            "str" to
                StringType(
                    typeName = "str",
                    language = this,
                    generics = listOf(),
                    primitive = false,
                    mutable = false,
                ),
            "list" to
                ListType(
                    typeName = "list",
                    elementType =
                        ObjectType(
                            typeName = "object",
                            generics = listOf(),
                            primitive = false,
                            mutable = true,
                            language = this,
                        ),
                    language = this,
                ),
            "tuple" to
                ListType(
                    typeName = "tuple",
                    elementType =
                        ObjectType(
                            typeName = "object",
                            generics = listOf(),
                            primitive = false,
                            mutable = true,
                            language = this,
                        ),
                    language = this,
                    primitive = true,
                ),
            "dict" to
                MapType(
                    typeName = "dict",
                    elementType =
                        ObjectType(
                            typeName = "object",
                            generics = listOf(),
                            primitive = false,
                            mutable = true,
                            language = this,
                        ),
                    language = this,
                ),
            "set" to
                SetType(
                    typeName = "set",
                    elementType =
                        ObjectType(
                            typeName = "object",
                            generics = listOf(),
                            primitive = false,
                            mutable = true,
                            language = this,
                        ),
                    language = this,
                ),
        )

    @DoNotPersist
    override val evaluator: ValueEvaluator
        get() = ValueEvaluator() // Todo

    override fun propagateTypeOfBinaryOperation(
        operatorCode: String?,
        lhsType: Type,
        rhsType: Type,
        hint: BinaryOperator?,
    ): Type {
        when {
            // Todo adapt this
            operatorCode == "/" && lhsType is NumericType && rhsType is NumericType -> {

                return primitiveType("float")
            }
            operatorCode == "*" && lhsType is StringType && rhsType is NumericType -> {
                return lhsType
            }
            operatorCode == "//" && lhsType is NumericType && rhsType is NumericType -> {
                return if (lhsType is IntegerType && rhsType is IntegerType) {

                    primitiveType("int")
                } else {
                    primitiveType("float")
                }
            }

            // The rest behaves like other languages
            else ->
                return super.propagateTypeOfBinaryOperation(operatorCode, lhsType, rhsType, hint)
        }
    }

    /**
     * Todo this is probably not possible
     */
    override fun tryCast(
        type: Type,
        targetType: Type,
        hint: HasType?,
        targetHint: HasType?,
    ): CastResult {

        if (targetHint is ParameterDeclaration) {
            // However, if we find type hints, we at least want to issue a warning if the types
            // would not match
            if (hint != null && targetType !is UnknownType && targetType !is AutoType) {
                val match = super.tryCast(type, targetType, hint, targetHint)
                if (match == CastNotPossible) {
                    warnWithFileLocation(
                        hint as Node,
                        log,
                        "Argument type of call to {} ({}) does not match type annotation on the function parameter ({}), we ignore this",
                        hint.astParent?.name,
                        type.name,
                        targetType.name,
                    )
                }
            }

            return DirectMatch
        }

        return super.tryCast(type, targetType, hint, targetHint)
    }

    // Todo look for implicit namespace construction depending on file structure in rust to derive this from
    fun nameToLanguageFiles(name: Name): Set<File> {
        val filesForNamespace =
            fileExtensions
                .flatMap { extension ->
                    setOf(name, Name(name)).map {
                        File(
                            it.toString().replace(language.namespaceDelimiter, File.separator) +
                                "." +
                                extension
                        )
                    }
                }
                .toMutableSet()
        return filesForNamespace
    }

    companion object {

    }
}
