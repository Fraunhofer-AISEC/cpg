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
package de.fraunhofer.aisec.cpg

import de.fraunhofer.aisec.cpg.frontends.HasVisibilityModifiers
import de.fraunhofer.aisec.cpg.frontends.Language
import de.fraunhofer.aisec.cpg.frontends.SupportsNewParse
import de.fraunhofer.aisec.cpg.frontends.TestLanguage
import de.fraunhofer.aisec.cpg.frontends.TestLanguageFrontend
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.Constructor
import de.fraunhofer.aisec.cpg.graph.declarations.Field
import de.fraunhofer.aisec.cpg.graph.declarations.Function
import de.fraunhofer.aisec.cpg.graph.declarations.Method
import de.fraunhofer.aisec.cpg.graph.declarations.Record
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnit
import de.fraunhofer.aisec.cpg.graph.declarations.Variable
import de.fraunhofer.aisec.cpg.graph.expressions.Construction
import de.fraunhofer.aisec.cpg.graph.expressions.MemberCall
import de.fraunhofer.aisec.cpg.graph.expressions.Reference
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.graph.unknownType
import de.fraunhofer.aisec.cpg.passes.ControlFlowSensitiveDFGPass
import de.fraunhofer.aisec.cpg.passes.DFGPass
import de.fraunhofer.aisec.cpg.passes.EvaluationOrderGraphPass
import de.fraunhofer.aisec.cpg.passes.ImportResolver
import de.fraunhofer.aisec.cpg.passes.PointsToPass
import de.fraunhofer.aisec.cpg.passes.SymbolResolver
import de.fraunhofer.aisec.cpg.passes.TypeHierarchyResolver
import de.fraunhofer.aisec.cpg.passes.TypeResolver
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * A minimal test language whose frontend understands two "content" forms, so we can exercise
 * [TranslationManager.addSource]'s stale-inferred-stub cleanup without needing a real parser:
 * - `call:<name>` creates a top-level function `main(p)` whose body calls `<name>(p)`. If `<name>`
 *   is not yet declared anywhere, [de.fraunhofer.aisec.cpg.passes.SymbolResolver] will infer a stub
 *   [de.fraunhofer.aisec.cpg.graph.declarations.Function] for it (like any other language).
 * - `define:<name>` creates a single, real, top-level function declaration named `<name>`.
 */
class StaleStubTestLanguage : TestLanguage(), HasVisibilityModifiers {
    override val fileExtensions: List<String>
        get() = listOf("stale")

    override val frontend: KClass<out StaleStubTestLanguageFrontend>
        get() = StaleStubTestLanguageFrontend::class
}

