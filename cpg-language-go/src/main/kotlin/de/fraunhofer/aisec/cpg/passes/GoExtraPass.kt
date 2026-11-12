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
import de.fraunhofer.aisec.cpg.graph.declarations.Function
import de.fraunhofer.aisec.cpg.graph.expressions.Assign
import de.fraunhofer.aisec.cpg.graph.expressions.DeclarationStatement
import de.fraunhofer.aisec.cpg.graph.expressions.ForEach
import de.fraunhofer.aisec.cpg.graph.expressions.InitializerList
import de.fraunhofer.aisec.cpg.graph.expressions.KeyValue
import de.fraunhofer.aisec.cpg.graph.expressions.Reference
import de.fraunhofer.aisec.cpg.graph.scopes.Scope
import de.fraunhofer.aisec.cpg.graph.types.*
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker
import de.fraunhofer.aisec.cpg.passes.configuration.DependsOn
import de.fraunhofer.aisec.cpg.passes.configuration.ExecuteBefore
import de.fraunhofer.aisec.cpg.passes.configuration.RequiresLanguage
import de.fraunhofer.aisec.cpg.processing.strategy.Strategy

/**
 * This constant defines the name of the built-in Go translation unit. It is used to check or
 * reference the presence of the Go-specific built-in functions and definitions within the
 * translation context.
 */
private const val BUILTIN_GO = "builtin.go"

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
 * In the frontend we only do the assignment, therefore we need to create a new [Variable] for `b`
 * and inject a [DeclarationStatement].
 *
 * ## Adjust Names of Keys in Key Value Expressions to FQN
 *
 * This pass also adjusts the names of keys in a [KeyValue], which is part of an [InitializerList]
 * to a fully-qualified name that contains the name of the [ObjectType] that the expression is
 * creating. This way we can resolve the static references to the field to the actual field.
 *
 * ## Add Methods and Fields of Embedded Structs to the Record's Scope
 *
 * This pass also adds methods of [Record.embeddedStructs] into the scope of the [Record] itself, so
 * that it can be resolved using the regular [SymbolResolver].
 */
@ExecuteBefore(SymbolResolver::class)
@ExecuteBefore(EvaluationOrderGraphPass::class)
@DependsOn(ImportResolver::class)
@DependsOn(TypeResolver::class)
@RequiresLanguage(GoLanguage::class)
@Description(
    "This pass takes care of several things that we need to clean up, once all translation units are successfully parsed, but before any of the remaining CPG passes, such as call resolving occurs. Adds Type Listeners for Key/Value Variables in For-Each Statements, Infers NamespaceDeclarations for Import Packages, Declares Variables in Short Assignments, Adjust Names of Keys in Key Value Expressions to FQN, and Adds Methods and Fields of Embedded Structs to the Record's Scope."
)
class GoExtraPass(ctx: TranslationContext) : TranslationUnitPass(ctx) {

    private var walker: SubgraphWalker.ScopedWalker<AstNode> =
        SubgraphWalker.ScopedWalker(scopeManager, Strategy::AST_FORWARD)

    override val scope: Scope
        get() = scopeManager.currentScope

    init {
        walker.registerHandler { node ->
            when (node) {
                is Record -> handleRecord(node)
                is Assign -> handleAssign(node)
                is ForEach -> handleForEach(node)
                is InitializerList -> handleInitializerList(node)
            }
        }
    }

    override fun accept(tu: TranslationUnit) {
        // Add built-in translation unit, if it does not exist yet
        val component = tu.component
        if (component?.translationUnits?.none { it.name.toString() == BUILTIN_GO } == true) {
            component.translationUnits += addBuiltIn()
        }

        walker.iterate(tu)
    }

