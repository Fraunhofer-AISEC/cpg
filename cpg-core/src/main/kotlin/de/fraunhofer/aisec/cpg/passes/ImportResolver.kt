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
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.graph.types.UnknownType
import de.fraunhofer.aisec.cpg.passes.order.DependsOn
import de.fraunhofer.aisec.cpg.processing.IVisitor
import de.fraunhofer.aisec.cpg.processing.strategy.Strategy
import java.util.*
import java.util.regex.Pattern

@DependsOn(TypeHierarchyResolver::class)
open class ImportResolver(ctx: TranslationContext) : ComponentPass(ctx) {
    protected val records: MutableList<RecordDecl> = ArrayList()
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

    protected fun getStaticImports(recordDecl: RecordDecl): MutableSet<ValueDecl> {
        val partitioned =
            recordDecl.staticImportStatements.groupBy { it.endsWith("*") }.toMutableMap()

        val staticImports = mutableSetOf<ValueDecl>()
        val importPattern = Pattern.compile("(?<base>.*)\\.(?<member>.*)")

        for (specificStaticImport in partitioned[false] ?: listOf()) {
            val matcher = importPattern.matcher(specificStaticImport)
            if (!matcher.matches()) {
                continue
            }
            val base = importables[matcher.group("base")]
            var members = setOf<ValueDecl>()
            if (base is RecordDecl) {
                members = getOrCreateMembers(base, matcher.group("member"))
            } else if (base is EnumDecl) {
                members = getOrCreateMembers(base, matcher.group("member"))
            }
            staticImports.addAll(members)
        }

        for (asteriskImport in partitioned[true] ?: listOf()) {
            val base = importables[asteriskImport.replace(".*", "")]
            if (base is RecordDecl) {
                val classes = listOf(base, *base.superTypeDeclarations.toTypedArray())
                // Add all the static methods implemented in the class "base" and its superclasses
                staticImports.addAll(classes.flatMap { it.methods }.filter(MethodDecl::isStatic))
                // Add all the static fields implemented in the class "base" and its superclasses
                staticImports.addAll(
                    classes.flatMap { it.fields }.filter { "static" in it.modifiers }
                )
            } else if (base is EnumDecl) {
                staticImports.addAll(base.entries)
            }
        }
        return staticImports
    }

    protected fun getDeclarationsForTypeNames(targetTypes: List<String>): MutableSet<Declaration> {
        return targetTypes.mapNotNull { importables[it] }.toMutableSet()
    }

    protected fun getOrCreateMembers(base: EnumDecl, name: String): Set<ValueDecl> {
        return base.entries.filter { it.name.localName == name }.toSet()
    }

    protected fun getOrCreateMembers(base: RecordDecl, name: String): Set<ValueDecl> {
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

        // now it gets weird: you can import a field and a number of methods that have the same
        // name, all with a *single* static import...
        val result = mutableSetOf<ValueDecl>()
        result.addAll(memberMethods)
        result.addAll(memberFields)
        if (result.isEmpty()) {
            // the target might be a field or a method, we don't know. Thus, we need to create both
            val targetField =
                base.newFieldDecl(
                    name,
                    UnknownType.getUnknownType(base.language),
                    ArrayList(),
                    "",
                    null,
                    null,
                    false,
                    base.language
                )
            targetField.isInferred = true
            val targetMethod = base.newMethodDecl(name, "", true, base)
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
                    if (t is RecordDecl) {
                        records.add(t)
                        importables.putIfAbsent(t.name.toString(), t)
                    } else if (t is EnumDecl) {
                        importables.putIfAbsent(t.name.toString(), t)
                    }
                }
            }
        )
    }
}