class StaleStubTestLanguageFrontend(
    ctx: TranslationContext = TranslationContext(TranslationConfiguration.builder().build()),
    language: Language<TestLanguageFrontend> = StaleStubTestLanguage(),
) : TestLanguageFrontend(ctx, language), SupportsNewParse {
    override fun parse(file: File): TranslationUnit {
        return parse(file.readText(), file.toPath())
    }

    override fun parse(content: String, path: Path?): TranslationUnit {
        val tu = newTranslationUnit(path?.fileName?.toString() ?: content)
        scopeManager.resetToGlobal(tu)

        when {
            content.startsWith("call:") -> {
                val name = content.removePrefix("call:")
                newFunction("main", holder = tu, enterScope = true) { main ->
                    newParameter("p", holder = main)
                    main.body =
                        newBlock(enterScope = true) { block ->
                            val call = newCall(newReference(name))
                            call.arguments += newReference("p")
                            block.statements += call
                        }
                }
            }
            content.startsWith("define:") -> {
                // Matches the single-parameter signature that "call:" infers a stub with, so the
                // real declaration's signature is compatible with the stale stub's.
                val name = content.removePrefix("define:")
                newFunction(name, holder = tu, enterScope = true) { newParameter("p", holder = it) }
            }
            content.startsWith("topcall:") -> {
                // "topcall:<name>" creates a top-level, module-scope call to <name>(p) directly in
                // the TU's statements -- i.e. with NO enclosing Function -- so that
                // SymbolResolver infers a stub for it exactly like "call:", but any later stub
                // cleanup has no Function to mark dirty and must fall back to the enclosing
                // TranslationUnit instead.
                val name = content.removePrefix("topcall:")
                val call = newCall(newReference(name))
                call.arguments += newLiteral(0)
                tu.statements += call
            }
            content.startsWith("definewithdefault:") -> {
                // "definewithdefault:<name>" declares a real, top-level function <name>(p, extra =
                // <default>) with a SECOND, trailing parameter that has a default value -- unlike
                // "define:", which matches the stub's arity 1:1. This exercises the case where the
                // real definition has MORE parameters than the call the stub was inferred from
                // (the stub for "call:<name>" only has 1 parameter, matching that call's single
                // argument).
                val name = content.removePrefix("definewithdefault:")
                newFunction(name, holder = tu, enterScope = true) { function ->
                    newParameter("p", holder = function)
                    newParameter("extra", holder = function) { it.default = newLiteral(0) }
                }
            }
            content.startsWith("classcall:") -> {
                // "classcall:ClassName:methodName" declares a record ClassName with a method
                // "caller" that calls the (not yet declared) methodName on the implicit receiver,
                // so SymbolResolver infers a Method stub inside ClassName's RecordScope.
                val (className, methodName) = content.removePrefix("classcall:").split(":")
                newRecord(className, "class", holder = tu, enterScope = true) { record ->
                    newMethod(
                        "caller",
                        recordDeclaration = record,
                        holder = record,
                        enterScope = true,
                    ) { caller ->
                        caller.receiver = newVariable("this", record.toType())
                        caller.body =
                            newBlock(enterScope = true) { block ->
                                block.statements += newCall(newReference(methodName))
                            }
                    }
                }
            }
            content.startsWith("definemethod:") -> {
                // "definemethod:ClassName:methodName" adds a real method named methodName to the
                // *pre-existing* record ClassName (looked up via the live, shared ScopeManager),
                // to exercise stub cleanup for a non-global (RecordScope) declaration. The method
                // is registered as a symbol in the record's scope (like a real reopened-class/
                // partial-class declaration would be), but kept as an AST child of this new `tu`
                // so it is discoverable via `tu.allChildren<Function>()`, matching the contract
                // that TranslationManager.updateIncrementally relies on.
                val (className, methodName) = content.removePrefix("definemethod:").split(":")
                val record =
                    scopeManager
                        .lookupSymbolByName(
                            Name(className),
                            language,
                            startScope = scopeManager.globalScope,
                        )
                        .filterIsInstance<Record>()
                        .single()

                scopeManager.enterScope(record)
                val method =
                    newMethod(methodName, recordDeclaration = record, enterScope = true) { m ->
                        m.receiver = newVariable("this", record.toType())
                    }
                scopeManager.addDeclaration(method)
                scopeManager.leaveScope(record)

                tu.addDeclaration(method)
            }
            content.startsWith("defineprivatemethod:") -> {
                // "defineprivatemethod:ClassName:methodName" mirrors "definemethod:" exactly, but
                // the added method has Visibility.PRIVATE -- used to exercise isAccessibleFrom's
                // PRIVATE-vs-PROTECTED distinction (a private base-class member must not become
                // reachable from a subclass just because the subclass inherits from it).
                val (className, methodName) =
                    content.removePrefix("defineprivatemethod:").split(":")
                val record =
                    scopeManager
                        .lookupSymbolByName(
                            Name(className),
                            language,
                            startScope = scopeManager.globalScope,
                        )
                        .filterIsInstance<Record>()
                        .single()

                scopeManager.enterScope(record)
                val method =
                    newMethod(methodName, recordDeclaration = record, enterScope = true) { m ->
                        m.receiver = newVariable("this", record.toType())
                        m.visibility = Visibility.PRIVATE
                    }
                scopeManager.addDeclaration(method)
                scopeManager.leaveScope(record)

                tu.addDeclaration(method)
            }
            content.startsWith("classnew:") -> {
                // "classnew:ClassName" declares an empty record ClassName (no constructor at all)
                // plus a top-level function "main" that constructs `new ClassName()`, so
                // SymbolResolver infers a (zero-arg) Constructor stub inside ClassName's
                // RecordScope. This exercises the Constructor-vs-Method branch of
                // detachInferredDeclaration, since Constructor extends Method but is stored in
                // Record.constructorEdges, not Record.methodEdges.
                val className = content.removePrefix("classnew:")
                newRecord(className, "class", holder = tu, enterScope = true) {}
                newFunction("main", holder = tu, enterScope = true) { main ->
                    main.body =
                        newBlock(enterScope = true) { block ->
                            val construct = newConstruction(className)
                            construct.type = objectType(className)
                            block.statements += construct
                        }
                }
            }
            content.startsWith("loopfunc:") -> {
                // "loopfunc:<name>" declares a top-level function <name>(p) with a local variable
                // `x` that is written before, read in a loop condition, rewritten in the loop
                // body, and read again after the loop. Unlike "call:"/"define:", this gives
                // EvaluationOrderGraphPass/BasicBlockCollectorPass/ControlDependenceGraphPass/
                // SccPass (branch + loop) and DFGPass/ControlFlowSensitiveDFGPass (write-then-read
                // of `x`) something genuinely non-trivial to compute.
                val name = content.removePrefix("loopfunc:")
                newFunction(name, holder = tu, enterScope = true) { function ->
                    newParameter("p", holder = function)
                    function.body =
                        newBlock(enterScope = true) { block ->
                            block.statements += newDeclarationStatement { declStmt ->
                                newVariable("x", holder = declStmt) {
                                    it.initializer = newLiteral(0)
                                }
                            }
                            block.statements +=
                                newWhile(enterScope = true) { whileStmt ->
                                    whileStmt.condition =
                                        newBinaryOperator("<") {
                                            it.lhs = newReference("x")
                                            it.rhs = newLiteral(10)
                                        }
                                    whileStmt.statement =
                                        newBlock(enterScope = true) { innerBlock ->
                                            innerBlock.statements +=
                                                newAssign(
                                                    operatorCode = "=",
                                                    lhs = listOf(newReference("x")),
                                                    rhs =
                                                        listOf(
                                                            newBinaryOperator("+") {
                                                                it.lhs = newReference("x")
                                                                it.rhs = newLiteral(1)
                                                            }
                                                        ),
                                                )
                                        }
                                }
                            block.statements += newReturn { it.returnValue = newReference("x") }
                        }
                }
            }
            content.startsWith("defineconstructor:") -> {
                // "defineconstructor:ClassName" adds a real, zero-arg constructor to the
                // *pre-existing* record ClassName (looked up via the live, shared ScopeManager), to
                // exercise stub cleanup for an inferred Constructor.
                val className = content.removePrefix("defineconstructor:")
                val record =
                    scopeManager
                        .lookupSymbolByName(
                            Name(className),
                            language,
                            startScope = scopeManager.globalScope,
                        )
                        .filterIsInstance<Record>()
                        .single()

                scopeManager.enterScope(record)
                val constructor =
                    newConstructor(className, recordDeclaration = record, enterScope = true) {}
                scopeManager.addDeclaration(constructor)
                scopeManager.leaveScope(record)

                tu.addDeclaration(constructor)
            }
            content.startsWith("refunresolved:") -> {
                // "refunresolved:<name>" creates a top-level function "main(p)" whose body returns
                // a bare, unqualified reference to <name>. StaleStubTestLanguage does not implement
                // HasGlobalVariables, so SymbolResolver.tryVariableInference cannot infer a global
                // for it -- the reference is left genuinely unresolved (refersTo == null), unlike
                // "call:", which always gets an inferred stub.
                val name = content.removePrefix("refunresolved:")
                newFunction("main", holder = tu, enterScope = true) { main ->
                    newParameter("p", holder = main)
                    main.body =
                        newBlock(enterScope = true) { block ->
                            block.statements += newReturn { it.returnValue = newReference(name) }
                        }
                }
            }
            content.startsWith("defineglobal:") -> {
                // "defineglobal:<name>" declares a single, real, top-level (GlobalScope) Variable
                // named <name>.
                val name = content.removePrefix("defineglobal:")
                newVariable(name, holder = tu)
            }
            content.startsWith("globalwithref:") -> {
                // "globalwithref:<name>" declares a real, top-level Variable <name> AND a function
                // "mainRef(p)" that returns a reference to it, both in the same TU, so the initial
                // analyze() resolves the reference directly to this real declaration (never
                // unresolved, never inferred).
                val name = content.removePrefix("globalwithref:")
                newVariable(name, holder = tu)
                newFunction("mainRef", holder = tu, enterScope = true) { main ->
                    newParameter("p", holder = main)
                    main.body =
                        newBlock(enterScope = true) { block ->
                            block.statements += newReturn { it.returnValue = newReference(name) }
                        }
                }
            }
            content.startsWith("localvar:") -> {
                // "localvar:<funcName>:<varName>" declares a top-level function <funcName>(p) with
                // a function-local variable <varName> that is never referenced. Used to verify that
                // a local declaration is never treated as a "new non-local symbol" candidate for
                // the batched reconciliation scan.
                val (funcName, varName) = content.removePrefix("localvar:").split(":")
                newFunction(funcName, holder = tu, enterScope = true) { function ->
                    newParameter("p", holder = function)
                    function.body =
                        newBlock(enterScope = true) { block ->
                            block.statements += newDeclarationStatement { declStmt ->
                                newVariable(varName, holder = declStmt)
                            }
                        }
                }
            }
            content.startsWith("multidefine:") -> {
                // "multidefine:<name1>,<name2>,..." declares several real, top-level, single
                // parameter functions in ONE file/TU, so a single addSource call introduces several
                // new non-local symbols at once.
                val names = content.removePrefix("multidefine:").split(",")
                for (name in names) {
                    newFunction(name, holder = tu, enterScope = true) {
                        newParameter("p", holder = it)
                    }
                }
            }
            content.startsWith("classref:") -> {
                // "classref:ClassName:fieldName" declares a record ClassName with a method "reader"
                // that returns a bare, unqualified reference to fieldName on the implicit receiver,
                // so SymbolResolver infers a Field stub inside ClassName's RecordScope (since
                // StaleStubTestLanguage/TestLanguage implements HasImplicitReceiver).
                val (className, fieldName) = content.removePrefix("classref:").split(":")
                newRecord(className, "class", holder = tu, enterScope = true) { record ->
                    newMethod(
                        "reader",
                        recordDeclaration = record,
                        holder = record,
                        enterScope = true,
                    ) { reader ->
                        reader.receiver = newVariable("this", record.toType())
                        reader.body =
                            newBlock(enterScope = true) { block ->
                                block.statements += newReturn {
                                    it.returnValue = newReference(fieldName)
                                }
                            }
                    }
                }
            }
            content.startsWith("definefield:") -> {
                // "definefield:ClassName:fieldName" adds a real Field named fieldName to the
                // *pre-existing* record ClassName (looked up via the live, shared ScopeManager),
                // mirroring "definemethod:" but for a Field/Reference instead of a Method/Call.
                val (className, fieldName) = content.removePrefix("definefield:").split(":")
                val record =
                    scopeManager
                        .lookupSymbolByName(
                            Name(className),
                            language,
                            startScope = scopeManager.globalScope,
                        )
                        .filterIsInstance<Record>()
                        .single()

                scopeManager.enterScope(record)
                val field = newField(fieldName, holder = record)
                scopeManager.leaveScope(record)

                tu.addDeclaration(field)
            }
            content.startsWith("membercall:") -> {
                // "membercall:ClassName:methodName" declares an (initially empty) record ClassName
                // and a top-level, ZERO-argument function "main(p: ClassName)" whose body performs
                // an EXPLICIT-receiver member call `p.methodName()` -- a genuine MemberCall/
                // MemberAccess resolved via `p`'s static type, not an implicit "this" receiver like
                // "classcall:". `main` is not lexically inside ClassName at all, so this exercises
                // reconcileCalls's receiver-type-based (not lexical-scope-based) reachability check
                // for explicit member calls/accesses. Zero-argument to match "definemethod:"'s
                // signature (which only ever adds a receiver, no other parameters).
                val (className, methodName) = content.removePrefix("membercall:").split(":")
                newRecord(className, "class", holder = tu, enterScope = true) {}
                newFunction("main", holder = tu, enterScope = true) { main ->
                    newParameter("p", objectType(className), holder = main)
                    main.body =
                        newBlock(enterScope = true) { block ->
                            val access = newMemberAccess(methodName, base = newReference("p"))
                            block.statements += newMemberCall(access)
                        }
                }
            }
            content.startsWith("membercallderived:") -> {
                // "membercallderived:BaseName:DerivedName:methodName" declares two (initially
                // empty) records, DerivedName extending BaseName, and a top-level, zero-argument
                // function "main(p: DerivedName)" whose body performs an explicit-receiver member
                // call `p.methodName()`. Used to exercise isReachableFrom's ancestor-chain walk: a
                // method later added to the BASE class must still resolve a pre-existing call whose
                // receiver's static type is the DERIVED class.
                val (baseName, derivedName, methodName) =
                    content.removePrefix("membercallderived:").split(":")
                val base = newRecord(baseName, "class", holder = tu, enterScope = true) {}
                val derived = newRecord(derivedName, "class", holder = tu, enterScope = true) {}
                derived.addSuperClass(base.toType())
                newFunction("main", holder = tu, enterScope = true) { main ->
                    newParameter("p", objectType(derivedName), holder = main)
                    main.body =
                        newBlock(enterScope = true) { block ->
                            val access = newMemberAccess(methodName, base = newReference("p"))
                            block.statements += newMemberCall(access)
                        }
                }
            }
            content.startsWith("membercallwithinderived:") -> {
                // "membercallwithinderived:BaseName:DerivedName:methodName" declares two (initially
                // empty) records, DerivedName extending BaseName, and a method "caller(other:
                // DerivedName)" declared INSIDE DerivedName whose body performs an
                // explicit-receiver member call `other.methodName()` on a DIFFERENT
                // DerivedName-typed instance (not `this`). Unlike "membercallderived:" (a free
                // top-level function, so the accessing record is null), the accessing record here
                // IS DerivedName -- letting us exercise isAccessibleFrom's PRIVATE-vs-PROTECTED
                // distinction: a `private` method later added to the BASE class must NOT become a
                // viable candidate just because the caller's own record (Derived) transitively
                // inherits from Base, even though the receiver's static type is also reachable via
                // that same ancestor chain.
                val (baseName, derivedName, methodName) =
                    content.removePrefix("membercallwithinderived:").split(":")
                val base = newRecord(baseName, "class", holder = tu, enterScope = true) {}
                val derived =
                    newRecord(derivedName, "class", holder = tu, enterScope = true) { record ->
                        newMethod(
                            "caller",
                            recordDeclaration = record,
                            holder = record,
                            enterScope = true,
                        ) { caller ->
                            caller.receiver = newVariable("this", record.toType())
                            newParameter("other", objectType(derivedName), holder = caller)
                            caller.body =
                                newBlock(enterScope = true) { block ->
                                    val access =
                                        newMemberAccess(methodName, base = newReference("other"))
                                    block.statements += newMemberCall(access)
                                }
                        }
                    }
                derived.addSuperClass(base.toType())
            }
            content.startsWith("definezeroarg:") -> {
                // "definezeroarg:<name>" declares a single, real, top-level, ZERO-parameter free
                // function <name>().
                val name = content.removePrefix("definezeroarg:")
                newFunction(name, holder = tu, enterScope = true) {}
            }
            content.startsWith("defineconstructor2:") -> {
                // "defineconstructor2:ClassName" adds a SECOND, distinct, real zero-arg constructor
                // to the *pre-existing* record ClassName, so that a Construction resolved against
                // BOTH constructors is genuinely ambiguous (two equally-viable candidates).
                val className = content.removePrefix("defineconstructor2:")
                val record =
                    scopeManager
                        .lookupSymbolByName(
                            Name(className),
                            language,
                            startScope = scopeManager.globalScope,
                        )
                        .filterIsInstance<Record>()
                        .single()

                scopeManager.enterScope(record)
                val constructor =
                    newConstructor(className, recordDeclaration = record, enterScope = true) {}
                scopeManager.addDeclaration(constructor)
                scopeManager.leaveScope(record)

                tu.addDeclaration(constructor)
            }
        }

        return tu
    }

    override fun typeOf(type: Any): Type {
        return unknownType()
    }

    override fun codeOf(astNode: Any): String? {
        return null
    }

    override fun locationOf(astNode: Any): PhysicalLocation? {
        return null
    }

    override fun setComment(node: Node, astNode: Any) {}
}

