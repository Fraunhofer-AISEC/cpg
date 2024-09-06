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

import de.fraunhofer.aisec.cpg.frontends.HasFunctionalConstructs
import de.fraunhofer.aisec.cpg.frontends.HasOperatorOverloading
import de.fraunhofer.aisec.cpg.frontends.HasShortCircuitOperators
import de.fraunhofer.aisec.cpg.frontends.Language
import de.fraunhofer.aisec.cpg.frontends.of
import de.fraunhofer.aisec.cpg.graph.HasOverloadedOperation
import de.fraunhofer.aisec.cpg.graph.autoType
import de.fraunhofer.aisec.cpg.graph.declarations.ParameterDeclaration
import de.fraunhofer.aisec.cpg.graph.scopes.Symbol
import de.fraunhofer.aisec.cpg.graph.statements.expressions.BinaryOperator
import de.fraunhofer.aisec.cpg.graph.statements.expressions.UnaryOperator
import de.fraunhofer.aisec.cpg.graph.types.*
import kotlin.reflect.KClass
import org.neo4j.ogm.annotation.Transient

/** The Python language. */
class PythonLanguage :
    Language<PythonLanguageFrontend>(),
    HasShortCircuitOperators,
    HasOperatorOverloading,
    HasFunctionalConstructs {
    override val fileExtensions = listOf("py", "pyi")
    override val namespaceDelimiter = "."
    @Transient
    override val frontend: KClass<out PythonLanguageFrontend> = PythonLanguageFrontend::class
    override val conjunctiveOperators = listOf("and")
    override val disjunctiveOperators = listOf("or")

    /**
     * All operators which perform and assignment and an operation using lhs and rhs. See
     * https://docs.python.org/3/library/operator.html#in-place-operators
     */
    override val compoundAssignmentOperators =
        setOf("+=", "-=", "*=", "**=", "/=", "//=", "%=", "<<=", ">>=", "&=", "|=", "^=", "@=")

    // https://docs.python.org/3/reference/datamodel.html#special-method-names
    @Transient
    override val overloadedOperatorNames:
        Map<Pair<KClass<out HasOverloadedOperation>, String>, Symbol> =
        mapOf(
            UnaryOperator::class of
                "[]" to
                "__getitem__", // ... then x[i] is roughly equivalent to type(x).__getitem__(x, i)
            BinaryOperator::class of "<" to "__lt__",
            BinaryOperator::class of "<=" to "__le__",
            BinaryOperator::class of "==" to "__eq__",
            BinaryOperator::class of "!=" to "__ne__",
            BinaryOperator::class of ">" to "__gt__",
            BinaryOperator::class of ">=" to "__ge__",
            BinaryOperator::class of "+" to "__add__",
            BinaryOperator::class of "-" to "__sub__",
            BinaryOperator::class of "*" to "__mul__",
            BinaryOperator::class of "@" to "__matmul__",
            BinaryOperator::class of "/" to "__truediv__",
            BinaryOperator::class of "//" to "__floordiv__",
            BinaryOperator::class of "%" to "__mod__",
            BinaryOperator::class of "**" to "__pow__",
            BinaryOperator::class of "<<" to "__lshift__",
            BinaryOperator::class of ">>" to "__rshift__",
            BinaryOperator::class of "&" to "__and__",
            BinaryOperator::class of "^" to "__xor__",
            BinaryOperator::class of "|" to "__or__",
            BinaryOperator::class of "+=" to "__iadd__",
            BinaryOperator::class of "-=" to "__isub__",
            BinaryOperator::class of "*=" to "__imul__",
            BinaryOperator::class of "@=" to "__imatmul__",
            BinaryOperator::class of "/=" to "__itruediv__",
            BinaryOperator::class of "//=" to "__ifloordiv__",
            BinaryOperator::class of "%=" to "__imod__",
            BinaryOperator::class of "**=" to "__ipow__",
            BinaryOperator::class of "<<=" to "__ilshift__",
            BinaryOperator::class of ">>=" to "__irshift__",
            BinaryOperator::class of "&=" to "__iand__",
            BinaryOperator::class of "^=" to "__ixor__",
            BinaryOperator::class of "|=" to "__ior__",
            UnaryOperator::class of "-" to "__neg__",
            UnaryOperator::class of "+" to "__pos__",
            UnaryOperator::class of "~" to "__invert__",
            UnaryOperator::class of
                "()" to
                "__call__", // ... x(arg1, arg2, ...) roughly translates to type(x).__call__(x,
            // arg1, ...)
        )

    /** See [Documentation](https://docs.python.org/3/library/stdtypes.html#). */
    @Transient
    override val builtInTypes =
        mapOf(
            "bool" to BooleanType("bool", language = this),
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

    override fun propagateTypeOfBinaryOperation(operation: BinaryOperator): Type {
        val autoType = autoType()
        if (
            operation.operatorCode == "/" &&
                operation.lhs.type is NumericType &&
                operation.rhs.type is NumericType
        ) {
            // In Python, the / operation automatically casts the result to a float
            return getSimpleTypeOf("float") ?: autoType
        } else if (
            operation.operatorCode == "//" &&
                operation.lhs.type is NumericType &&
                operation.rhs.type is NumericType
        ) {
            return if (operation.lhs.type is IntegerType && operation.rhs.type is IntegerType) {
                // In Python, the // operation keeps the type as an int if both inputs are integers
                // or casts it to a float otherwise.
                getSimpleTypeOf("int") ?: autoType
            } else {
                getSimpleTypeOf("float") ?: autoType
            }
        }

        // The rest behaves like other languages
        return super.propagateTypeOfBinaryOperation(operation)
    }

    companion object {
        /**
         * This is a "modifier" to differentiate parameters in functions that are "positional" only.
         * This information will be stored in [ParameterDeclaration.modifiers] so that we can use is
         * later in call resolving.
         */
        const val MODIFIER_POSITIONAL_ONLY_ARGUMENT = "posonlyarg"

        /**
         * This is a "modifier" to differentiate parameters in functions that are "keyword" only.
         * This information will be stored in [ParameterDeclaration.modifiers] so that we can use is
         * later in call resolving.
         */
        const val MODIFIER_KEYWORD_ONLY_ARGUMENT = "kwonlyarg"
    }
}
