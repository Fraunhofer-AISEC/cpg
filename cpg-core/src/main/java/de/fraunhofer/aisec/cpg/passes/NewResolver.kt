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
package de.fraunhofer.aisec.cpg.passes

import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.graph.Name
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.declarations.Declaration
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.graph.duplicate
import de.fraunhofer.aisec.cpg.graph.fqn
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.graph.types.ObjectType
import de.fraunhofer.aisec.cpg.helpers.Util
import de.fraunhofer.aisec.cpg.passes.inference.inferFunction
import de.fraunhofer.aisec.cpg.passes.inference.startInference
import de.fraunhofer.aisec.cpg.passes.scopes.ScopeManager
import de.fraunhofer.aisec.cpg.processing.IVisitor
import de.fraunhofer.aisec.cpg.processing.strategy.Strategy
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * A new approach to variable and call resolving.
 *
 * It depends on the [TypeResolver] to run first.
 */
class NewResolver : Pass() {

    private lateinit var sm: ScopeManager

    override fun accept(t: TranslationResult) {
        // We want to have access to the individual translation units for the inference system
        for (tu in t.translationUnits) {
            tu.accept({ Strategy.AST_FORWARD(it) }, ResolveVisitor(sm, tu))
        }
    }

    inner class ResolveVisitor(val sm: ScopeManager, var tu: TranslationUnitDeclaration) :
        IVisitor<Node?>() {
        fun visit(ref: DeclaredReferenceExpression) {
            ref.refersTo = resolve(ref, tu)
        }

        fun visit(me: MemberExpression) {
            // We need to resolve the base of the member expression first. However, this way, we
            // resolve it twice :(
            (me.base as? DeclaredReferenceExpression)?.let { it.refersTo = resolve(it, tu) }
            me.refersTo = resolve(me, tu)
        }
    }

    fun resolve(ref: DeclaredReferenceExpression, tu: TranslationUnitDeclaration): Declaration? {
        // While discouraged, we cannot make sure that some of the following code is trying to
        // check the current scope using [ScopeManager.currentScope]. We therefore "jump" directly
        // to the scope of the reference to minimize harm.
        sm.jumpTo(ref.scope)

        // Check, if this is a member expression, because if so, we are always in a qualified
        // lookup. Otherwise, we need to decide based on the name.
        val symbols =
            if (ref is MemberExpression) {
                resolveMemberExpression(ref, tu)
            } else {
                resolveDeclaredReferenceExpression(ref)
            }

        // Now we have a list of symbols, yay. Check, if this reference has a resolution
        // decider, e.g. a call expression that is calling this expression. In this case, we let it
        // decide.
        val decider = ref.resolutionDecider
        decider?.let {
            return decider.decide(symbols, ref, tu)
        }

        // Infer fields from member expressions
        // TODO: convert to new inference system
        /*if (symbols.isEmpty() && ref is MemberExpression) {
            val declaration = inference?.inferFieldDeclaration(ref, tu)
            if (declaration != null) {
                symbols = listOf(declaration)
            }
        }*/

        // Take the first one (the closest in the scope)
        return symbols.firstOrNull()
    }

    private fun resolveDeclaredReferenceExpression(
        ref: DeclaredReferenceExpression,
    ): List<Declaration> {
        // By default, we start our symbol search from the scope of the reference. This might be
        // adjusted by different factors, such as qualified names or through member expressions.
        val searchScope = ref.scope

        val name = ref.name

        // Look up the symbol by name
        return if (name.isQualified()) {
            sm.lookupQualified(name)
        } else {
            sm.lookupUnqualified(searchScope, ref.name)
        }
    }