class IncrementalUpdateTest {
    private fun tempSource(topLevel: File, fileName: String, content: String): File {
        return File(topLevel, fileName).apply {
            writeText(content)
            deleteOnExit()
        }
    }

    /**
     * Registers a minimal pass pipeline without [PointsToPass]/[ControlFlowSensitiveDFGPass] (see
     * the identically-named helper in `PartialPassExecutionTest` for why): with neither registered,
     * [DFGPass] itself deterministically attaches argument-to-parameter edges
     * ([de.fraunhofer.aisec.cpg.helpers.Util.attachCallParameters]) and reference read/write edges,
     * which is exactly the condition [IncrementalUpdate.kt]'s `dfgHandlesArgumentEdgesItself` also
     * gates the new direct-attach graph surgery on -- required for the tests below to observe it.
     */
    private fun TranslationConfiguration.Builder.minimalPasses(): TranslationConfiguration.Builder {
        registerPass<TypeHierarchyResolver>()
        registerPass<SymbolResolver>()
        registerPass<ImportResolver>()
        registerPass<DFGPass>()
        registerPass<EvaluationOrderGraphPass>()
        registerPass<TypeResolver>()
        return this
    }

    @Test
    fun testAddSourceCleansUpStaleInferredStub() {
        val topLevel =
            Files.createTempDirectory("cpg-incremental-update-test").toFile().apply {
                deleteOnExit()
            }
        val callerFile = tempSource(topLevel, "caller.stale", "call:foo")

        val config =
            TranslationConfiguration.builder()
                .topLevel(topLevel)
                .sourceLocations(callerFile)
                .registerLanguage<StaleStubTestLanguage>()
                .defaultPasses()
                .disableCleanup()
                .build()

        val manager = TranslationManager.builder().config(config).build()
        val result = manager.analyze().get()
        assertTrue(result.isLive)

        val component = result.components.single()
        val fooCall = result.calls.single { it.name.localName == "foo" }
        val main = result.functions.single { it.name.localName == "main" }

        // Before addSource: the call was resolved against an inferred stub.
        val stub = fooCall.invokes.singleOrNull()
        assertNotNull(stub, "Expected the unresolved call to 'foo' to create an inferred stub")
        assertTrue(stub.isInferred)
        assertEquals(1, stub.parameters.size)

        val param = stub.parameters.single()
        assertTrue(
            param.prevDFG.isNotEmpty(),
            "Expected DFGPass to connect the call's argument to the stub's parameter",
        )
        assertTrue(fooCall.prevDFG.contains(stub))

        // Now add the real definition of 'foo'.
        val second = tempSource(topLevel, "foo.stale", "define:foo")
        val tu = manager.addSource(result, component, second)
        assertNotNull(tu)

        val realFoo = tu.declarations.filterIsInstance<Function>().single()
        assertEquals("foo", realFoo.name.localName)
        assertFalse(realFoo.isInferred)

        // The stale invokes edge (and the DFG edges that hung off it) must be gone, replaced by the
        // real declaration -- the batched reconciliation adds it immediately rather than leaving
        // `invokes` empty until a later runDirtyPasses.
        assertEquals(listOf(realFoo), fooCall.invokes)
        assertFalse(fooCall.prevDFG.contains(stub))
        assertTrue(param.prevDFG.isEmpty())

        // The stub is now orphaned and should have been detached entirely.
        assertTrue(stub.calledBy.isEmpty())
        assertFalse(component.translationUnits.first().declarations.contains(stub))

        val remaining =
            result.finalCtx.scopeManager.lookupSymbolByName(
                Name("foo"),
                result.finalCtx.availableLanguage<StaleStubTestLanguage>()!!,
                startScope = result.finalCtx.scopeManager.globalScope,
            )
        assertFalse(remaining.contains(stub))
        assertTrue(remaining.contains(realFoo))

        // Dirty-marking: the new declaration and the caller function must be flagged for
        // SymbolResolver.
        assertTrue(result.dirtyNodes[realFoo]?.contains(SymbolResolver::class) == true)
        assertTrue(result.dirtyNodes[main]?.contains(SymbolResolver::class) == true)
    }

