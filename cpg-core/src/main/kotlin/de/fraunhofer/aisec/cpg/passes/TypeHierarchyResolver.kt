/*
 * Copyright (c) 2019, Fraunhofer AISEC. All rights reserved.
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
import de.fraunhofer.aisec.cpg.graph.AstNode
import de.fraunhofer.aisec.cpg.graph.Component
import de.fraunhofer.aisec.cpg.graph.Name
import de.fraunhofer.aisec.cpg.graph.declarations.EnumDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.Method
import de.fraunhofer.aisec.cpg.graph.declarations.Record
import de.fraunhofer.aisec.cpg.graph.types.ObjectType
import de.fraunhofer.aisec.cpg.passes.configuration.DependsOn
import de.fraunhofer.aisec.cpg.processing.IVisitor
import de.fraunhofer.aisec.cpg.processing.strategy.Strategy

/**
 * Transitively connect [Record] nodes with their supertypes' records.
 *
 * Supertypes are all interfaces a class implements and the superclass it inherits from (including
 * all of their respective supertypes). The JavaParser provides us with initial info about direct
 * ancestors' names. This pass then recursively maps those and their own supertypes to the correct
 * [Record] (if available).
 *
 * After determining the ancestors of a class, all inherited methods are scanned to find out which
 * of them are overridden/implemented in the current class. See
 * [FunctionDeclaration.getOverriddenBy]
 *
 * **Attention:** Needs to be run before other analysis passes, as it triggers a type refresh. This
 * is needed e.g. for [de.fraunhofer.aisec.cpg.graph.TypeManager.getCommonType] to be re-evaluated
 * at places where it is crucial to have parsed all [Record]s. Otherwise, type information in the
 * graph might not be fully correct
 */
@DependsOn(TypeResolver::class)
@Description(
    "Builds the type hierarchy within the CPG, establishing relationships between types such as inheritance and interface implementation."
)
open class TypeHierarchyResolver(ctx: TranslationContext) : ComponentPass(ctx) {
    protected val recordMap = mutableMapOf<Name, Record>()
    protected val enums = mutableListOf<EnumDeclaration>()

    override fun accept(component: Component) {
        for (tu in component.translationUnits) {
            findRecordsAndEnums(tu)
        }
        for (recordDecl in recordMap.values) {
            val supertypeRecords = findSupertypeRecords(recordDecl)
            val allMethodsFromSupertypes = getAllMethodsFromSupertypes(supertypeRecords)
            analyzeOverridingMethods(recordDecl, allMethodsFromSupertypes)
        }
        for (enumDecl in enums) {
            val directSupertypeRecords =
                enumDecl.superTypes.mapNotNull { (it as? ObjectType)?.recordDeclaration }.toSet()
            val allSupertypes =
                directSupertypeRecords.map { findSupertypeRecords(it) }.flatten().toSet()
            enumDecl.superTypeDeclarations = allSupertypes
        }
    }

    protected fun findRecordsAndEnums(node: AstNode) {
        // Using a visitor to avoid loops in the AST
        node.accept(
            Strategy::AST_FORWARD,
            object : IVisitor<AstNode>() {
                override fun visit(t: AstNode) {
                    if (t is EnumDeclaration) {
                        enums.add(t)
                    } else if (t is Record) {
                        recordMap.putIfAbsent(t.name, t)
                    }
                }
            },
        )
    }

    protected fun getAllMethodsFromSupertypes(supertypeRecords: Set<Record>): List<Method> {
        return supertypeRecords.map { it.methods }.flatten()
    }

    protected fun findSupertypeRecords(recordDeclaration: Record): Set<Record> {
        val superTypeDeclarations =
            recordDeclaration.superTypes
                .mapNotNull { (it as? ObjectType)?.recordDeclaration }
                .toSet()
        recordDeclaration.superTypeDeclarations = superTypeDeclarations

        // This will make sure that the type is correctly registered with the type system and that
        // all the super types are set correctly
        recordDeclaration.toType()

        return superTypeDeclarations
    }

    protected fun analyzeOverridingMethods(
        declaration: Record,
        allMethodsFromSupertypes: List<Method>,
    ) {
        for (superMethod in allMethodsFromSupertypes) {
            val overrideCandidates =
                declaration.methods.filter { superMethod.isOverrideCandidate(it) }
            superMethod.addOverriddenBy(overrideCandidates)
            overrideCandidates.forEach { it.addOverrides(superMethod) }
        }
    }

    override fun cleanup() {
        // nothing to do
    }
}
