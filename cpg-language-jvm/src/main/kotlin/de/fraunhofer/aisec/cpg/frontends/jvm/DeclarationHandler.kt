/*
 * Copyright (c) 2024, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.frontends.jvm

import de.fraunhofer.aisec.cpg.frontends.DeclarationContext
import de.fraunhofer.aisec.cpg.frontends.Handler
import de.fraunhofer.aisec.cpg.frontends.HasKeywordSemantics
import de.fraunhofer.aisec.cpg.frontends.KeywordSemantics
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.graph.scopes.RecordScope
import sootup.core.jimple.basic.Local
import sootup.core.model.SootClass
import sootup.core.model.SootField
import sootup.core.model.SootMethod

class DeclarationHandler(frontend: JVMLanguageFrontend) :
    Handler<Declaration, Any, JVMLanguageFrontend>(::ProblemDeclaration, frontend) {

    override fun handle(ctx: Any): Declaration {
        try {
            return when (ctx) {
                is SootClass -> handleClass(ctx)
                is SootMethod -> handleMethod(ctx)
                is SootField -> handleField(ctx)
                is Local -> handleLocal(ctx)
                else -> {
                    log.warn("Unhandled declaration type: ${ctx.javaClass.simpleName}")
                    newProblemDeclaration(
                        "Unhandled declaration type: ${ctx.javaClass.simpleName}",
                        rawNode = ctx,
                    )
                }
            }
        } catch (e: Exception) {
            log.error("Error while handling a declaration", e)
            return newProblemDeclaration(
                "Error handling declaration ${ctx}: ${e.message}",
                rawNode = ctx,
            )
        }
    }

    private fun handleClass(sootClass: SootClass): Record {
        val record =
            newRecord(
                sootClass.getName(),
                if (sootClass.isInterface()) {
                    "interface"
                } else {
                    "class"
                },
                rawNode = sootClass,
            )

        // Map the class' access flags onto the canonical visibility model. The class is not yet on
        // the scope stack, so the current scope reflects its enclosing (declaration) context.
        record.applyAccessFlags(sootClass.modifiers.lowercasedNames())

        // Collect super class
        val o = sootClass.superclass
        if (o.isPresent) {
            record.addSuperClass(frontend.typeOf(o.get()))
        }

        // Collect implemented interfaces
        for (i in sootClass.interfaces) {
            record.implementedInterfaces += frontend.typeOf(i)
        }

        // Enter the class scope
        frontend.scopeManager.enterScope(record)

        // Loop through all fields
        for (sootField in sootClass.fields) {
            val field = handle(sootField) as? Field
            if (field != null) {
                frontend.scopeManager.addDeclaration(field)
                record.addDeclaration(field)
            }
        }

        // Loop through all methods
        for (sootMethod in sootClass.methods) {
            val method = handle(sootMethod) as? Method
            if (method != null) {
                frontend.scopeManager.addDeclaration(method)
                record.addDeclaration(method)
            }
        }

        // Leave the class scope
        frontend.scopeManager.leaveScope(record)

        return record
    }

    private fun handleMethod(sootMethod: SootMethod): Method {
        val record = frontend.scopeManager.currentRecord

        val method =
            if (sootMethod.name == "<init>") {
                newConstructor(sootMethod.name, record, rawNode = sootMethod)
            } else {
                newMethod(
                    sootMethod.name,
                    sootMethod.isStatic,
                    frontend.scopeManager.currentRecord,
                    rawNode = sootMethod,
                )
            }

        // Map the method's access flags onto the canonical visibility model.
        method.applyAccessFlags(sootMethod.modifiers.lowercasedNames())

        // Enter method scope
        frontend.scopeManager.enterScope(method)

        // Add "@this" as the receiver
        val receiver =
            newVariable("@this", method.recordDeclaration?.toType() ?: unknownType())
                .implicit("@this")
        frontend.scopeManager.addDeclaration(receiver)
        method.receiver = receiver

        // Add method parameters
        for ((index, type) in sootMethod.parameterTypes.withIndex()) {
            val param = newParameter("@parameter${index}", frontend.typeOf(type))
            frontend.scopeManager.addDeclaration(param)
            method.parameters += param
        }

        // Parse body if doNotParseBody returns false
        if (!frontend.frontendConfiguration.doNotParseBody(method) && sootMethod.isConcrete) {
            // Handle method body
            method.body = frontend.statementHandler.handle(sootMethod.body)
        }

        // Leave method scope
        frontend.scopeManager.leaveScope(method)

        return method
    }

    fun handleField(field: SootField): Field {
        val declaration = newField(field.name, frontend.typeOf(field.type), rawNode = field)

        // Map the field's access flags onto the canonical visibility model. A field is always a
        // record member.
        declaration.applyAccessFlags(field.modifiers.lowercasedNames())

        return declaration
    }

    private fun handleLocal(local: Local): Variable {
        return newVariable(local.name, frontend.typeOf(local.type), rawNode = local)
    }

    /**
     * The [DeclarationContext] the frontend is currently building a declaration in, derived from
     * the active scope. Since JVM access flags are context-independent, this only distinguishes
     * record members from top-level declarations for completeness and future use.
     */
    private val currentDeclarationContext: DeclarationContext
        get() =
            when (frontend.scopeManager.currentScope) {
                is RecordScope -> DeclarationContext.RECORD
                else -> DeclarationContext.GLOBAL
            }

    /**
     * Projects the JVM bytecode access flags of a declaration onto the canonical visibility model.
     *
     * The [rawModifiers] are the lower-cased access-flag spellings, including ones without a
     * canonical meaning (such as `final` or `volatile`); they are kept losslessly in
     * [Declaration.modifiers]. Each flag is additionally interpreted via the language's
     * [HasKeywordSemantics] trait and the results are merged per axis:
     * `public`/`protected`/`private` set [Declaration.visibility] while `static` sets
     * [ValueDeclaration.isStatic]. A declaration carries at most one access-control flag,
     * optionally combined with `static`, so folding the flags together is unambiguous. Flags this
     * language does not model yield empty [KeywordSemantics] and are ignored here. The *absence* of
     * `public`/`protected`/`private` denotes Java's package-private default and therefore maps to
     * [Visibility.PACKAGE].
     *
     * Limitation for nested classes: the JVM stores a nested class' real accessibility
     * (`private`/`protected`/`static`) in the *enclosing* class' `InnerClasses` attribute, not in
     * the nested class' own ClassFile `access_flags`. SootUp only surfaces the latter
     * (`AsmClassSource.resolveModifiers`), so a nested `private`/`protected` class is reported as
     * [Visibility.PACKAGE]/[Visibility.PUBLIC] and a nested `static` class is not marked static.
     * [JVMVisibilityTest] pins this (currently unavoidable) behavior. Field- and method-level flags
     * are unaffected, as member access flags live in the member itself.
     */
    private fun Declaration.applyAccessFlags(rawModifiers: Set<String>) {
        modifiers = modifiers + rawModifiers

        val language = frontend.language
        if (language !is HasKeywordSemantics) {
            return
        }

        val semantics =
            rawModifiers.fold(KeywordSemantics()) { acc, keyword ->
                acc.merge(language.interpretKeyword(keyword, currentDeclarationContext))
            }

        // No access flag at all means package-private (Visibility.PACKAGE) in the JVM.
        visibility = semantics.visibility ?: Visibility.PACKAGE

        (this as? ValueDeclaration)?.let { vd -> semantics.isStatic?.let { vd.isStatic = it } }
    }
}

/** Returns the lower-cased [Enum.name]s of these access-flag modifiers. */
private fun Iterable<Enum<*>>.lowercasedNames(): MutableSet<String> =
    mapTo(mutableSetOf()) { it.name.lowercase() }
