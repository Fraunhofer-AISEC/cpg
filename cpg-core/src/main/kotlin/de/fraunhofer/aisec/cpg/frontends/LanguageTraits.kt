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

import de.fraunhofer.aisec.cpg.TranslationContext
import de.fraunhofer.aisec.cpg.graph.HasOperatorCode
import de.fraunhofer.aisec.cpg.graph.HasOverloadedOperation
import de.fraunhofer.aisec.cpg.graph.LanguageProvider
import de.fraunhofer.aisec.cpg.graph.Name
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.RecordDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.graph.scopes.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.passes.*
import java.io.File
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
        needsExactMatch: Boolean,
    ): Pair<Boolean, List<FunctionDeclaration>>
}

/**
 * A language trait that specifies, that this language has support for default arguments, e.g. in
 * functions.
 */
interface HasDefaultArguments : LanguageTrait

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
     * (often `super`).
     */
    val superClassKeyword: String

    fun SymbolResolver.handleSuperExpression(
        memberExpression: MemberExpression,
        curClass: RecordDeclaration,
    ): Boolean
}

/**
 * A language trait, that specifies that this language has support for implicit receiver, e.g., that
 * one can omit references to a base such as `this`. Common examples are C++ and Java.
 *
 * This is contrast to languages such as Python and Go where the name of the receiver such as `self`
 * is always required to access a field or method.
 *
 * We need this information to make a decision which symbols or scopes to consider when doing an
 * unqualified lookup of a symbol in [Scope.lookupSymbol]. More specifically, we need to skip the
 * symbols of a [RecordScope] if the language does NOT have this trait.
 */
interface HasImplicitReceiver : LanguageTrait {

    val receiverName: String
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
 * A language trait, that specifies that this language has global functions directly in the
 * [GlobalScope], i.e., not within a namespace, but directly contained in a
 * [TranslationUnitDeclaration]. For example, C++ has global functions, Java and Go do not (as every
 * function is either in a class or a namespace).
 */
interface HasGlobalFunctions : LanguageTrait

/**
 * A common trait for classes, in which supposed member expressions (and thus also member calls) in
 * the form of "a.b" have an ambiguity between a real field/method access (when "a" is an object)
 * and a qualified call because of an import, if "a" is an import / namespace.
 *
 * We can only resolve this after we have dealt with imports and know all symbols. Therefore, we
 * invoke the [ResolveMemberExpressionAmbiguityPass].
 */
interface HasMemberExpressionAmbiguity : LanguageTrait

/**
 * A common super-class for all language traits that arise because they are an ambiguity of a
 * function call, e.g., function-style casts. This means that we cannot differentiate between a
 * [CallExpression] and other expressions during the frontend, and we need to invoke the
 * [ResolveCallExpressionAmbiguityPass] to resolve this.
 */
sealed interface HasCallExpressionAmbiguity : LanguageTrait

/**
 * A language trait, that specifies that the language has so-called functional style casts, meaning
 * that they look like regular call expressions. Since we can therefore not distinguish between a
 * [CallExpression] and a [CastExpression], we need to employ an additional pass
 * ([ResolveCallExpressionAmbiguityPass]) after the initial language frontends are done.
 */
interface HasFunctionStyleCasts : HasCallExpressionAmbiguity

/**
 * A language trait, that specifies that the language has functional style (object) construction,
 * meaning that constructor calls look like regular call expressions (usually meaning that the
 * language has no dedicated `new` keyword).
 *
 * Since we can therefore not distinguish between a [CallExpression] and a [ConstructExpression] in
 * the frontend, we need to employ an additional pass ([ResolveCallExpressionAmbiguityPass]) after
 * the initial language frontends are done.
 */
interface HasFunctionStyleConstruction : HasCallExpressionAmbiguity

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

    /**
     * Returns the matching operator code for [name] in [overloadedOperatorNames]. While
     * [overloadedOperatorNames] can have multiple entries for a single operator code (e.g. to
     * differentiate between unary and binary ops), we only ever allow one distinct operator code
     * for a specific symbol. If non such distinct operator code is found, null is returned.
     */
    fun operatorCodeFor(name: Symbol): String? {
        return overloadedOperatorNames
            .filterValues { it == name }
            .keys
            .map { it.second }
            .distinct()
            .singleOrNull()
    }
}

/**
 * A language trait, that specifies that this language has variables and functions that are built
 * in. For resolution this means that a file may be included into the include paths that contains
 * the declaration or entire definition of the builtin functions and variables. The file should be
 * imported unconditionally from the use in import statements, as the contained declarations are
 * available without explicit importing.
 */
interface HasBuiltins : LanguageTrait {
    /** Returns the namespace under which builtins exist. */
    val builtinsNamespace: Name

    /** Name of files that may contain the builtin functions of a language */
    val builtinsFileCandidates: Set<File>
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

/** Checks whether the name for a function (as [CharSequence]) is a known operator name. */
context(LanguageProvider)
val CharSequence.isKnownOperatorName: Boolean
    get() {
        val language = language
        if (language !is HasOperatorOverloading) {
            return false
        }

        // If this is a parsed name, we only are interested in the local name
        val name =
            if (this is Name) {
                this.localName
            } else {
                this
            }

        return language.overloadedOperatorNames.containsValue(name)
    }