    /**
     * This function adds methods of [Record.embeddedStructs] into the scope of the struct itself,
     * so we can resolve method calls of embedded structs.
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
    private fun handleRecord(record: Record) {
        val promoted =
            when (record.kind) {
                "struct" -> promotedDeclarations(record)
                // An interface composes other interfaces by embedding them, and every method of an
                // embedded interface belongs to the embedding one's method set:
                //
                //     type ArrayEncoder interface {
                //         PrimitiveArrayEncoder
                //         AppendArray(ArrayMarshaler) error
                //     }
                //
                // A call to `AppendInt` -- declared by PrimitiveArrayEncoder -- on something typed
                // as ArrayEncoder has to find that declaration. Without it the resolver invents an
                // inferred method on ArrayEncoder instead, which has no body and no
                // implementations, so every call through the embedding interface stops there.
                "interface" -> promotedInterfaceMethods(record)
                else -> return
            }
        if (promoted.isEmpty()) {
            return
        }

        // Enter our record's scope
        scopeManager.enterScope(record)

        for (declaration in promoted) {
            // Only the scope is extended. Adding the declaration to the AST as well would
            // duplicate something that belongs to the embedded struct.
            scopeManager.addDeclaration(declaration)
        }

        scopeManager.leaveScope(record)
    }

    /**
     * Collects the methods that [record], an interface, obtains from the interfaces it embeds.
     *
     * An embedded interface is modelled as a super type of the embedding one (see
     * `SpecificationHandler.handleInterfaceTypeSpec`), and embedding is transitive. A method the
     * interface declares itself takes precedence, so names it already owns are not promoted.
     */
    private fun promotedInterfaceMethods(record: Record): List<Declaration> {
        val claimed = record.methods.mapTo(mutableSetOf()) { it.name.localName }
        val promoted = mutableListOf<Declaration>()
        val visited = mutableSetOf(record)
        val frontier = ArrayDeque(record.embeddedInterfaces.filter { visited.add(it) })

        while (frontier.isNotEmpty()) {
            val embedded = frontier.removeFirst()
            for (method in embedded.methods) {
                if (claimed.add(method.name.localName)) {
                    promoted += method
                }
            }
            frontier.addAll(embedded.embeddedInterfaces.filter { visited.add(it) })
        }

        return promoted
    }

    /** The interfaces that this record embeds, i.e. its super types that are interfaces. */
    private val Record.embeddedInterfaces: List<Record>
        get() =
            this.superTypes.mapNotNull {
                ((it as? ObjectType)?.recordDeclaration)?.takeIf { r -> r.kind == "interface" }
            }

    /**
     * Collects the fields and methods that [record] promotes from the structs it embeds, following
     * Go's selector rules (see https://go.dev/ref/spec#Selectors).
     *
     * Promotion is transitive, so a struct `A` embedding `B` which itself embeds `C` promotes `C`'s
     * members as well, at depth two. The rule for choosing between several members of the same name
     * is "shallowest wins": `x.f` denotes the `f` at the smallest depth at which one exists. If
     * more than one `f` exists at that smallest depth, the selector is illegal in Go, and a deeper
     * `f` does not rescue it -- so such a name is not promoted at all.
     *
     * We therefore walk the embedding graph breadth first, one depth at a time, and only promote a
     * name that is neither already taken at a shallower depth nor ambiguous at the current one.
     * Embedding may be cyclic through pointers (`type A struct { *A }`), so visited records are
     * tracked.
     */
    /**
     * The methods declared on this record itself. [de.fraunhofer.aisec.cpg.graph.types.ObjectType]
     * `.methods` also walks the super types, which for Go includes the interfaces the struct
     * satisfies, so it cannot be used to decide what a record genuinely owns. [Record.methods] is
     * no alternative either: a Go method is a top-level declaration carrying a receiver and is
     * therefore not an AST child of its record. We go through the type and keep only what points
     * back at this record.
     */
    private val Record.declaredMethods: List<Method>
        get() = this.toType().methods.filter { it.recordDeclaration == this }

    private fun promotedDeclarations(record: Record): List<Declaration> {
        // Depth 0: everything the record declares itself shadows anything promoted.
        val claimed = mutableSetOf<String>()
        record.fields.mapTo(claimed) { it.name.localName }
        // Only the methods this record declares itself. ObjectType.methods also returns the
        // methods of the super types, which for Go includes every interface the struct satisfies
        // -- claiming those names here would shadow the embedded struct's own members.
        record.declaredMethods.mapTo(claimed) { it.name.localName }

        val promoted = mutableListOf<Declaration>()
        val visited = mutableSetOf(record)
        var frontier = record.embeddedStructs.filterTo(mutableListOf()) { visited.add(it) }

        while (frontier.isNotEmpty()) {
            // Group everything visible at this depth by name, so that a name occurring in more
            // than one embedded struct can be recognised as ambiguous.
            val byName = mutableMapOf<String, MutableList<Declaration>>()
            for (embedded in frontier) {
                for (field in embedded.fields) {
                    byName.getOrPut(field.name.localName) { mutableListOf() } += field
                }
                for (method in embedded.declaredMethods) {
                    byName.getOrPut(method.name.localName) { mutableListOf() } += method
                }
            }

            for ((name, declarations) in byName) {
                if (name in claimed) {
                    // Shadowed by a shallower member.
                    continue
                }
                // Claim the name either way: an ambiguous name stays ambiguous, a deeper member
                // of the same name does not become selectable.
                claimed += name
                declarations.singleOrNull()?.let { promoted += it }
            }

            frontier =
                frontier
                    .flatMapTo(mutableListOf()) { it.embeddedStructs }
                    .filterTo(mutableListOf()) { visited.add(it) }
        }

        return promoted
    }