    @Test
    fun testAddSourceCleansUpStaleInferredStubInRecordScope() {
        val topLevel =
            Files.createTempDirectory("cpg-incremental-update-record-test").toFile().apply {
                deleteOnExit()
            }
        val callerFile = tempSource(topLevel, "caller.stale", "classcall:Greeter:helper")

        val config =
            TranslationConfiguration.builder()
                .topLevel(topLevel)
                .sourceLocations(callerFile)
                .registerLanguage<StaleStubTestLanguage>()
                .defaultPasses()
                .disableCleanup()
                .build()

        val manager = TranslationManager.builder().config(config).build()
        val result = manager.analyze().get()
        val component = result.components.single()

        val helperCall = result.calls.single { it.name.localName == "helper" }
        val stub = helperCall.invokes.singleOrNull()
        assertNotNull(stub, "Expected the unresolved call to 'helper' to create an inferred stub")
        assertTrue(stub.isInferred)
        assertTrue(stub is Method, "Expected the stub to be inferred as a Method, not a Function")

        // Sanity check: the stub was registered in the record's scope, not the global scope.
        val record = result.records.single { it.name.localName == "Greeter" }
        assertTrue(record.methods.contains(stub))

        // Add the real method to the (pre-existing) record.
        val second = tempSource(topLevel, "helper.stale", "definemethod:Greeter:helper")
        val tu = manager.addSource(result, component, second)
        assertNotNull(tu)

        val realHelper = tu.declarations.filterIsInstance<Function>().single()
        assertEquals("helper", realHelper.name.localName)
        assertFalse(realHelper.isInferred)

        // The stale invokes edge must be gone (replaced by the real declaration), and the
        // now-orphaned stub detached from the record.
        assertEquals(listOf(realHelper), helperCall.invokes)
        assertTrue(stub.calledBy.isEmpty())
        assertFalse(record.methods.contains(stub))

        assertTrue(result.dirtyNodes[realHelper]?.contains(SymbolResolver::class) == true)
    }

    @Test
    fun testAddSourceDetachesStaleInferredConstructorStub() {
        // Regression test: Constructor extends Method, but Record stores constructors in
        // Record.constructorEdges/constructors, not Record.methodEdges/methods (see
        // Record.addDeclaration, which checks `is Constructor` before `is Method`).
        // detachInferredDeclaration must mirror that same check order, otherwise it silently
        // no-ops for a stale inferred Constructor stub (it would try `parent.methods.remove(...)`,
        // which never contains the constructor) and the stub is never detached.
        val topLevel =
            Files.createTempDirectory("cpg-incremental-update-constructor-test").toFile().apply {
                deleteOnExit()
            }
        val callerFile = tempSource(topLevel, "caller.stale", "classnew:Foo")

        val config =
            TranslationConfiguration.builder()
                .topLevel(topLevel)
                .sourceLocations(callerFile)
                .registerLanguage<StaleStubTestLanguage>()
                .defaultPasses()
                .disableCleanup()
                .build()

        val manager = TranslationManager.builder().config(config).build()
        val result = manager.analyze().get()
        val component = result.components.single()

        val record = result.records.single { it.name.localName == "Foo" }
        val stub = record.constructors.singleOrNull()
        assertNotNull(stub, "Expected `new Foo()` to create an inferred Constructor stub")
        assertTrue(stub.isInferred)
        assertTrue(record.constructors.contains(stub))
        assertTrue(stub.calledBy.isNotEmpty())

        val main = result.functions.single { it.name.localName == "main" }
        val construction = main.body.allChildren<Construction>().single()
        assertSame(stub, construction.constructor)

        // Add the real (zero-arg) constructor to the (pre-existing) record.
        val second = tempSource(topLevel, "foo.stale", "defineconstructor:Foo")
        val tu = manager.addSource(result, component, second)
        assertNotNull(tu)

        val realConstructor = tu.declarations.filterIsInstance<Constructor>().single()
        assertFalse(realConstructor.isInferred)

        // The now-orphaned Constructor stub must be detached from the record's constructors (not
        // silently left behind because it was looked for in the wrong collection).
        assertTrue(stub.calledBy.isEmpty())
        assertFalse(record.constructors.contains(stub))

        // Regression test: Construction.constructor is a separate backing field that the setter
        // forwards one-directionally to `invokes` -- mutating `construction.invokes` directly must
        // not leave `construction.constructor` stale; it must be resynced to the real constructor
        // that replaced the now-detached stub.
        assertEquals(listOf<Function>(realConstructor), construction.invokes)
        assertSame(
            realConstructor,
            construction.constructor,
            "Expected Construction.constructor to be resynced to the new real constructor",
        )

        assertTrue(result.dirtyNodes[realConstructor]?.contains(SymbolResolver::class) == true)
    }

    @Test
    fun testAddSourceRecognizesStaleStubWithFewerArgsThanRealFunctionsDefaultParameters() {
        // Regression test: Function.matchesSignature is designed to be called as
        // `candidate.matchesSignature(callArgumentTypes)`. Calling it the other way around --
        // `stub.matchesSignature(newFunction's parameter types)` -- incorrectly reports
        // IncompatibleSignature whenever the real function has MORE parameters than the call the
        // stub was inferred from (e.g. a trailing default/optional parameter), since the stub's
        // (few) parameters can never consume the real function's (more) parameter types. This
        // must not prevent the stale stub from being recognized and cleaned up.
        val topLevel =
            Files.createTempDirectory("cpg-incremental-update-default-param-test").toFile().apply {
                deleteOnExit()
            }
        val callerFile = tempSource(topLevel, "caller.stale", "call:foo")

        val config =
            TranslationConfiguration.builder()
                .topLevel(topLevel)
                .sourceLocations(callerFile)
                .registerLanguage<StaleStubTestLanguage>()
                .defaultPasses()
                .disableCleanup()
                .build()

        val manager = TranslationManager.builder().config(config).build()
        val result = manager.analyze().get()
        val component = result.components.single()

        val fooCall = result.calls.single { it.name.localName == "foo" }
        val stub = fooCall.invokes.singleOrNull()
        assertNotNull(stub, "Expected the unresolved call to 'foo' to create an inferred stub")
        assertTrue(stub.isInferred)
        assertEquals(1, stub.parameters.size)

        // The real 'foo' has TWO parameters (the second one with a default), i.e. strictly more
        // than the 1-argument call the stub was inferred from.
        val second = tempSource(topLevel, "foo.stale", "definewithdefault:foo")
        val tu = manager.addSource(result, component, second)
        assertNotNull(tu)

        val realFoo = tu.declarations.filterIsInstance<Function>().single()
        assertEquals("foo", realFoo.name.localName)
        assertEquals(2, realFoo.parameters.size)
        assertFalse(realFoo.isInferred)

        // The stub must still be recognized as stale and cleaned up, despite the arity mismatch.
        assertEquals(listOf(realFoo), fooCall.invokes)
        assertTrue(stub.calledBy.isEmpty())
        assertFalse(component.translationUnits.first().declarations.contains(stub))
        assertTrue(result.dirtyNodes[realFoo]?.contains(SymbolResolver::class) == true)
    }

    @Test
    fun testAddSourceMarksEnclosingTranslationUnitDirtyForTopLevelStaleCall() {
        // Regression test: a stale call with NO enclosing Function (e.g. a top-level/module-scope
        // statement -- TranslationUnit is itself an EOGStarterHolder "to catch any static
        // statements in the TU") must still get dirty-marked for re-resolution once its stale
        // invokes/DFG edges are torn down; `call.firstParentOrNull<Function>()` silently no-op'ing
        // on null must not leave the call permanently unresolved.
        val topLevel =
            Files.createTempDirectory("cpg-incremental-update-toplevel-test").toFile().apply {
                deleteOnExit()
            }
        val callerFile = tempSource(topLevel, "caller.stale", "topcall:foo")

        val config =
            TranslationConfiguration.builder()
                .topLevel(topLevel)
                .sourceLocations(callerFile)
                .registerLanguage<StaleStubTestLanguage>()
                .defaultPasses()
                .disableCleanup()
                .build()

        val manager = TranslationManager.builder().config(config).build()
        val result = manager.analyze().get()
        val component = result.components.single()
        val callerTu = component.translationUnits.single()

        val fooCall = result.calls.single { it.name.localName == "foo" }
        assertNull(
            fooCall.firstParentOrNull<Function>(),
            "Expected the top-level call to have no enclosing Function",
        )
        val stub = fooCall.invokes.singleOrNull()
        assertNotNull(stub, "Expected the unresolved top-level call to 'foo' to create a stub")
        assertTrue(stub.isInferred)

        val second = tempSource(topLevel, "foo.stale", "define:foo")
        val tu = manager.addSource(result, component, second)
        assertNotNull(tu)

        val realFoo = tu.declarations.filterIsInstance<Function>().single()
        assertFalse(realFoo.isInferred)

        // The stale invokes edge is gone, replaced by the real declaration...
        assertEquals(listOf(realFoo), fooCall.invokes)
        // ...and the enclosing TranslationUnit (not a Function, since there is none) must have
        // been marked dirty for SymbolResolver, so a later runDirtyPasses actually revisits it.
        assertTrue(result.dirtyNodes[callerTu]?.contains(SymbolResolver::class) == true)

        manager.runDirtyPasses(result)
        assertEquals(listOf(realFoo), fooCall.invokes)
    }

