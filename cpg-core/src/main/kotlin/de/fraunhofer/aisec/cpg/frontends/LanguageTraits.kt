/*
 * Copyright (c) 2021, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.frontends

import de.fraunhofer.aisec.cpg.ScopeManager
import de.fraunhofer.aisec.cpg.TranslationContext
import de.fraunhofer.aisec.cpg.graph.HasOperatorCode
import de.fraunhofer.aisec.cpg.graph.HasOverloadedOperation
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.RecordDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.graph.scopes.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.passes.*
import kotlin.reflect.KClass

/**
 * A language trait is a feature or trait that is common to a group of programming languages. Any
 * [Language] that supports them should implement the desired trait interface. Examples could be the
 * support of pointers, support for templates or generics.
 *
 * Currently, this interface has no methods. However, in the future, this could be used to execute
 * language/frontend-specific code for the particular trait. This could help to fine-tune the
 * [SymbolResolver] for specific languages.
 */
interface LanguageTrait

/** A language trait, that specifies that this language has support for templates or generics. */
interface HasGenerics : LanguageTrait {
    /** The char starting the template specific code (e.g. `<`) */
    val startCharacter: Char

    /** The char ending the template specific code (e.g. `>`) */
    val endCharacter: Char
}

/** A language trait, that specifies that this language has support for templates or generics. */
interface HasTemplates : HasGenerics {
    /**
     * This function can be used to fine-tune the resolution of template function calls.
     *
     * Note: The function itself should NOT set the [CallExpression.invokes] but rather return a
     * list of possible candidates.
     *
     * @return a pair in which the first member denotes whether resolution was successful and the
     *   second parameter is a list of [FunctionDeclaration] candidates.
     */
    fun handleTemplateFunctionCalls(
        curClass: RecordDeclaration?,
        templateCall: CallExpression,
        applyInference: Boolean,
        ctx: TranslationContext,
        currentTU: TranslationUnitDeclaration?,
        needsExactMatch: Boolean
    ): Pair<Boolean, List<FunctionDeclaration>>
}

/**
 * A language trait that specifies, that this language has support for default arguments, e.g. in
 * functions.
 */
interface HasDefaultArguments : LanguageTrait

/**
 * A language trait that specifies that this language has a complex call resolution that we need to
 * fine-tune in the language implementation.
 */
interface HasComplexCallResolution : LanguageTrait {
    /**
     * A function that can be used to fine-tune resolution of a method [call].
     *
     * Note: The function itself should NOT set the [CallExpression.invokes] but rather return a
     * list of possible candidates.
     *
     * @return a list of [FunctionDeclaration] candidates.
     */
    fun refineMethodCallResolution(
        curClass: RecordDeclaration?,
        possibleContainingTypes: Set<Type>,
        call: CallExpression,
        ctx: TranslationContext,
        currentTU: TranslationUnitDeclaration,
        callResolver: SymbolResolver
    ): List<FunctionDeclaration>
}

/** A language trait that specifies if the language supports function pointers. */
interface HasFunctionPointers : LanguageTrait

/**
 * A language trait that specifies if the language has the concept of "structs". The alternative is
 * to use classes. Note that some languages can have both and that in some languages structs and
 * classes are essentially the same. This is mostly used to determine if an inferred record
 * declaration *can be* a struct or *must be* a class.
 */
interface HasStructs : LanguageTrait

/**
 * A language trait that specifies if the language has the concept of "classes". The alternative is
 * to use structs. Note that some languages can have both and that in some languages structs and
 * classes are essentially the same. This is mostly used to determine if an inferred record
 * declaration *can be* a class or *must be* a struct.
 */
interface HasClasses : LanguageTrait

/**
 * A language trait, that specifies that this language has support for superclasses. If so, we
 * should consider the specified superclass keyword to resolve calls etc.
 */