    private fun addBuiltIn(): TranslationUnit {
        val builtin = newTranslationUnit(BUILTIN_GO)
        builtin.language = GoLanguage()
        scopeManager.resetToGlobal(builtin)

        return with(builtin) {
            val len = newFunction("len", localNameOnly = true)
            len.parameters = mutableListOf(newParameter("v", autoType()))
            len.returnTypes = listOf(primitiveType("int"))
            addBuiltInFunction(len)

            /**
             * ```go
             * func append(slice []Type, elems ...Type) []Type
             * ```
             */
            val append = newFunction("append", localNameOnly = true)
            append.parameters =
                mutableListOf(
                    newParameter("slice", autoType().array()),
                    newParameter("elems", autoType(), variadic = true),
                )
            append.returnTypes = listOf(autoType().array())
            addBuiltInFunction(append)

            /**
             * ```go
             * func panic(v any)
             * ```
             */
            val panic = newFunction("panic", localNameOnly = true)
            panic.parameters = mutableListOf(newParameter("v", primitiveType("any")))
            addBuiltInFunction(panic)

            /**
             * ```go
             * func recover() any
             * ```
             */
            val recover = newFunction("panic", localNameOnly = true)
            panic.returnTypes = listOf(primitiveType("any"))
            addBuiltInFunction(recover)

            // The `error` record has to be a declaration of the builtin translation unit, not just
            // a node hanging off its own method. If it is not reachable through the AST, no pass
            // can see it -- in particular it is not recognized as an interface -- and the type
            // resolver later synthesizes a second, structurally empty `error` record of kind
            // "struct" for the `error` type that is used everywhere. Calls through `error` then
            // dispatch against that placeholder and stop at the body-less `error.Error`.
            val error = newRecord("error", "interface")
            scopeManager.addDeclaration(error)
            this.declarations += error

            scopeManager.enterScope(error)

            val errorFunc = newMethod("Error", recordDeclaration = error)
            errorFunc.returnTypes = listOf(primitiveType("string"))
            addBuiltInFunction(errorFunc)
            // `addBuiltInFunction` registers the method with the translation unit, but the record
            // also has to list it, because the structural-satisfaction check compares method sets.
            error.methods += errorFunc

            scopeManager.leaveScope(error)
            builtin
        }
    }

    private fun TranslationUnit.addBuiltInFunction(func: Function) {
        func.type =
            FunctionType(
                funcTypeName(func.signatureTypes, func.returnTypes),
                func.signatureTypes,
                func.returnTypes,
                func.language,
            )
        scopeManager.addDeclaration(func)
        this.declarations += func
    }