    @Test
    fun testAddSourceResolvesCompletelyUnresolvedCall() {
        // Unlike "call:" with the default configuration (which always gets an inferred stub),
        // disabling function inference leaves the call genuinely, completely unresolved -- no stub
        // is ever created. reconcileCalls must still pick this up once the real function arrives.
        val topLevel =
            Files.createTempDirectory("cpg-incremental-update-unresolved-call-test")
                .toFile()
                .apply { deleteOnExit() }
        val callerFile = tempSource(topLevel, "caller.stale", "call:foo")

        val config =
            TranslationConfiguration.builder()
                .topLevel(topLevel)
                .sourceLocations(callerFile)
                .registerLanguage<StaleStubTestLanguage>()
                .defaultPasses()
                .inferenceConfiguration(
                    InferenceConfiguration.Builder().inferFunctions(false).build()
                )
                .disableCleanup()
                .build()

        val manager = TranslationManager.builder().config(config).build()
        val result = manager.analyze().get()
        val component = result.components.single()

        val fooCall = result.calls.single { it.name.localName == "foo" }
        assertTrue(
            fooCall.invokes.isEmpty(),
            "Expected the call to remain completely unresolved with function inference disabled",
        )
        val main = result.functions.single { it.name.localName == "main" }

        val second = tempSource(topLevel, "foo.stale", "define:foo")
        val tu = manager.addSource(result, component, second)
        assertNotNull(tu)

        val realFoo = tu.declarations.filterIsInstance<Function>().single()
        assertFalse(realFoo.isInferred)

        assertEquals(listOf(realFoo), fooCall.invokes)
        assertTrue(result.dirtyNodes[realFoo]?.contains(SymbolResolver::class) == true)
        assertTrue(result.dirtyNodes[main]?.contains(SymbolResolver::class) == true)
    }

    @Test
    fun testAddSourceAddsNewCandidateAlongsideExistingRealInvoke() {
        // A call already resolved to a real, non-inferred function must not lose that resolution
        // just because a second, equally-viable real function shows up later -- Call.invokes
        // tolerates multiple candidates, so the new one is simply added.
        val topLevel =
            Files.createTempDirectory("cpg-incremental-update-multi-candidate-test")
                .toFile()
                .apply { deleteOnExit() }
        val firstBarFile = tempSource(topLevel, "bar.stale", "define:bar")
        val callerFile = tempSource(topLevel, "caller.stale", "call:bar")

        val config =
            TranslationConfiguration.builder()
                .topLevel(topLevel)
                .sourceLocations(firstBarFile, callerFile)
                .registerLanguage<StaleStubTestLanguage>()
                .defaultPasses()
                .disableCleanup()
                .build()

        val manager = TranslationManager.builder().config(config).build()
        val result = manager.analyze().get()
        val component = result.components.single()

        val barCall = result.calls.single { it.name.localName == "bar" }
        val firstBar = result.functions.single { it.name.localName == "bar" }
        assertFalse(firstBar.isInferred)
        assertEquals(listOf(firstBar), barCall.invokes)
        val main = result.functions.single { it.name.localName == "main" }

        val second = tempSource(topLevel, "bar2.stale", "define:bar")
        val tu = manager.addSource(result, component, second)
        assertNotNull(tu)

        val secondBar = tu.declarations.filterIsInstance<Function>().single()
        assertFalse(secondBar.isInferred)
        assertNotSame(firstBar, secondBar)

        // Both candidates must be present -- the pre-existing, valid one was not removed.
        assertEquals(setOf(firstBar, secondBar), barCall.invokes.toSet())
        assertTrue(result.dirtyNodes[secondBar]?.contains(SymbolResolver::class) == true)
        assertTrue(result.dirtyNodes[main]?.contains(SymbolResolver::class) == true)
    }

    @Test
    fun testAddSourceRemovesInferredStubButKeepsExistingRealCandidate() {
        // A call whose `invokes` contains BOTH a real candidate and an inferred stub: once a
        // matching real declaration is added, only the inferred stub (the "bridge") is removed;
        // the pre-existing real candidate is left alone.
        val topLevel =
            Files.createTempDirectory("cpg-incremental-update-mixed-invokes-test").toFile().apply {
                deleteOnExit()
            }
        val callerFile = tempSource(topLevel, "caller.stale", "call:baz")

        val config =
            TranslationConfiguration.builder()
                .topLevel(topLevel)
                .sourceLocations(callerFile)
                .registerLanguage<StaleStubTestLanguage>()
                .defaultPasses()
                .disableCleanup()
                .build()

        val manager = TranslationManager.builder().config(config).build()
        val result = manager.analyze().get()
        val component = result.components.single()

        val bazCall = result.calls.single { it.name.localName == "baz" }
        val stub = bazCall.invokes.singleOrNull()
        assertNotNull(stub, "Expected the unresolved call to 'baz' to create an inferred stub")
        assertTrue(stub.isInferred)

        // SymbolResolver never actually leaves an invokes edge in exactly this state itself (a real
        // candidate never coexists with an inferred one for the same, still-unresolved call), so we
        // construct it directly here to exercise reconcileCalls's "remove only the inferred one,
        // keep everything else" behavior in isolation.
        val decoyFile = tempSource(topLevel, "decoy.stale", "define:decoy")
        val decoyTu = manager.addSource(result, component, decoyFile)
        assertNotNull(decoyTu)
        val decoyFn = decoyTu.declarations.filterIsInstance<Function>().single()
        bazCall.invokes = (bazCall.invokes + decoyFn).toMutableList()
        assertEquals(setOf(stub, decoyFn), bazCall.invokes.toSet())

        val second = tempSource(topLevel, "baz.stale", "define:baz")
        val tu = manager.addSource(result, component, second)
        assertNotNull(tu)

        val realBaz = tu.declarations.filterIsInstance<Function>().single()
        assertFalse(realBaz.isInferred)

        assertEquals(setOf(decoyFn, realBaz), bazCall.invokes.toSet())
        assertTrue(stub.calledBy.isEmpty())
        assertFalse(component.translationUnits.flatMap { it.declarations }.contains(stub))
    }

    @Test
    fun testAddSourceResolvesUnresolvedReference() {
        // A plain, non-call Reference (e.g. a global variable access) that is currently unresolved
        // gets resolved once a matching real, non-local declaration is added.
        val topLevel =
            Files.createTempDirectory("cpg-incremental-update-unresolved-ref-test").toFile().apply {
                deleteOnExit()
            }
        val callerFile = tempSource(topLevel, "caller.stale", "refunresolved:x")

        val config =
            TranslationConfiguration.builder()
                .topLevel(topLevel)
                .sourceLocations(callerFile)
                .registerLanguage<StaleStubTestLanguage>()
                .defaultPasses()
                .disableCleanup()
                .build()

        val manager = TranslationManager.builder().config(config).build()
        val result = manager.analyze().get()
        val component = result.components.single()

        val ref = result.allChildren<Reference>().single { it.name.localName == "x" }
        assertNull(ref.refersTo, "Expected the reference to 'x' to be completely unresolved")
        val main = result.functions.single { it.name.localName == "main" }

        val second = tempSource(topLevel, "x.stale", "defineglobal:x")
        val tu = manager.addSource(result, component, second)
        assertNotNull(tu)

        val realX = tu.declarations.filterIsInstance<Variable>().single()
        assertFalse(realX.isInferred)

        assertSame(realX, ref.refersTo)
        assertTrue(result.dirtyNodes[main]?.contains(SymbolResolver::class) == true)
    }

