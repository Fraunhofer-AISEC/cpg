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
package de.fraunhofer.aisec.cpg.passes

import de.fraunhofer.aisec.cpg.TranslationContext
import de.fraunhofer.aisec.cpg.frontends.golang.GoLanguage
import de.fraunhofer.aisec.cpg.graph.Component
import de.fraunhofer.aisec.cpg.graph.allChildren
import de.fraunhofer.aisec.cpg.graph.declarations.Method
import de.fraunhofer.aisec.cpg.graph.declarations.Record
import de.fraunhofer.aisec.cpg.passes.configuration.DependsOn
import de.fraunhofer.aisec.cpg.passes.configuration.ExecuteBefore
import de.fraunhofer.aisec.cpg.passes.configuration.RequiresLanguage

/**
 * Binds Go methods to the [Record] of their receiver when the frontend could not do so.
 *
 * In Go a method is a top-level declaration carrying a receiver, and it may appear in any file of
 * the package -- in particular in a different file than the type it belongs to. The frontend can
 * only resolve the receiver's record while parsing if that record happens to have been parsed
 * already, so for a method that precedes its type the binding is simply missing. Generated code
 * makes this the common case rather than a corner case: miekg/dns declares the `Header`, `copy` and
 * `len` methods of nearly every resource record type in ztypes.go, while the types themselves live
 * in types.go.
 *
 * An unbound method is invisible to everything that reasons over a type's method set: the type's
 * method set comes out incomplete, it is not recognized as implementing the interfaces it actually
 * satisfies, and every call dispatched through those interfaces loses the corresponding target.
 *
 * Once the whole component has been parsed the order no longer matters, so the remaining methods
 * can be bound by name here. This has to happen before [GoInterfaceSatisfactionPass] computes
 * method sets and before [TypeHierarchyResolver] derives the hierarchy from them.
 */
@RequiresLanguage(GoLanguage::class)
@DependsOn(GoExtraPass::class)
@ExecuteBefore(GoInterfaceSatisfactionPass::class)
@ExecuteBefore(TypeHierarchyResolver::class)
@Description(
    "Binds Go methods whose receiver type was not yet parsed to the record of their receiver, so that method sets are complete regardless of the order in which the files of a package were parsed."
)
class GoMethodBindingPass(ctx: TranslationContext) : ComponentPass(ctx) {

    override fun accept(component: Component) {
        val records = component.allChildren<Record>()
        if (records.isEmpty()) {
            return
        }

        // A Go method's name is built as `Name(funcName, fqnRecord)`, so its parent is the fully
        // qualified name of the receiver type. Prefer that over the local name, which would merge
        // same-named types from different packages -- `Header` exists in several.
        val byFqn = mutableMapOf<String, Record>()
        for (record in records) {
            byFqn.putIfAbsent(record.name.toString(), record)
        }

        var bound = 0
        for (method in component.allChildren<Method>()) {
            if (method.recordDeclaration != null) {
                continue
            }
            val owner = method.name.parent?.toString() ?: continue
            val record = byFqn[owner] ?: continue

            method.recordDeclaration = record
            // Setting the record is not enough. TypeHierarchyResolver.analyzeOverridingMethods
            // looks the override candidates up in the *scope* of the record, which is what the
            // frontend populates when it can bind the method itself. Without registering the
            // symbol here the method set stays invisible to the hierarchy and `overriddenBy`
            // remains empty, so the call through the interface is still not devirtualized.
            scopeManager.enterScope(record)
            scopeManager.addDeclaration(method)
            scopeManager.leaveScope(record)
            bound++
        }

        if (bound > 0) {
            log.info(
                "Go method binding: bound {} method(s) to their receiver's record that the frontend could not resolve while parsing",
                bound,
            )
        }
    }

    override fun cleanup() {
        // nothing to do
    }
}
