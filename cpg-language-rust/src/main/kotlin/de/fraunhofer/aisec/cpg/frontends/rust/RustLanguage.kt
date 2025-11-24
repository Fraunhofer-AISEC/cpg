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
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.declarations.ParameterDeclaration
import de.fraunhofer.aisec.cpg.graph.scopes.Symbol
import de.fraunhofer.aisec.cpg.graph.statements.expressions.BinaryOperator
import de.fraunhofer.aisec.cpg.graph.statements.expressions.UnaryOperator
import de.fraunhofer.aisec.cpg.graph.types.*
import de.fraunhofer.aisec.cpg.helpers.Util.warnWithFileLocation
import de.fraunhofer.aisec.cpg.helpers.neo4j.SimpleNameConverter
import de.fraunhofer.aisec.cpg.persistence.DoNotPersist
import kotlin.reflect.KClass
import org.neo4j.ogm.annotation.Transient
import org.neo4j.ogm.annotation.typeconversion.Convert

/** The Rust language. */
class RustLanguage :
    Language<RustLanguageFrontend>(),
    HasShortCircuitOperators,
    HasOperatorOverloading,
    HasFunctionStyleConstruction
    // ! HasDefaultArguments
    // HasMemberExpressionAmbiguity,
    // HasBuiltins,

{
    override val fileExtensions = listOf("rs")
    override val namespaceDelimiter = "."
    @Convert(value = SimpleNameConverter::class)
    // override val builtinsNamespace: Name = Name("")
    // override val builtinsFileCandidates = nameToLanguageFiles(builtinsNamespace)

    @Transient
    override val frontend: KClass<out RustLanguageFrontend> = RustLanguageFrontend::class
    override val conjunctiveOperators = listOf("&&")
    override val disjunctiveOperators = listOf("||")

    override val simpleAssignmentOperators: Set<String>
        get() = setOf("=")

    override val compoundAssignmentOperators =
        setOf("+=", "-=", "*=", "/=", "%=", "&=", "<<=", ">>=", "^=", "|=")

    @Transient
    // https://doc.rust-lang.org/book/appendix-02-operators.html
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
            BinaryOperator::class of ">>=" to "ShrAssign::she_assign",
            BinaryOperator::class of "^" to "BitXor::bitxor",
            BinaryOperator::class of "^=" to "BitXorAssign::bitxor_assign",
            BinaryOperator::class of "|" to "BitOr::bitor",
            BinaryOperator::class of "|=" to "BitOrAssign::bitor_assign",
            // It looks like the following two operators are not directly overloaded but rather by
            // the used comparative ops
            // BinaryOperator::class of ".." to "PartialOrd",
            // BinaryOperator::class of "..=" to "PartialOrd",

        )

    /** See [Documentation](https://doc.rust-lang.org/stable/std/index.html#primitives). */
    @Transient
    override val builtInTypes =
        mapOf<String, Type>(
            // https://doc.rust-lang.org/stable/reference/types/boolean.html
            "bool" to BooleanType(typeName = "bool", language = this),
            // https://doc.rust-lang.org/stable/reference/types/numeric.html
            "u8" to IntegerType("u8", 8, this, NumericType.Modifier.UNSIGNED),
            "u16" to IntegerType("u16", 16, this, NumericType.Modifier.UNSIGNED),
            "u32" to IntegerType("u32", 32, this, NumericType.Modifier.UNSIGNED),
            "u64" to IntegerType("u64", 64, this, NumericType.Modifier.UNSIGNED),
            "u128" to IntegerType("u128", 128, this, NumericType.Modifier.UNSIGNED),
            "i8" to IntegerType("i8", 8, this, NumericType.Modifier.SIGNED),
            "i16" to IntegerType("i16", 16, this, NumericType.Modifier.SIGNED),
            "i32" to IntegerType("i32", 32, this, NumericType.Modifier.SIGNED),
            "i64" to IntegerType("i64", 64, this, NumericType.Modifier.SIGNED),
            "i128" to IntegerType("i128", 128, this, NumericType.Modifier.SIGNED),
            "usize" to
                IntegerType(
                    "usize",
                    null /* At least 16 bits, but architecture dependent */,
                    this,
                    NumericType.Modifier.UNSIGNED,
                ),
            "isize" to
                IntegerType(
                    "isize",
                    null /* At least 16 bits, but architecture dependent */,
                    this,
                    NumericType.Modifier.UNSIGNED,
                ),
            "f32" to FloatingPointType("f32", 32, this, NumericType.Modifier.SIGNED),
            "f64" to FloatingPointType("f64", 64, this, NumericType.Modifier.SIGNED),

            // https://doc.rust-lang.org/stable/reference/types/textual.html
            "char" to IntegerType("char", 32, this, NumericType.Modifier.UNSIGNED),
            "str" to
                StringType(
                    typeName = "str",
                    language = this,
                    generics = listOf(),
                    primitive = true, // Debatable whether this is primitive or not
                    mutable = false,
                ),
            "String" to
                StringType(
                    typeName = "String",
                    language = this,
                    generics = listOf(),
                    primitive = false,
                    mutable = true,
                ),
            // https://doc.rust-lang.org/stable/reference/types/never.html
            // Tuples in rust are of fixed size with well-defined element types, so here we just
            // create an empty tuple
            "!" to ObjectType("Never", listOf(), false, this),
            // https://doc.rust-lang.org/stable/reference/types/tuple.html
            "()" to TupleType(types = listOf()),
            // https://doc.rust-lang.org/stable/reference/types/array.html
            // Arrays similarly are of a defined type, but we cannot infer them from the string
            // entry of the mapping
            // types of slices are noted in the same way as arrays, we therefore use them
            // interchangeably
            "[]" to
                ListType(
                    typeName = "array",
                    elementType = ObjectType(),
                    language = this,
                    primitive = false,
                ),
            // https://doc.rust-lang.org/stable/reference/types/function-item.html
            "fn()" to
                FunctionType(
                    typeName = "fn",
                    parameters = listOf(),
                    returnTypes = listOf(),
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
            operatorCode == "+" && lhsType is StringType && rhsType is StringType -> {

                return builtInTypes.get("String") as Type
            }
            else ->
                return super.propagateTypeOfBinaryOperation(operatorCode, lhsType, rhsType, hint)
        }
    }

    /** Todo this is probably not possible */
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

    companion object {}
}