    @Test
    fun testAddSourceReplacesInferredFieldReference() {
        // A Reference resolved to an inferred Field gets replaced by the real Field once it is
        // added, mirroring the inferred-stub-is-a-bridge behavior for calls, generalized to the
        // single-edge Reference case.
        val topLevel =
            Files.createTempDirectory("cpg-incremental-update-inferred-field-test").toFile().apply {
                deleteOnExit()
            }
        val callerFile = tempSource(topLevel, "caller.stale", "classref:Greeter:secret")

        val config =
            TranslationConfiguration.builder()
                .topLevel(topLevel)
                .sourceLocations(callerFile)
                .registerLanguage<StaleStubTestLanguage>()
                .defaultPasses()
                .disableCleanup()
                .build()

        val manager = TranslationManager.builder().config(config).build()
        val result = manager.analyze().get()
        val component = result.components.single()

        val ref = result.allChildren<Reference>().single { it.name.localName == "secret" }
        val inferredField = ref.refersTo
        assertNotNull(inferredField, "Expected the reference to 'secret' to infer a Field")
        assertTrue(inferredField.isInferred)
        assertTrue(inferredField is Field, "Expected the inferred declaration to be a Field")

        val record = result.records.single { it.name.localName == "Greeter" }
        assertTrue(record.fields.contains(inferredField))

        val second = tempSource(topLevel, "secret.stale", "definefield:Greeter:secret")
        val tu = manager.addSource(result, component, second)
        assertNotNull(tu)

        val realField = tu.declarations.filterIsInstance<Field>().single()
        assertFalse(realField.isInferred)

        assertSame(realField, ref.refersTo)
        assertFalse(record.fields.contains(inferredField))
    }

    @Test
    fun testAddSourceLeavesReferenceResolvedToRealDeclarationUntouched() {
        // Case 3 for references (deliberately left alone): a Reference already resolved to a real,
        // non-inferred declaration must not be re-pointed at a new, same-named declaration based on
        // a name/scope heuristic.
        val topLevel =
            Files.createTempDirectory("cpg-incremental-update-already-resolved-ref-test")
                .toFile()
                .apply { deleteOnExit() }
        val callerFile = tempSource(topLevel, "caller.stale", "globalwithref:x")

        val config =
            TranslationConfiguration.builder()
                .topLevel(topLevel)
                .sourceLocations(callerFile)
                .registerLanguage<StaleStubTestLanguage>()
                .defaultPasses()
                .disableCleanup()
                .build()

        val manager = TranslationManager.builder().config(config).build()
        val result = manager.analyze().get()
        val component = result.components.single()

        val ref = result.allChildren<Reference>().single { it.name.localName == "x" }
        val originalX = ref.refersTo
        assertNotNull(originalX)
        assertFalse(originalX.isInferred)

        val second = tempSource(topLevel, "x2.stale", "defineglobal:x")
        val tu = manager.addSource(result, component, second)
        assertNotNull(tu)

        val newX = tu.declarations.filterIsInstance<Variable>().single()
        assertNotSame(originalX, newX)

        // Untouched: still the original declaration, not the new, same-named one.
        assertSame(originalX, ref.refersTo)
    }

    @Test
    fun testAddSourceIgnoresLocalVariableAsNewSymbol() {
        // A function-local variable introduced by addSource must never be treated as a new
        // non-local symbol -- even if its name collides with an existing unresolved reference
        // elsewhere in the graph, that reference must be left untouched.
        val topLevel =
            Files.createTempDirectory("cpg-incremental-update-local-var-test").toFile().apply {
                deleteOnExit()
            }
        val callerFile = tempSource(topLevel, "caller.stale", "refunresolved:x")

        val config =
            TranslationConfiguration.builder()
                .topLevel(topLevel)
                .sourceLocations(callerFile)
                .registerLanguage<StaleStubTestLanguage>()
                .defaultPasses()
                .disableCleanup()
                .build()

        val manager = TranslationManager.builder().config(config).build()
        val result = manager.analyze().get()
        val component = result.components.single()

        val ref = result.allChildren<Reference>().single { it.name.localName == "x" }
        assertNull(ref.refersTo)

        val second = tempSource(topLevel, "local.stale", "localvar:helper:x")
        val tu = manager.addSource(result, component, second)
        assertNotNull(tu)

        assertNull(
            ref.refersTo,
            "A function-local variable must not be treated as a new non-local symbol",
        )
    }

    @Test
    fun testAddSourceReconcilesMultipleNewSymbolsFromOneScan() {
        // A single addSource call introducing several new non-local symbols at once must resolve
        // ALL of them correctly -- exercising the batched, single-scan reconciliation with more
        // than one candidate.
        val topLevel =
            Files.createTempDirectory("cpg-incremental-update-batched-test").toFile().apply {
                deleteOnExit()
            }
        val callerA = tempSource(topLevel, "a.stale", "call:alpha")
        val callerB = tempSource(topLevel, "b.stale", "call:beta")
        val callerC = tempSource(topLevel, "c.stale", "call:gamma")

        val config =
            TranslationConfiguration.builder()
                .topLevel(topLevel)
                .sourceLocations(callerA, callerB, callerC)
                .registerLanguage<StaleStubTestLanguage>()
                .defaultPasses()
                .disableCleanup()
                .build()

        val manager = TranslationManager.builder().config(config).build()
        val result = manager.analyze().get()
        val component = result.components.single()

        val alphaCall = result.calls.single { it.name.localName == "alpha" }
        val betaCall = result.calls.single { it.name.localName == "beta" }
        val gammaCall = result.calls.single { it.name.localName == "gamma" }
        val alphaStub = alphaCall.invokes.single()
        val betaStub = betaCall.invokes.single()
        val gammaStub = gammaCall.invokes.single()
        assertTrue(alphaStub.isInferred && betaStub.isInferred && gammaStub.isInferred)

        val second = tempSource(topLevel, "defs.stale", "multidefine:alpha,beta,gamma")
        val tu = manager.addSource(result, component, second)
        assertNotNull(tu)

        val realFunctions =
            tu.declarations.filterIsInstance<Function>().associateBy { it.name.localName }
        assertEquals(setOf("alpha", "beta", "gamma"), realFunctions.keys)

        assertEquals(listOf(realFunctions["alpha"]), alphaCall.invokes)
        assertEquals(listOf(realFunctions["beta"]), betaCall.invokes)
        assertEquals(listOf(realFunctions["gamma"]), gammaCall.invokes)

        assertTrue(alphaStub.calledBy.isEmpty())
        assertTrue(betaStub.calledBy.isEmpty())
        assertTrue(gammaStub.calledBy.isEmpty())
    }

    @Test
    fun testAddSourceDoesNotLinkUnrelatedFreeFunctionToMemberCall() {
        // Regression test: reachability for an EXPLICIT member call (`p.render()`) must be decided
        // via the receiver's static type -> its RecordDeclaration, not via a lexical scope-chain
        // walk from the call site. A lexical walk would incorrectly find an unrelated, same-named,
        // same-signature top-level free function (since `main` and the free function share the same
        // top-level/global scope), even though real SymbolResolver would never conflate a member
        // call with a free function.
        val topLevel =
            Files.createTempDirectory("cpg-incremental-update-membercall-unrelated-test")
                .toFile()
                .apply { deleteOnExit() }
        val callerFile = tempSource(topLevel, "caller.stale", "membercall:Widget:render")

        val config =
            TranslationConfiguration.builder()
                .topLevel(topLevel)
                .sourceLocations(callerFile)
                .registerLanguage<StaleStubTestLanguage>()
                .defaultPasses()
                .disableCleanup()
                .build()

        val manager = TranslationManager.builder().config(config).build()
        val result = manager.analyze().get()
        val component = result.components.single()

        val renderCall = result.calls.single { it.name.localName == "render" }
        assertTrue(renderCall is MemberCall, "Expected an explicit-receiver MemberCall")
        val stub = renderCall.invokes.singleOrNull()
        assertNotNull(
            stub,
            "Expected the unresolved member call to 'render' to create an inferred stub",
        )
        assertTrue(stub.isInferred)
        assertTrue(stub is Method)

        val widget = result.records.single { it.name.localName == "Widget" }
        assertTrue(widget.methods.contains(stub))

        // Add an UNRELATED, top-level free function that shares the name and a compatible
        // (zero-arg) signature, but is not a member of Widget at all.
        val second = tempSource(topLevel, "unrelated.stale", "definezeroarg:render")
        val tu = manager.addSource(result, component, second)
        assertNotNull(tu)
        val freeRender = tu.declarations.filterIsInstance<Function>().single()
        assertFalse(freeRender.isInferred)

        // The MemberCall must NOT have linked to the unrelated free function, and the inferred
        // stub must still be the sole (unresolved-but-inferred) candidate.
        assertFalse(renderCall.invokes.contains(freeRender))
        assertEquals(listOf<Function>(stub), renderCall.invokes)
        assertTrue(widget.methods.contains(stub))
    }

