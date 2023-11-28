/*
 * Copyright (c) 2023, Fraunhofer AISEC. All rights reserved.
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
import de.fraunhofer.aisec.cpg.frontends.golang.*
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.graph.statements.DeclarationStatement
import de.fraunhofer.aisec.cpg.graph.statements.ForEachStatement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.graph.types.*
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker
import de.fraunhofer.aisec.cpg.passes.inference.startInference
import de.fraunhofer.aisec.cpg.passes.order.ExecuteBefore

/**
 * This pass takes care of several things that we need to clean up, once all translation units are
 * successfully parsed, but before any of the remaining CPG passes, such as call resolving occurs.
 *
 * ## Add Type Listeners for Key/Value Variables in For-Each Statements
 *
 * In Go, a common idiom is to use a short assignment in a for-each statement to declare a key and
 * value object without explicitly specifying the type.
 *
 * ```go
 * var bytes = []byte{1,2,3,4}
 * for key, value := range bytes {
 *   // key is of type int; value of type byte
 *   fmt.Printf("bytes[%d]=%d\n", key, value)
 * }
 * ```
 *
 * The key variable is always of type `int`, whereas the value variable depends on the iterated
 * expression. Therefore, we set up type listeners based on the iterated object.
 *
 * ## Infer NamespaceDeclarations for Import Packages
 *
 * We want to infer namespace declarations for import packages, that are unknown to us. This allows
 * us to then infer functions in those packages as well.
 *
 * ## Declare Variables in Short Assignments
 *
 * We want to implicitly declare variables in a short assignment. We cannot do this in the frontend
 * itself, because some of the variables in the assignment might already exist, and those are not
 * declared, but just assigned. Only the non-defined variables are declared by the short assignment.
 *
 * The following short assignment (in the second line) only declares the variable `b` but assigns
 * `1` to the already existing variable `a` and `2` to the new variable `b`.
 *
 * ```go
 * var a int
 * a, b := 1, 2
 * ```
 *
 * In the frontend we only do the assignment, therefore we need to create a new
 * [VariableDeclaration] for `b` and inject a [DeclarationStatement].
 *
 * ## Converting Call Expressions into Cast Expressions
 *
 * In Go, it is possible to convert compatible types by "calling" the type name as a function, such
 * as
 *
 * ```go
 * var i = int(2.0)
 * ```
 *
 * This is also possible with more complex types, such as interfaces or aliased types, as long as
 * they are compatible. Because types in the same package can be defined in multiple files, we
 * cannot decide during the frontend run. Therefore, we need to execute this pass before the
 * [SymbolResolver] and convert certain [CallExpression] nodes into a [CastExpression].
 *
 * ## Adjust Names of Keys in Key Value Expressions to FQN
 *
 * This pass also adjusts the names of keys in a [KeyValueExpression], which is part of an
 * [InitializerListExpression] to a fully-qualified name that contains the name of the [ObjectType]
 * that the expression is creating. This way we can resolve the static references to the field to
 * the actual field.
 *
 * ## Add Methods of Embedded Structs to the Record's Scope
 *
 * This pass also adds methods of [RecordDeclaration.embeddedStructs] into the scope of the
 * [RecordDeclaration] itself, so that it can be resolved using the regular [SymbolResolver].
 */
@ExecuteBefore(SymbolResolver::class)
@ExecuteBefore(EvaluationOrderGraphPass::class)
@ExecuteBefore(DFGPass::class)
class GoExtraPass(ctx: TranslationContext) : ComponentPass(ctx) {

    override fun accept(component: Component) {
        // Add built-int functions, but only if one of the components contains a GoLanguage
        if (component.translationUnits.any { it.language is GoLanguage }) {
            component.translationUnits += addBuiltIn()
        }

        val walker = SubgraphWalker.ScopedWalker(scopeManager)
        walker.registerHandler { _, parent, node ->
            when (node) {
                is CallExpression -> handleCall(node, parent)
                is IncludeDeclaration -> handleInclude(node)
                is RecordDeclaration -> handleRecordDeclaration(node)
                is AssignExpression -> handleAssign(node)
                is ForEachStatement -> handleForEachStatement(node)
                is InitializerListExpression -> handleInitializerListExpression(node)
            }
        }

        for (tu in component.translationUnits) {
            walker.iterate(tu)
        }
    }

    /**
     * This function adds methods of [RecordDeclaration.embeddedStructs] into the scope of the
     * struct itself, so we can resolve method calls of embedded structs.
     *
     * For example, if a struct embeds another struct (see https://go.dev/ref/spec#Struct_types), we
     * can call any methods of the embedded struct on the one that embeds it:
     * ```go
     * type MyTime struct {
     *   time.Time
     * }
     *
     * func main() {
     *   var t = MyTime{Time: time.Now()}
     *   t.Add(-5*time.Second)
     * }
     * ```
     */
    private fun handleRecordDeclaration(record: RecordDeclaration) {
        // We are only interest in structs, not interfaces
        if (record.kind != "struct") {
            return
        }

        // Enter our record's scope
        scopeManager.enterScope(record)

        // Loop through the embedded struct and add their methods to the record's scope.
        for (method in record.embeddedStructs.flatMap { it.methods }) {
            // Add it to the scope, but do NOT add it to the underlying AST field (methods),
            // otherwise we would duplicate the method in the AST
            scopeManager.addDeclaration(method, addToAST = false)
        }

        scopeManager.leaveScope(record)
    }

