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

import de.fraunhofer.aisec.cpg.frontends.Language
import de.fraunhofer.aisec.cpg.frontends.SupportsNewParse
import de.fraunhofer.aisec.cpg.frontends.TestLanguage
import de.fraunhofer.aisec.cpg.frontends.TestLanguageFrontend
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.Constructor
import de.fraunhofer.aisec.cpg.graph.declarations.Function
import de.fraunhofer.aisec.cpg.graph.declarations.Method
import de.fraunhofer.aisec.cpg.graph.declarations.Record
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnit
import de.fraunhofer.aisec.cpg.graph.expressions.Construction
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.graph.unknownType
import de.fraunhofer.aisec.cpg.passes.SymbolResolver
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
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
class StaleStubTestLanguage : TestLanguage() {
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

        // The stale invokes edge (and the DFG edges that hung off it) must be gone.
        assertTrue(fooCall.invokes.isEmpty())
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

        // The stale invokes edge must be gone, and the now-orphaned stub detached from the record.
        assertTrue(helperCall.invokes.isEmpty())
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
        // forwards one-directionally to `invokes` -- clearing `construction.invokes` alone must
        // not leave `construction.constructor` still dangling at the now-detached stub.
        assertTrue(construction.invokes.isEmpty())
        assertNull(
            construction.constructor,
            "Expected Construction.constructor to be reset alongside the cleared invokes edge",
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
        assertTrue(fooCall.invokes.isEmpty())
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

        // The stale invokes edge is gone...
        assertTrue(fooCall.invokes.isEmpty())
        // ...and the enclosing TranslationUnit (not a Function, since there is none) must have
        // been marked dirty for SymbolResolver, so a later runDirtyPasses actually revisits it.
        assertTrue(result.dirtyNodes[callerTu]?.contains(SymbolResolver::class) == true)

        manager.runDirtyPasses(result)
        assertEquals(listOf(realFoo), fooCall.invokes)
    }
}