    @Test
    fun testAddSourceResolvesMemberCallViaReceiverTypeRegardlessOfLexicalLocation() {
        // Positive counterpart of the test above: once the REAL method is added to the receiver's
        // actual record, the explicit member call must resolve correctly via the receiver-type-
        // based reachability check, even though the caller (`main`) is not lexically inside Widget
        // at all.
        val topLevel =
            Files.createTempDirectory("cpg-incremental-update-membercall-resolved-test")
                .toFile()
                .apply { deleteOnExit() }
        val callerFile = tempSource(topLevel, "caller.stale", "membercall:Widget:render")

        val config =
            TranslationConfiguration.builder()
                .topLevel(topLevel)
                .sourceLocations(callerFile)
                .registerLanguage<StaleStubTestLanguage>()
                .defaultPasses()
                .disableCleanup()
                .build()

        val manager = TranslationManager.builder().config(config).build()
        val result = manager.analyze().get()
        val component = result.components.single()

        val renderCall = result.calls.single { it.name.localName == "render" }
        val stub = renderCall.invokes.singleOrNull()
        assertNotNull(
            stub,
            "Expected the unresolved member call to 'render' to create an inferred stub",
        )
        assertTrue(stub.isInferred)

        val second = tempSource(topLevel, "widget_render.stale", "definemethod:Widget:render")
        val tu = manager.addSource(result, component, second)
        assertNotNull(tu)
        val realRender = tu.declarations.filterIsInstance<Function>().single()
        assertFalse(realRender.isInferred)

        assertEquals(listOf(realRender), renderCall.invokes)
        assertTrue(stub.calledBy.isEmpty())
    }

    @Test
    fun testAddSourcePreservesAmbiguousConstructionCandidates() {
        // Regression test: Construction.constructor's setter forwards a non-null assignment to
        // `invokes` WHOLESALE (`invokes = mutableListOf(value)`). If reconcileCalls resynced it
        // unconditionally, a SECOND addSource call that adds another, equally-viable real
        // constructor would silently collapse the (now genuinely ambiguous) `invokes` back down to
        // a single entry instead of preserving both candidates.
        val topLevel =
            Files.createTempDirectory("cpg-incremental-update-ambiguous-construction-test")
                .toFile()
                .apply { deleteOnExit() }
        val callerFile = tempSource(topLevel, "caller.stale", "classnew:Foo")

        val config =
            TranslationConfiguration.builder()
                .topLevel(topLevel)
                .sourceLocations(callerFile)
                .registerLanguage<StaleStubTestLanguage>()
                .defaultPasses()
                .disableCleanup()
                .build()

        val manager = TranslationManager.builder().config(config).build()
        val result = manager.analyze().get()
        val component = result.components.single()

        val record = result.records.single { it.name.localName == "Foo" }
        val main = result.functions.single { it.name.localName == "main" }
        val construction = main.body.allChildren<Construction>().single()

        val stub = record.constructors.singleOrNull()
        assertNotNull(stub, "Expected `new Foo()` to create an inferred Constructor stub")
        assertTrue(stub.isInferred)

        // First addSource: adds the first real constructor. This is the already-covered
        // (0 -> 1) safe-resync case -- the inferred stub is replaced by ctor1 alone.
        val first = tempSource(topLevel, "ctor1.stale", "defineconstructor:Foo")
        val firstTu = manager.addSource(result, component, first)
        assertNotNull(firstTu)
        val ctor1 = firstTu.declarations.filterIsInstance<Constructor>().single()
        assertFalse(ctor1.isInferred)
        assertEquals(listOf<Function>(ctor1), construction.invokes)
        assertSame(ctor1, construction.constructor)

        // Second addSource: adds a SECOND, distinct, equally-viable real constructor. Since ctor1
        // is real (not inferred), it must be kept, and ctor2 simply added alongside it -- this is
        // now a genuinely ambiguous Construction with two real candidates.
        val second = tempSource(topLevel, "ctor2.stale", "defineconstructor2:Foo")
        val secondTu = manager.addSource(result, component, second)
        assertNotNull(secondTu)
        val ctor2 = secondTu.declarations.filterIsInstance<Constructor>().single()
        assertFalse(ctor2.isInferred)
        assertNotSame(ctor1, ctor2)

        // Both candidates must be present -- `constructor`'s destructive resync must have been
        // skipped once `invokes` held more than one candidate.
        assertEquals(setOf<Function>(ctor1, ctor2), construction.invokes.toSet())
    }

    @Test
    fun testAddSourceResolvesMemberCallAgainstMethodAddedToBaseClass() {
        // Regression test: isReachableFrom's member-access branch must walk the receiver's
        // ancestor chain (like SymbolResolver.resolveMemberByName does), not just check for an
        // exact declaring-type match. A pre-existing call `p.methodName()` where `p`'s static type
        // is Derived, and a new method later added to Derived's BASE class, must still resolve --
        // an exact-match-only check would wrongly conclude the base method is unreachable from a
        // Derived-typed receiver.
        val topLevel =
            Files.createTempDirectory("cpg-incremental-update-inherited-membercall-test")
                .toFile()
                .apply { deleteOnExit() }
        val callerFile =
            tempSource(topLevel, "caller.stale", "membercallderived:Base:Derived:greet")

        val config =
            TranslationConfiguration.builder()
                .topLevel(topLevel)
                .sourceLocations(callerFile)
                .registerLanguage<StaleStubTestLanguage>()
                .defaultPasses()
                .disableCleanup()
                .build()

        val manager = TranslationManager.builder().config(config).build()
        val result = manager.analyze().get()
        val component = result.components.single()

        val greetCall = result.calls.single { it.name.localName == "greet" }
        assertTrue(greetCall is MemberCall, "Expected an explicit-receiver MemberCall")
        val stub = greetCall.invokes.singleOrNull()
        assertNotNull(
            stub,
            "Expected the unresolved member call to 'greet' to create an inferred stub",
        )
        assertTrue(stub.isInferred)

        // Add the real method to the BASE class, not Derived (whose static type is what the
        // receiver `p` actually has).
        val second = tempSource(topLevel, "base_greet.stale", "definemethod:Base:greet")
        val tu = manager.addSource(result, component, second)
        assertNotNull(tu)
        val realGreet = tu.declarations.filterIsInstance<Function>().single()
        assertFalse(realGreet.isInferred)

        // The call must resolve against the inherited method, and the inferred stub must be gone.
        assertEquals(listOf(realGreet), greetCall.invokes)
        assertTrue(stub.calledBy.isEmpty())
    }

    @Test
    fun testAddSourceAttachesCallDfgEdgesImmediatelyWithoutRunDirtyPasses() {
        // Behavioral change under test: reconcileCalls no longer relies on marking DFGPass dirty
        // and
        // waiting for a later runDirtyPasses -- it attaches the arg->param and
        // "invoked function flows into the call" DFG edges directly, so they must already be
        // present as soon as addSource returns.
        val topLevel =
            Files.createTempDirectory("cpg-incremental-update-immediate-call-dfg-test")
                .toFile()
                .apply { deleteOnExit() }
        val callerFile = tempSource(topLevel, "caller.stale", "call:foo")

        val config =
            TranslationConfiguration.builder()
                .topLevel(topLevel)
                .sourceLocations(callerFile)
                .registerLanguage<StaleStubTestLanguage>()
                .minimalPasses()
                .disableCleanup()
                .build()

        val manager = TranslationManager.builder().config(config).build()
        val result = manager.analyze().get()
        val component = result.components.single()

        val fooCall = result.calls.single { it.name.localName == "foo" }
        val stub = fooCall.invokes.singleOrNull()
        assertNotNull(stub, "Expected the unresolved call to 'foo' to create an inferred stub")

        val second = tempSource(topLevel, "foo.stale", "define:foo")
        val tu = manager.addSource(result, component, second)
        assertNotNull(tu)
        val realFoo = tu.declarations.filterIsInstance<Function>().single()
        assertFalse(realFoo.isInferred)

        // No runDirtyPasses call here at all -- the edges must already be correct.
        assertEquals(listOf(realFoo), fooCall.invokes)
        assertTrue(
            fooCall.prevDFG.contains(realFoo),
            "Expected the 'invoked function flows into the call' edge to be attached immediately",
        )
        val param = realFoo.parameters.single()
        assertTrue(
            param.prevDFG.isNotEmpty(),
            "Expected the argument->parameter edge to be attached immediately",
        )
    }