    private fun addBuiltIn(): TranslationUnitDeclaration {
        val builtin = newTranslationUnitDeclaration("builtin.go")
        builtin.language = GoLanguage()
        scopeManager.resetToGlobal(builtin)

        return with(builtin) {
            val len = newFunctionDeclaration("len", localNameOnly = true)
            len.parameters = listOf(newParameterDeclaration("v", autoType()))
            len.returnTypes = listOf(primitiveType("int"))
            addBuiltInFunction(len)

            /**
             * ```go
             * func append(slice []Type, elems ...Type) []Type
             * ```
             */
            val append = newFunctionDeclaration("append", localNameOnly = true)
            append.parameters =
                listOf(
                    newParameterDeclaration("slice", autoType().array()),
                    newParameterDeclaration("elems", autoType(), variadic = true),
                )
            append.returnTypes = listOf(autoType().array())
            addBuiltInFunction(append)

            /**
             * ```go
             * func panic(v any)
             * ```
             */
            val panic = newFunctionDeclaration("panic", localNameOnly = true)
            panic.parameters = listOf(newParameterDeclaration("v", primitiveType("any")))
            addBuiltInFunction(panic)

            /**
             * ```go
             * func recover() any
             * ```
             */
            val recover = newFunctionDeclaration("panic", localNameOnly = true)
            panic.returnTypes = listOf(primitiveType("any"))
            addBuiltInFunction(recover)

            val error = newRecordDeclaration("error", "interface")
            scopeManager.enterScope(error)

            val errorFunc = newMethodDeclaration("Error", recordDeclaration = error)
            errorFunc.returnTypes = listOf(primitiveType("string"))
            addBuiltInFunction(errorFunc)

            scopeManager.leaveScope(error)
            builtin
        }
    }

    private fun addBuiltInFunction(func: FunctionDeclaration) {
        func.type =
            typeManager.registerType(
                FunctionType(
                    funcTypeName(func.signatureTypes, func.returnTypes),
                    func.signatureTypes,
                    func.returnTypes
                )
            )
        scopeManager.addDeclaration(func)
    }

    /**
     * handleInitializerListExpression changes the references of keys in a [KeyValueExpression] to
     * include the object it is creating as a parent name.
     */
    private fun handleInitializerListExpression(node: InitializerListExpression) {
        var type: Type? = node.type

        // If our type is an "overlay", we need to look for the underlying type
        type =
            if (type.isOverlay) {
                type.underlyingType
            } else {
                type
            }

        // The type of a "inner" composite literal can be omitted if the outer one is creating
        // an array type. In this case, we need to set the type manually because the type for
        // the "inner" one is empty.
        // Example code:
        // ```go
        // var a = []*MyObject{
        //   {
        //      Name: "a",
        //   },
        //   {
        //      Name: "b",
        //   }
        // }
        if (type is PointerType && type.isArray) {
            for (init in node.initializers) {
                if (init is InitializerListExpression) {
                    init.type = type.elementType
                } else if (init is KeyValueExpression && init.value is InitializerListExpression) {
                    init.value?.type = type.elementType
                }
            }
        }

        // We are not interested in arrays and maps, but only the "inner" single-object expressions
        if (
            type is UnknownType ||
                (type is PointerType && type.isArray) ||
                node.type.name.localName == "map"
        ) {
            return
        }

        for (keyValue in node.initializers.filterIsInstance<KeyValueExpression>()) {
            val key = keyValue.key
            if (key is Reference) {
                key.name = Name(key.name.localName, node.type.root.name)
                key.isStaticAccess = true
            }
        }
    }

    /**
     * handleForEachStatement adds a [HasType.TypeObserver] to the [ForEachStatement.iterable] of an
     * [ForEachStatement] in order to determine the types used in [ForEachStatement.variable] (index
     * and iterated value).
     */
    private fun handleForEachStatement(forEach: ForEachStatement) {
        (forEach.iterable as HasType).registerTypeObserver(
            object : HasType.TypeObserver {
                override fun typeChanged(newType: Type, src: HasType) {
                    if (src.type is UnknownType) {
                        return
                    }

                    val variable = forEach.variable
                    if (variable is DeclarationStatement) {
                        // The key is the first variable. It is always an int
                        val keyVariable =
                            variable.declarations.firstOrNull() as? VariableDeclaration
                        keyVariable?.type = forEach.primitiveType("int")

                        // The value is the second one. Its type depends on the array type
                        val valueVariable =
                            variable.declarations.getOrNull(1) as? VariableDeclaration
                        ((forEach.iterable as? HasType)?.type as? PointerType)?.let {
                            valueVariable?.type = it.elementType
                        }
                    }
                }

                override fun assignedTypeChanged(assignedTypes: Set<Type>, src: HasType) {
                    // Nothing to do
                }
            }
        )
    }

