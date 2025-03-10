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

import de.fraunhofer.aisec.cpg.TranslationContext
import de.fraunhofer.aisec.cpg.frontends.*
import de.fraunhofer.aisec.cpg.graph.AstNode
import de.fraunhofer.aisec.cpg.graph.HasOverloadedOperation
import de.fraunhofer.aisec.cpg.graph.Name
import de.fraunhofer.aisec.cpg.graph.autoType
import de.fraunhofer.aisec.cpg.graph.declarations.ParameterDeclaration
import de.fraunhofer.aisec.cpg.graph.scopes.Symbol
import de.fraunhofer.aisec.cpg.graph.statements.expressions.BinaryOperator
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Reference
import de.fraunhofer.aisec.cpg.graph.statements.expressions.UnaryOperator
import de.fraunhofer.aisec.cpg.graph.types.*
import de.fraunhofer.aisec.cpg.helpers.Util.warnWithFileLocation
import de.fraunhofer.aisec.cpg.helpers.neo4j.SimpleNameConverter
import java.io.File
import kotlin.reflect.KClass
import org.neo4j.ogm.annotation.Transient
import org.neo4j.ogm.annotation.typeconversion.Convert

/** The Python language. */
class PythonLanguage(ctx: TranslationContext) :
    Language<PythonLanguageFrontend>(ctx),
    HasShortCircuitOperators,
    HasOperatorOverloading,
    HasFunctionStyleConstruction,
    HasMemberExpressionAmbiguity,
    HasBuiltins,
    HasDefaultArguments {
    override val fileExtensions = listOf("py", "pyi")
    override val namespaceDelimiter = "."
    @Convert(value = SimpleNameConverter::class)
    override val builtinsNamespace: Name = Name("builtins")

    @Transient
    override val frontend: KClass<out PythonLanguageFrontend> = PythonLanguageFrontend::class
    override val conjunctiveOperators = listOf("and")
    override val disjunctiveOperators = listOf("or")

    /**
     * You can either use `=` or `:=` in Python. But the latter is only available in a "named
     * expression" (`a = (x := 1)`). We still need to include both however, otherwise
     * [Reference.access] will not be set correctly in "named expressions".
     */
    override val simpleAssignmentOperators: Set<String>
        get() = setOf("=", ":=")

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
            "bool" to BooleanType(ctx, "bool", language = this),
            "int" to
                IntegerType(
                    ctx,
                    "int",
                    Integer.MAX_VALUE,
                    this,
                    NumericType.Modifier.NOT_APPLICABLE,
                ), // Unlimited precision
            "float" to
                FloatingPointType(
                    ctx,
                    "float",
                    32,
                    this,
                    NumericType.Modifier.NOT_APPLICABLE,
                ), // This depends on the implementation
            "complex" to
                NumericType(
                    ctx,
                    "complex",
                    null,
                    this,
                    NumericType.Modifier.NOT_APPLICABLE,
                ), // It's two floats
            "str" to StringType(ctx, "str", this, listOf()),
            "list" to
                ListType(
                    ctx,
                    typeName = "list",
                    elementType = ObjectType(ctx, "object", listOf(), false, this),
                    language = this,
                ),
            "tuple" to
                ListType(
                    ctx,
                    typeName = "tuple",
                    elementType = ObjectType(ctx, "object", listOf(), false, this),
                    language = this,
                ),
            "dict" to
                MapType(
                    ctx,
                    typeName = "dict",
                    elementType = ObjectType(ctx, "object", listOf(), false, this),
                    language = this,
                ),
            "set" to
                SetType(
                    ctx,
                    typeName = "set",
                    elementType = ObjectType(ctx, "object", listOf(), false, this),
                    language = this,
                ),
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

    override fun tryCast(
        type: Type,
        targetType: Type,
        hint: HasType?,
        targetHint: HasType?,
    ): CastResult {
        // Parameters in python do not have a static type. Therefore, we need to match for all types
        // when trying to cast one type to the type of a function parameter at *runtime*
        if (targetHint is ParameterDeclaration) {
            // However, if we find type hints, we at least want to issue a warning if the types
            // would not match
            if (hint != null && targetType !is UnknownType && targetType !is AutoType) {
                val match = super.tryCast(type, targetType, hint, targetHint)
                if (match == CastNotPossible && hint is AstNode) {
                    warnWithFileLocation(
                        hint,
                        log,
                        "Argument type of call to {} ({}) does not match type annotation on the function parameter ({}), but since Python does have runtime checks, we ignore this",
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

    /**
     * Returns the files that can represent the given name. This includes all possible file
     * extensions and the name plus the `__init__` identifier, as this is the name for declaration
     * files if the namespace has sub-namespaces.
     */
    fun nameToLanguageFiles(name: Name): Set<File> {
        val filesForNamespace =
            fileExtensions
                .flatMap { extension ->
                    setOf(name, Name(IDENTIFIER_INIT, name)).map {
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

        /**
         * The initialization identifier of python, used for constructors and as name for module
         * initialization.
         */
        const val IDENTIFIER_INIT = "__init__"
    }
}