    private fun resolveMemberExpression(
        ref: MemberExpression,
        tu: TranslationUnitDeclaration
    ): List<Declaration> {
        // We are interested in the type of our base, because this determines the (record) scope
        // that we need to start our symbol search from.
        val baseType = ref.base?.type

        // Our base type needs to an object type, otherwise we cannot proceed
        // TODO: actually it probably can also be some weird function pointer stuff, but
        //  lets deal with that later
        if (baseType !is ObjectType) {
            Util.errorWithFileLocation(
                ref,
                log,
                "Type of member expression base is not an object type. Cannot resolve member ${ref.name}"
            )

            return listOf()
        }

        // Also, we need our type to be resolved, otherwise, there is no point in continuing. At
        // first, we try if we can infer the record declaration if it does not exist and if we
        // are configured to do so.
        // TODO: Lookup the scope instead of relying the recordDeclaration being here
        var record = baseType.recordDeclaration
        if (record == null) {
            record = baseType.startInference().inferRecordDeclaration(baseType, tu)
        }
        // If the record still does not exist at this pont, we cannot continue
        if (record == null) {
            Util.errorWithFileLocation(
                ref,
                log,
                "Type of member expression base was not correctly resolved to its declaration. Cannot resolve member ${ref.name}"
            )

            return listOf()
        }

        // Update the record on the member expression, so that other passes or the inference
        // system can work with this information and do not need to retrieve it again
        ref.record = record

        // Looks like we (almost) have everything in place. The last thing we need is the scope
        // of the record declaration.
        val scope = sm.lookupScope(record)
        if (scope == null) {
            Util.errorWithFileLocation(
                ref,
                log,
                "Scope of record declaration of member expression base is empty. Cannot resolve member ${ref.name}"
            )

            return listOf()
        }

        // Build a qualified name according to our scope
        val qualifiedName = scope.name.fqn(ref.name.localName)

        // Build super class scope names
        // TODO: need to do that recursive
        val supers = mutableListOf<Name>()
        for (superRecord in record.superTypeDeclarations) {
            sm.lookupScope(superRecord)?.let {
                supers.add(it.name.fqn(ref.name.localName))
            }
        }

        // Lookup the name using the qualified name as well as its super class names
        return sm.lookupQualified(listOf(qualifiedName, *supers.toTypedArray()))
    }

    override fun cleanup() {}
}

/**
 * A [ResolutionDecision] that decides resolution based on the call expression. It decides on the
 * appropriate [Declaration] and sets the [CallExpression.invokesEdges] property.
 */
object CallResolution : ResolutionDecision<CallExpression, DeclaredReferenceExpression> {

    var log: Logger = LoggerFactory.getLogger(CallResolution::class.java)

    override fun decide(
        benefactor: CallExpression,
        source: DeclaredReferenceExpression,
        symbols: List<Declaration>,
        tu: TranslationUnitDeclaration
    ): Declaration? {
        // First, we try to match based on the signature
        var candidates =
            symbols.filterIsInstance<FunctionDeclaration>().filter {
                it.hasSignature(benefactor.signature)
            }

        if (candidates.size > 1) {
            log.warn(
                "One than more candidate found, this might be a problem. We are using the first one"
            )
        }

        // Invoke our inference system
        if (candidates.isEmpty()) {
            val func = tu.inferFunction(benefactor)
            candidates = listOf(func)
        }

        // Set the invoke edge
        benefactor.invokes = candidates

        return candidates.firstOrNull()
    }
}

interface ResolutionDecision<T : Node, S : Node> {
    fun decide(
        benefactor: T,
        source: S,
        symbols: List<Declaration>,
        tu: TranslationUnitDeclaration
    ): Declaration?
}

/**
 * Adds implicit duplicates of the TemplateParams to the implicit ConstructExpression
 *
 * @param templateParams of the VariableDeclaration/NewExpression
 * @param constructExpression duplicate TemplateParameters (implicit) to preserve AST, as
 * ConstructExpression uses AST as well as the VariableDeclaration/NewExpression
 */
fun addImplicitTemplateParametersToCall(
    templateParams: List<Node?>?,
    constructExpression: ConstructExpression?
) {
    if (templateParams != null) {
        for (node in templateParams) {
            if (node is TypeExpression) {
                constructExpression?.addTemplateParameter(node.duplicate(true))
            } else if (node is Literal<*>) {
                constructExpression?.addTemplateParameter(node.duplicate(true))
            }
        }
    }
}