interface HasSuperClasses : LanguageTrait {
    /**
     * Determines which keyword is used to access functions, etc. of the superclass of an object
     * (often "super).
     */
    val superClassKeyword: String

    fun handleSuperCall(
        callee: MemberExpression,
        curClass: RecordDeclaration,
        scopeManager: ScopeManager,
    ): Boolean
}

/**
 * A language trait, that specifies that this language has certain qualifiers. If so, we should
 * consider them when parsing the types.
 */
interface HasQualifier : LanguageTrait {
    /** The qualifiers which exist in the language. */
    val qualifiers: List<String>
}

/**
 * A language trait, that specifies that this language has "elaborated type specifiers". If so, we
 * should consider them when parsing the types.
 */
interface HasElaboratedTypeSpecifier : LanguageTrait {
    val elaboratedTypeSpecifier: List<String>
}

/**
 * A language trait, that specifies that this language has specifiers which let us conclude that we
 * do not know the type. If so, we should consider them when parsing the types.
 */
interface HasUnknownType : LanguageTrait {
    val unknownTypeString: List<String>
}

/**
 * A language trait, that specifies that this language has binary operators that will short-circuit
 * evaluation if the logical result is already known: '&&', '||' in Java or 'and','or' in Python
 */
interface HasShortCircuitOperators : LanguageTrait {
    /**
     * Operations which only execute the rhs of a binary operation if the lhs is `true`. Typically,
     * these are `&&`, `and` or `^`
     */
    val conjunctiveOperators: List<String>

    /**
     * Operations which only execute the rhs of a binary operation if the lhs is `false`. Typically,
     * these are `||`, `or` or `v`
     */
    val disjunctiveOperators: List<String>

    /**
     * The union of [conjunctiveOperators] and [disjunctiveOperators], i.e., all binary operators of
     * this language which result in some kind of branching behavior.
     */
    val operatorCodes: Set<String>
        get() = conjunctiveOperators.union(disjunctiveOperators)
}

/**
 * A language trait, that specifies that this language treats functions "first-class citizens",
 * meaning they can be assigned to variables and passed as arguments to other functions.
 */
interface HasFirstClassFunctions : LanguageTrait

/**
 * A language trait, that specifies that this language has an "anonymous" identifier, used for
 * unused parameters or suppressed assignments.
 */
interface HasAnonymousIdentifier : LanguageTrait {
    val anonymousIdentifier: String
        get() = "_"
}

/**
 * A language trait, that specifies that this language has global variables directly in the
 * [GlobalScope], i.e., not within a namespace, but directly contained in a
 * [TranslationUnitDeclaration].
 */
interface HasGlobalVariables : LanguageTrait

/**
 * A language trait, that specifies that the language has so-called functional style casts, meaning
 * that they look like regular call expressions. Since we can therefore not distinguish between a
 * [CallExpression] and a [CastExpression], we need to employ an additional pass
 * ([ReplaceCallCastPass]) after the initial language frontends are done.
 */
interface HasFunctionalCasts : LanguageTrait

/**
 * A language trait that specifies that this language allowed overloading functions, meaning that
 * multiple functions can share the same name with different parameters.
 */
interface HasFunctionOverloading : LanguageTrait

/** A language trait that specifies that this language allows overloading of operators. */
interface HasOperatorOverloading : LanguageTrait {

    /**
     * A map of operator codes and function names acting as overloaded operators. The key is a pair
     * of the class and [HasOperatorCode.operatorCode] (ideally created by [of]) and the value is
     * the name of the function.
     */
    val overloadedOperatorNames: Map<Pair<KClass<out HasOverloadedOperation>, String>, Symbol>
}

/**
 * Creates a [Pair] of class and operator code used in
 * [HasOperatorOverloading.overloadedOperatorNames].
 */
inline infix fun <reified T : HasOverloadedOperation> KClass<T>.of(
    operatorCode: String
): Pair<KClass<T>, String> {
    return Pair(T::class, operatorCode)
}
