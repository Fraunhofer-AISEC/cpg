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
import de.fraunhofer.aisec.cpg.graph.Name
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.RecordDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.MemberExpression
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.passes.CallResolver
import java.util.regex.Pattern

/**
 * A language trait is a feature or trait that is common to a group of programming languages. Any
 * [Language] that supports them should implement the desired trait interface. Examples could be the
 * support of pointers, support for templates or generics.
 *
 * Currently, this interface has no methods. However, in the future, this could be used to execute
 * language/frontend-specific code for the particular trait. This could help to fine-tune the
 * [de.fraunhofer.aisec.cpg.passes.CallResolver] for specific languages.
 */
interface LanguageTrait

/** A language trait, that specifies that this language has support for templates or generics. */
interface HasTemplates : LanguageTrait {

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
        scopeManager: ScopeManager,
        currentTU: TranslationUnitDeclaration
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
     * A function that can be used to fine-tune resolution of a normal (non-method) [call].
     *
     * Note: The function itself should NOT set the [CallExpression.invokes] but rather return a
     * list of possible candidates.
     *
     * @return a list of [FunctionDeclaration] candidates.
     */
    fun refineNormalCallResolution(
        call: CallExpression,
        scopeManager: ScopeManager,
        currentTU: TranslationUnitDeclaration
    ): List<FunctionDeclaration>

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
        scopeManager: ScopeManager,
        currentTU: TranslationUnitDeclaration,
        callResolver: CallResolver
    ): List<FunctionDeclaration>

    /**
     * A function to fine-tune the results of [CallResolver.getInvocationCandidatesFromRecord],
     * which retrieves a list of [FunctionDeclaration] candidates from a [RecordDeclaration].
     *
     * Note: The function itself should NOT set the [CallExpression.invokes] but rather return a
     * list of possible candidates.
     *
     * @return a list of [FunctionDeclaration] candidates.
     */
    fun refineInvocationCandidatesFromRecord(
        recordDeclaration: RecordDeclaration,
        call: CallExpression,
        namePattern: Pattern
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
        recordMap: Map<Name, RecordDeclaration>
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
    // '&&', 'and', '^'
    val conjunctiveOperators: List<String>
    // '||', 'or', 'v'
    val disjunctiveOperators: List<String>
}