    /**
     * This function gets called for every [AssignExpression], to check, whether we need to
     * implicitly define any variables assigned in the statement.
     */
    private fun handleAssign(assign: AssignExpression) {
        // Only filter nodes that could potentially declare
        if (assign.operatorCode != ":=") {
            return
        }

        // Loop through the target variables (left-hand side)
        for ((idx, expr) in assign.lhs.withIndex()) {
            if (expr is Reference) {
                // And try to resolve it
                val ref = scopeManager.resolveReference(expr)
                if (ref == null) {
                    // We need to implicitly declare it, if it's not declared before.
                    val decl = newVariableDeclaration(expr.name, expr.autoType())
                    decl.location = expr.location
                    decl.isImplicit = true

                    // We cannot assign an initializer here because this will lead to duplicate
                    // DFG edges, but we need to propagate the type information
                    if (assign.rhs.size < assign.lhs.size && assign.rhs.size == 1) {
                        assign.rhs[0].registerTypeObserver(InitializerTypePropagation(decl, idx))
                    } else {
                        assign.rhs[idx].registerTypeObserver(InitializerTypePropagation(decl))
                    }

                    assign.declarations += decl

                    // Add it to the scope, so other assignments / references can "see" it.
                    scopeManager.addDeclaration(decl)
                }
            }
        }
    }

    /**
     * This function gets called for every [IncludeDeclaration] (which in Go imports a whole
     * package) and checks, if we need to infer a [NamespaceDeclaration] for this particular
     * include.
     */
    // TODO: Somehow, this gets called twice?!
    private fun handleInclude(include: IncludeDeclaration) {
        // If the namespace is included as _, we can ignore it, as its only included as a runtime
        // dependency
        if (include.name.localName == "_") {
            return
        }

        // Try to see if we already know about this namespace somehow
        val namespace =
            scopeManager.resolve<NamespaceDeclaration>(scopeManager.globalScope, true) {
                it.name == include.name && it.path == include.filename
            }

        // If not, we can infer a namespace declaration, so we can bundle all inferred function
        // declarations in there
        if (namespace.isEmpty()) {
            scopeManager.globalScope
                ?.astNode
                ?.startInference(ctx)
                ?.createInferredNamespaceDeclaration(include.name, include.filename)
        }
    }

    /**
     * This function gets called for every [CallExpression] and checks, whether this is actually a
     * "calling" a type and is thus a [CastExpression] rather than a [CallExpression].
     */
    private fun handleCall(call: CallExpression, parent: Node?) {
        // We need to check, whether the "callee" refers to a type and if yes, convert it into a
        // cast expression. And this is only really necessary, if the function call has a single
        // argument.
        var callee = call.callee
        if (parent != null && callee != null && call.arguments.size == 1) {
            val language = parent.language ?: GoLanguage()

            var pointer = false
            // If the argument is a UnaryOperator, unwrap them
            if (callee is UnaryOperator && callee.operatorCode == "*") {
                pointer = true
                callee = callee.input
            }

            // First, check if this is a built-in type
            if (language.builtInTypes.contains(callee.name.toString())) {
                replaceCallWithCast(callee.name.toString(), parent, call, false)
            } else {
                // If not, then this could still refer to an existing type. We need to make sure
                // that we take the current namespace into account
                val fqn =
                    if (callee.name.parent == null) {
                        scopeManager.currentNamespace.fqn(callee.name.localName)
                    } else {
                        callee.name
                    }

                if (typeManager.typeExists(fqn.toString())) {
                    replaceCallWithCast(fqn, parent, call, pointer)
                }
            }
        }
    }

    private fun replaceCallWithCast(
        typeName: CharSequence,
        parent: Node,
        call: CallExpression,
        pointer: Boolean,
    ) {
        val cast = newCastExpression()
        cast.code = call.code
        cast.language = call.language
        cast.location = call.location
        cast.castType =
            if (pointer) {
                call.objectType(typeName).pointer()
            } else {
                call.objectType(typeName)
            }
        cast.expression = call.arguments.single()

        if (parent !is ArgumentHolder) {
            log.error(
                "Parent AST node of call expression is not an argument holder. Cannot convert to cast expression. Further analysis might not be entirely accurate."
            )
            return
        }

        val success = parent.replaceArgument(call, cast)
        if (!success) {
            log.error(
                "Replacing call expression with cast expression was not successful. Further analysis might not be entirely accurate."
            )
        } else {
            call.disconnectFromGraph()
        }
    }

    override fun cleanup() {
        // Nothing to do
    }
}
