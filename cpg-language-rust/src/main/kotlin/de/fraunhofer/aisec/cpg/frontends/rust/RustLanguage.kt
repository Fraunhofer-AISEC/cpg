/*
 * Copyright (c) 2026, Fraunhofer AISEC. All rights reserved.
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

import de.fraunhofer.aisec.cpg.frontends.*
import de.fraunhofer.aisec.cpg.graph.HasOverloadedOperation
import de.fraunhofer.aisec.cpg.graph.scopes.Symbol
import de.fraunhofer.aisec.cpg.graph.statements.expressions.BinaryOperator
import de.fraunhofer.aisec.cpg.graph.statements.expressions.UnaryOperator
import de.fraunhofer.aisec.cpg.graph.types.*
import kotlin.reflect.KClass
import org.neo4j.ogm.annotation.Transient

/**
 * The [Language] definition for Rust. It currently supports basic types and operators.
 *
 * More information can be found in the [Rust Reference](https://doc.rust-lang.org/reference/).
 */
class RustLanguage :
    Language<RustLanguageFrontend>(),
    HasShortCircuitOperators,
    HasGenerics,
    HasStructs,
    HasFirstClassFunctions,
    HasGlobalFunctions,
    HasGlobalVariables,
    HasAnonymousIdentifier,
    HasFunctionPointers,
    HasFunctionStyleConstruction,
    HasOperatorOverloading {
    override val fileExtensions = listOf("rs")
    override val namespaceDelimiter = "::"

    override val frontend: KClass<out RustLanguageFrontend> = RustLanguageFrontend::class
    override val conjunctiveOperators = listOf("&&")
    override val disjunctiveOperators = listOf("||")

    override val startCharacter = '<'
    override val endCharacter = '>'

    override val compoundAssignmentOperators =
        setOf("+=", "-=", "*=", "/=", "%=", "&=", "|=", "^=", "<<=", ">>=")

    @Transient
    override val overloadedOperatorNames:
        Map<Pair<KClass<out HasOverloadedOperation>, String>, Symbol> =
        mapOf(
            UnaryOperator::class of "!" to "Not::not",
            UnaryOperator::class of "*" to "Deref::deref",
            UnaryOperator::class of "-" to "Neg::neg",
            BinaryOperator::class of "!=" to "PartialEq::ne",
            BinaryOperator::class of "%" to "Rem::rem",
            BinaryOperator::class of "%=" to "RemAssign::rem_assign",
            BinaryOperator::class of "&" to "BitAnd::bitand",
            BinaryOperator::class of "&=" to "BitAndAssign::bitand_assign",
            BinaryOperator::class of "*" to "Mul::mul",
            BinaryOperator::class of "*=" to "MulAssign::mul_assign",
            BinaryOperator::class of "+" to "Add::add",
            BinaryOperator::class of "+=" to "AddAssign::add_assign",
            BinaryOperator::class of "-" to "Sub::sub",
            BinaryOperator::class of "-=" to "SubAssign::sub_assign",
            BinaryOperator::class of "/" to "Div::div",
            BinaryOperator::class of "/=" to "DivAssign::div_assign",
            BinaryOperator::class of "<<" to "Shl::shl",
            BinaryOperator::class of "<<=" to "ShlAssign::shl_assign",
            BinaryOperator::class of "<" to "PartialOrd::lt",
            BinaryOperator::class of "<=" to "PartialOrd::le",
            BinaryOperator::class of "==" to "PartialEq::eq",
            BinaryOperator::class of ">" to "PartialOrd::gt",
            BinaryOperator::class of ">=" to "PartialOrd::ge",
            BinaryOperator::class of ">>" to "Shr::shr",
            BinaryOperator::class of ">>=" to "ShrAssign::shr_assign",
            BinaryOperator::class of "^" to "BitXor::bitxor",
            BinaryOperator::class of "^=" to "BitXorAssign::bitxor_assign",
            BinaryOperator::class of "|" to "BitOr::bitor",
            BinaryOperator::class of "|=" to "BitOrAssign::bitor_assign",
        )

    override fun propagateTypeOfBinaryOperation(
        operatorCode: String?,
        lhsType: Type,
        rhsType: Type,
        hint: BinaryOperator?,
    ): Type =
        when {
            operatorCode == "+" && lhsType is StringType && rhsType is StringType ->
                builtInTypes["String"] as Type
            else -> super.propagateTypeOfBinaryOperation(operatorCode, lhsType, rhsType, hint)
        }

    /** See [Documentation](https://doc.rust-lang.org/reference/types.html). */
    override val builtInTypes =
        mapOf(
            "bool" to BooleanType(typeName = "bool", language = this),
            "i8" to IntegerType("i8", 8, this, NumericType.Modifier.SIGNED),
            "i16" to IntegerType("i16", 16, this, NumericType.Modifier.SIGNED),
            "i32" to IntegerType("i32", 32, this, NumericType.Modifier.SIGNED),
            "i64" to IntegerType("i64", 64, this, NumericType.Modifier.SIGNED),
            "i128" to IntegerType("i128", 128, this, NumericType.Modifier.SIGNED),
            "isize" to IntegerType("isize", null, this, NumericType.Modifier.SIGNED),
            "u8" to IntegerType("u8", 8, this, NumericType.Modifier.UNSIGNED),
            "u16" to IntegerType("u16", 16, this, NumericType.Modifier.UNSIGNED),
            "u32" to IntegerType("u32", 32, this, NumericType.Modifier.UNSIGNED),
            "u64" to IntegerType("u64", 64, this, NumericType.Modifier.UNSIGNED),
            "u128" to IntegerType("u128", 128, this, NumericType.Modifier.UNSIGNED),
            "usize" to IntegerType("usize", null, this, NumericType.Modifier.UNSIGNED),
            "f32" to FloatingPointType("f32", 32, this, NumericType.Modifier.NOT_APPLICABLE),
            "f64" to FloatingPointType("f64", 64, this, NumericType.Modifier.NOT_APPLICABLE),
            "str" to StringType("str", language = this, primitive = true),
            "String" to StringType("String", language = this, primitive = false),
            "char" to IntegerType("char", 32, this, NumericType.Modifier.UNSIGNED),
        )
}