    @Test
    fun testAddSourceReconciledCallDoesNotMarkDfgFamilyPassesDirty() {
        // Regression test: since the "invoked function flows into the call" edge for a reconciled
        // call is now attached directly (see the test above), the caller must never be marked dirty
        // for DFGPass specifically -- that pass is ComponentPass-granularity, so marking it dirty
        // would trigger a whole-component rerun, exactly the cost this feature avoids. Uses the
        // actual `.defaultPasses()` configuration (PointsToPass registered), which is also the
        // config under which the accepted argument->parameter-edge gap documented on
        // attachStandardDfgEdges applies -- PointsToPass/ControlFlowSensitiveDFGPass are NOT marked
        // dirty either, not even as a fallback (see that doc for why).
        val topLevel =
            Files.createTempDirectory("cpg-incremental-update-no-dfg-dirty-test").toFile().apply {
                deleteOnExit()
            }
        val callerFile = tempSource(topLevel, "caller.stale", "call:foo")

        val config =
            TranslationConfiguration.builder()
                .topLevel(topLevel)
                .sourceLocations(callerFile)
                .registerLanguage<StaleStubTestLanguage>()
                .defaultPasses()
                .disableCleanup()
                .build()

        val manager = TranslationManager.builder().config(config).build()
        val result = manager.analyze().get()
        val component = result.components.single()
        val main = result.functions.single { it.name.localName == "main" }

        val second = tempSource(topLevel, "foo.stale", "define:foo")
        val tu = manager.addSource(result, component, second)
        assertNotNull(tu)

        val dirtyForMain = result.dirtyNodes[main].orEmpty()
        assertTrue(dirtyForMain.contains(SymbolResolver::class))
        assertFalse(dirtyForMain.contains(DFGPass::class))
        assertFalse(dirtyForMain.contains(ControlFlowSensitiveDFGPass::class))
        assertFalse(dirtyForMain.contains(PointsToPass::class))
    }

    @Test
    fun testAddSourceReconciledCallInvokesEdgeCorrectEvenWithPointsToPassRegistered() {
        // Under the actual default configuration (PointsToPass registered), the argument->parameter
        // edge is a known, accepted gap (see attachStandardDfgEdges's doc) -- but the call's
        // `invokes`/"invoked function flows into the call" edge is unaffected by that gap (attached
        // unconditionally) and must still be correct immediately, without runDirtyPasses.
        val topLevel =
            Files.createTempDirectory("cpg-incremental-update-defaultpasses-invokes-test")
                .toFile()
                .apply { deleteOnExit() }
        val callerFile = tempSource(topLevel, "caller.stale", "call:foo")

        val config =
            TranslationConfiguration.builder()
                .topLevel(topLevel)
                .sourceLocations(callerFile)
                .registerLanguage<StaleStubTestLanguage>()
                .defaultPasses()
                .disableCleanup()
                .build()

        val manager = TranslationManager.builder().config(config).build()
        val result = manager.analyze().get()
        val component = result.components.single()

        val second = tempSource(topLevel, "foo.stale", "define:foo")
        val tu = manager.addSource(result, component, second)
        assertNotNull(tu)
        val realFoo = tu.declarations.filterIsInstance<Function>().single()

        val fooCall = result.calls.single { it.name.localName == "foo" }
        assertEquals(listOf(realFoo), fooCall.invokes)
        assertTrue(fooCall.prevDFG.contains(realFoo))
    }

    @Test
    fun testAddSourceAttachesReferenceDfgEdgesImmediatelyWithoutRunDirtyPasses() {
        // Reference counterpart of
        // testAddSourceAttachesCallDfgEdgesImmediatelyWithoutRunDirtyPasses:
        // once a previously-unresolved reference is repointed at a real declaration,
        // reconcileReferences must attach the read/write DFG edge directly.
        val topLevel =
            Files.createTempDirectory("cpg-incremental-update-immediate-ref-dfg-test")
                .toFile()
                .apply { deleteOnExit() }
        val callerFile = tempSource(topLevel, "caller.stale", "refunresolved:x")

        val config =
            TranslationConfiguration.builder()
                .topLevel(topLevel)
                .sourceLocations(callerFile)
                .registerLanguage<StaleStubTestLanguage>()
                .minimalPasses()
                .disableCleanup()
                .build()

        val manager = TranslationManager.builder().config(config).build()
        val result = manager.analyze().get()
        val component = result.components.single()

        val ref = result.allChildren<Reference>().single { it.name.localName == "x" }
        assertNull(ref.refersTo)
        val main = result.functions.single { it.name.localName == "main" }

        val second = tempSource(topLevel, "x.stale", "defineglobal:x")
        val tu = manager.addSource(result, component, second)
        assertNotNull(tu)
        val realX = tu.declarations.filterIsInstance<Variable>().single()

        // No runDirtyPasses call here at all.
        assertSame(realX, ref.refersTo)
        assertTrue(
            ref.prevDFG.contains(realX),
            "Expected the read edge from the resolved declaration to be attached immediately",
        )

        val dirtyForMain = result.dirtyNodes[main].orEmpty()
        assertTrue(dirtyForMain.contains(SymbolResolver::class))
        assertFalse(dirtyForMain.contains(DFGPass::class))
    }

    @Test
    fun testAddSourceDoesNotLinkPrivateBaseMethodToCallFromDerivedClass() {
        // Regression test: isAccessibleFrom must require an EXACT match for Visibility.PRIVATE
        // (not just "is the same or an ancestor of the declaring record", which is only correct for
        // Visibility.PROTECTED) -- otherwise a `private` method added to a base class would
        // incorrectly become reachable from a pre-existing call inside a subclass, just because the
        // subclass transitively inherits from the declaring record.
        val topLevel =
            Files.createTempDirectory("cpg-incremental-update-private-base-method-test")
                .toFile()
                .apply { deleteOnExit() }
        val callerFile =
            tempSource(topLevel, "caller.stale", "membercallwithinderived:Base:Derived:secret")

        val config =
            TranslationConfiguration.builder()
                .topLevel(topLevel)
                .sourceLocations(callerFile)
                .registerLanguage<StaleStubTestLanguage>()
                .defaultPasses()
                .disableCleanup()
                .build()

        val manager = TranslationManager.builder().config(config).build()
        val result = manager.analyze().get()
        val component = result.components.single()

        val secretCall = result.calls.single { it.name.localName == "secret" }
        assertTrue(secretCall is MemberCall, "Expected an explicit-receiver MemberCall")
        val stub = secretCall.invokes.singleOrNull()
        assertNotNull(
            stub,
            "Expected the unresolved member call to 'secret' to create an inferred stub",
        )
        assertTrue(stub.isInferred)

        // Add a PRIVATE method named 'secret' to the BASE class -- reachable via the ancestor chain
        // (Derived extends Base), but NOT accessible from Derived's own "caller" method, since
        // private members are only accessible from within their own declaring record.
        val second = tempSource(topLevel, "base_secret.stale", "defineprivatemethod:Base:secret")
        val tu = manager.addSource(result, component, second)
        assertNotNull(tu)
        val privateSecret = tu.declarations.filterIsInstance<Function>().single()
        assertFalse(privateSecret.isInferred)

        // The call must NOT have been linked to the inaccessible private method -- it must still be
        // the sole (unresolved-but-inferred) candidate.
        assertFalse(secretCall.invokes.contains(privateSecret))
        assertEquals(listOf<Function>(stub), secretCall.invokes)
        assertTrue(stub.calledBy.isNotEmpty())
    }
}