    /**
     * handleInitializerList changes the references of keys in a [KeyValue] to include the object it
     * is creating as a parent name.
     */
    private fun handleInitializerList(node: InitializerList) {
        var type: Type? = node.type

        // If our type is an "overlay", we need to look for the underlying type
        type =
            if (type.namedType) {
                type.underlyingType
            } else {
                type
            }

        // The type of an "inner" composite literal can be omitted if the outer one is creating
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
                when (init) {
                    is InitializerList -> {
                        init.type = type.elementType
                    }

                    is KeyValue if init.value is InitializerList -> {
                        init.value.type = type.elementType
                    }

                    is KeyValue if init.key is InitializerList -> {
                        init.key.type = type.elementType
                    }
                }
            }
        } else if (type?.isMap == true) {
            for (init in node.initializers) {
                if (init is KeyValue) {
                    if (init.key is InitializerList) {
                        init.key.type = (type as ObjectType).generics.getOrNull(0) ?: unknownType()
                    } else if (init.value is InitializerList) {
                        init.value.type =
                            (type as ObjectType).generics.getOrNull(1) ?: unknownType()
                    }
                }
            }
        }

        // Afterward, we are not interested in arrays and maps, but only the "inner" single-object
        // expressions
        if (
            type is UnknownType ||
                (type is PointerType && type.isArray) ||
                node.type.name.localName == "map"
        ) {
            return
        }

        for (keyValue in node.initializers.filterIsInstance<KeyValue>()) {
            val key = keyValue.key
            if (key is Reference) {
                key.name = Name(key.name.localName, node.type.root.name)
                key.isStaticAccess = true
            }
        }
    }

    /**
     * handleForEach adds a [HasType.TypeObserver] to the [ForEach.iterable] of an [ForEach] in
     * order to determine the types used in [ForEach.variable] (index and iterated value).
     */
    private fun handleForEach(forEach: ForEach) {
        (forEach.iterable as HasType).registerTypeObserver(
            object : HasType.TypeObserver {
                override fun typeChanged(newType: Type, src: HasType) {
                    if (src.type is UnknownType) {
                        return
                    }

                    val variable = forEach.variable
                    if (variable is DeclarationStatement) {
                        val keyVariable = variable.declarations.firstOrNull() as? Variable
                        val valueVariable = variable.declarations.getOrNull(1) as? Variable

                        when (val iterated = (forEach.iterable as? HasType)?.type?.iterated) {
                            is MapType -> {
                                // Ranging over a map yields its keys and values. Only the value
                                // type is modelled by MapType, so the key is left alone rather
                                // than being forced to int, which it only is for a slice.
                                valueVariable?.type = iterated.elementType
                            }
                            is ObjectType -> {
                                // The Go frontend does not build a MapType: a map is an object
                                // type named "map" carrying its key and value type as generics.
                                // Ranging over it yields both, and unlike a slice the key is not
                                // an int.
                                iterated.generics.getOrNull(0)?.let { keyVariable?.type = it }
                                iterated.generics.getOrNull(1)?.let { valueVariable?.type = it }
                            }
                            is PointerType -> {
                                // Slices and arrays are modelled as pointers, and are indexed by
                                // an int.
                                keyVariable?.type = forEach.primitiveType("int")
                                valueVariable?.type = iterated.elementType
                            }
                            else -> {
                                // Nothing we can say about the element type.
                            }
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
     * This function gets called for every [Assign], to check, whether we need to implicitly define
     * any variables assigned in the statement.
     */
    private fun handleAssign(assign: Assign) {
        // Only filter nodes that could potentially declare
        if (assign.operatorCode != ":=") {
            return
        }

        // Loop through the target variables (left-hand side)
        for ((idx, expr) in assign.lhs.withIndex()) {
            if (expr is Reference) {
                // And try to resolve it as a variable
                val ref = scopeManager.lookupSymbolByNodeNameOfType<Variable>(expr)
                if (ref.isEmpty()) {
                    // We need to implicitly declare it, if it's not declared before.
                    val decl = newVariable(expr.name, expr.autoType())
                    decl.language = expr.language
                    decl.location = expr.location
                    decl.isImplicit = true

                    // We cannot assign an initializer here because this will lead to duplicate
                    // DFG edges, but we need to propagate the type information
                    if (assign.rhs.size < assign.lhs.size && assign.rhs.size == 1) {
                        assign.rhs[0].registerTypeObserver(InitializerTypePropagation(decl, idx))
                    } else {
                        assign.rhs[idx].registerTypeObserver(InitializerTypePropagation(decl))
                    }

                    // Add it to the scope, so other assignments / references can "see" it.
                    scopeManager.addDeclaration(decl)
                    assign.declarations += decl
                }
            }
        }
    }

    override fun cleanup() {
        // Nothing to do
    }
}

/**
 * The type actually iterated over by a `range` clause. A named type -- `type PADataSequence
 * []PAData` -- carries its slice or map nature in its super types rather than in the type itself,
 * so we look there as well.
 *
 * We deliberately do not use `underlyingType`, which expects a single super type: a named type may
 * have several once the interfaces it satisfies are known, and we only care about the one that
 * makes it iterable.
 */
private val Type.iterated: Type?
    get() {
        // A map is represented as an object type named "map" whose generics are its key and
        // value type, rather than as a MapType.
        fun Type.isIterable() =
            this is MapType ||
                this is PointerType ||
                (this is ObjectType && this.name.localName == "map" && this.generics.size == 2)

        return if (this.isIterable()) this else this.superTypes.firstOrNull { it.isIterable() }
    }
