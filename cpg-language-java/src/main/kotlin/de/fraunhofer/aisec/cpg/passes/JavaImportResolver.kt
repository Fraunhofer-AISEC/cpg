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
import de.fraunhofer.aisec.cpg.frontends.java.JavaLanguageFrontend
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.graph.types.UnknownType
import de.fraunhofer.aisec.cpg.passes.configuration.DependsOn
import de.fraunhofer.aisec.cpg.passes.configuration.RequiredFrontend
import de.fraunhofer.aisec.cpg.processing.IVisitor
import de.fraunhofer.aisec.cpg.processing.strategy.Strategy
import java.util.*
import java.util.regex.Pattern

/**
 * Some piece of legacy code that deals with Java imports. We need to convert this to the new import
 * system.
 *
 * We need to remove this class and use [ImportResolver] and [ImportDeclaration] instead.
 */
@DependsOn(TypeHierarchyResolver::class)
@RequiredFrontend(JavaLanguageFrontend::class)
open class JavaImportResolver(ctx: TranslationContext) : ComponentPass(ctx) {
    protected val records: MutableList<RecordDeclaration> = ArrayList()
    protected val importables: MutableMap<String, Declaration> = HashMap()

    override fun cleanup() {
        records.clear()
        importables.clear()
    }

    override fun accept(component: Component) {
        for (tu in component.translationUnits) {
            findImportables(tu)
        }
        for (recordDecl in records) {
            val imports = getDeclarationsForTypeNames(recordDecl.importStatements)
            recordDecl.imports = imports
            val staticImports = getStaticImports(recordDecl)
            recordDecl.staticImports = staticImports
        }
    }

    protected fun getStaticImports(
        recordDeclaration: RecordDeclaration
    ): MutableSet<ValueDeclaration> {
        val partitioned =
            recordDeclaration.staticImportStatements.groupBy { it.endsWith("*") }.toMutableMap()

        val staticImports = mutableSetOf<ValueDeclaration>()
        val importPattern = Pattern.compile("(?<base>.*)\\.(?<member>.*)")

        for (specificStaticImport in partitioned[false] ?: listOf()) {
            val matcher = importPattern.matcher(specificStaticImport)
            if (!matcher.matches()) {
                continue
            }
            val base = importables[matcher.group("base")]
            var members = setOf<ValueDeclaration>()
            if (base is RecordDeclaration) {
                members = getOrCreateMembers(base, matcher.group("member"))
            }
            staticImports.addAll(members)
        }

        for (asteriskImport in partitioned[true] ?: listOf()) {
            val base = importables[asteriskImport.replace(".*", "")]
            if (base is EnumDeclaration) {
                staticImports.addAll(base.entries)
            } else if (base is RecordDeclaration) {
                val classes = listOf(base, *base.superTypeDeclarations.toTypedArray())
                // Add all the static methods implemented in the class "base" and its superclasses
                staticImports.addAll(
                    classes.flatMap { it.methods }.filter(MethodDeclaration::isStatic)
                )
                // Add all the static fields implemented in the class "base" and its superclasses
                staticImports.addAll(
                    classes.flatMap { it.fields }.filter { "static" in it.modifiers }
                )
            }
        }
        return staticImports
    }

    protected fun getDeclarationsForTypeNames(targetTypes: List<String>): MutableSet<Declaration> {
        return targetTypes.mapNotNull { importables[it] }.toMutableSet()
    }

    protected fun getOrCreateMembers(base: RecordDeclaration, name: String): Set<ValueDeclaration> {
        val memberMethods = base.methods.filter { it.name.localName.endsWith(name) }.toMutableSet()

        // add methods from superclasses
        memberMethods.addAll(
            base.superTypeDeclarations
                .flatMap { it.methods }
                .filter { it.name.localName.endsWith(name) }
        )
        val memberFields = base.fields.filter { it.name.localName == name }.toMutableSet()
        // add fields from superclasses
        memberFields.addAll(
            base.superTypeDeclarations.flatMap { it.fields }.filter { it.name.localName == name }
        )

        val memberEntries = mutableSetOf<EnumConstantDeclaration>()
        if (base is EnumDeclaration) {
            base.entries[name]?.let { memberEntries.add(it) }
        }

        // now it gets weird: you can import a field and a number of methods that have the same
        // name, all with a *single* static import...
        // TODO(oxisto): Move all of the following code to the [Inference] class
        val result = mutableSetOf<ValueDeclaration>()
        result.addAll(memberMethods)
        result.addAll(memberFields)
        result.addAll(memberEntries)
        if (result.isEmpty()) {
            // the target might be a field or a method, we don't know. Thus, we need to create both
            val targetField =
                newFieldDeclaration(
                    name,
                    UnknownType.getUnknownType(base.language),
                    ArrayList(),
                    null,
                    false,
                )
            targetField.language = base.language
            targetField.isInferred = true

            val targetMethod = newMethodDeclaration(name, true, base)
            targetMethod.language = base.language
            targetMethod.isInferred = true

            base.addField(targetField)
            base.addMethod(targetMethod)
            result.add(targetField)
            result.add(targetMethod)
        }
        return result
    }

    protected fun findImportables(node: Node) {
        // Using a visitor to avoid loops in the AST
        node.accept(
            Strategy::AST_FORWARD,
            object : IVisitor<Node>() {
                override fun visit(t: Node) {
                    if (t is RecordDeclaration) {
                        records.add(t)
                        importables.putIfAbsent(t.name.toString(), t)
                    }
                }
            },
        )
    }
}
