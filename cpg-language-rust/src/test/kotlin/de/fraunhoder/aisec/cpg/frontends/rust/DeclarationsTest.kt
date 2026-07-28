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
package de.fraunhoder.aisec.cpg.frontends.rust

import de.fraunhofer.aisec.cpg.frontends.rust.RustLanguage
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.Typedef
import de.fraunhofer.aisec.cpg.graph.expressions.*
import de.fraunhofer.aisec.cpg.graph.types.ObjectType
import de.fraunhofer.aisec.cpg.graph.types.UnknownType
import de.fraunhofer.aisec.cpg.test.analyze
import de.fraunhofer.aisec.cpg.test.analyzeAndGetFirstTU
import java.nio.file.Path
import kotlin.test.*

class DeclarationsTest {

    @Test
    fun testEnumExpression() {
        val topLevel = Path.of("src", "test", "resources")
        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("enums.rs").toFile()), topLevel, true) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)

        // ------------------------------------------------------------------
        // Declaration structure: enum Expr { Call(String, Vec<Expr>), Record { name, pars },
        // Number(i64) } -- handleEnum turns the enum into a Record of kind "enum" and each
        // variant into a nested Record of kind "variant" that implements the enum's type.
        // ------------------------------------------------------------------
        val expr = tu.records["Expr"]
        assertNotNull(expr, "Expr enum should exist as a Record")
        assertEquals("enum", expr.kind)
        assertEquals(3, expr.records.size, "Expr should have 3 variants")

        val call = expr.records["Call"]
        assertNotNull(call, "Call variant should exist")
        assertEquals("variant", call.kind)
        assertEquals(2, call.fields.size, "Call is a tuple variant with 2 unnamed fields")
        assertEquals("0", call.fields[0].name.localName)
        assertEquals("1", call.fields[1].name.localName)
        assertEquals("String", call.fields[0].type.name.localName)
        assertTrue(
            call.implementedInterfaces.any { it.name.localName == "Expr" },
            "Call variant should implement/extend the Expr enum type",
        )

        val record = expr.records["Record"]
        assertNotNull(record, "Record variant should exist")
        assertEquals("variant", record.kind)
        assertEquals(2, record.fields.size, "Record is a record variant with 2 named fields")
        assertEquals("name", record.fields[0].name.localName)
        assertEquals("pars", record.fields[1].name.localName)
        assertEquals("String", record.fields[0].type.name.localName)
        assertTrue(
            record.implementedInterfaces.any { it.name.localName == "Expr" },
            "Record variant should implement/extend the Expr enum type",
        )

        val number = expr.records["Number"]
        assertNotNull(number, "Number variant should exist")
        assertEquals("variant", number.kind)
        assertEquals(1, number.fields.size, "Number is a tuple variant with 1 unnamed field")
        assertEquals("0", number.fields[0].name.localName)
        assertEquals("i64", number.fields[0].type.name.localName)
        assertTrue(
            number.implementedInterfaces.any { it.name.localName == "Expr" },
            "Number variant should implement/extend the Expr enum type",
        )

        // None of the variants declare an explicit discriminant, so no "discriminant" field
        // should have been synthesized anywhere
        assertTrue(
            expr.records.flatMap { it.fields }.none { it.name.localName == "discriminant" },
            "No variant should have a discriminant field since none use explicit discriminants",
        )

        // ------------------------------------------------------------------
        // Instantiations: Expr::Number(..), Expr::Call(..) and Expr::Record { .. } should all be
        // resolved into Construction expressions whose type points back to the corresponding
        // variant Record (via ObjectType.recordDeclaration), not just a plain unresolved Call.
        // ------------------------------------------------------------------
        val main = tu.functions["main"]
        assertNotNull(main)

        val numberConstructions =
            main.allChildren<Construction>().filter { it.type.name.localName == "Number" }
        assertEquals(4, numberConstructions.size, "Expected 4 Expr::Number(..) instantiations")
        numberConstructions.forEach { construction ->
            val type = assertIs<ObjectType>(construction.type)
            assertEquals(
                number,
                type.recordDeclaration,
                "Construction type should resolve back to the Number variant record",
            )
            assertIs<Reference>(construction.callee)
        }

        val tupleExprVar = main.variables["tuple_expr"]
        assertNotNull(tupleExprVar)
        val tupleConstruction = assertIs<Construction>(tupleExprVar.assignments.first().value)
        val tupleType = assertIs<ObjectType>(tupleConstruction.type)
        assertEquals("Call", tupleType.name.localName)
        assertEquals(
            call,
            tupleType.recordDeclaration,
            "Construction type should resolve back to the Call variant record",
        )
        assertEquals(2, tupleConstruction.arguments.size)

        val recordExprVar = main.variables["record_expr"]
        assertNotNull(recordExprVar)
        val recordConstruction = assertIs<Construction>(recordExprVar.assignments.first().value)
        val recordType = assertIs<ObjectType>(recordConstruction.type)
        assertEquals("Record", recordType.name.localName)
        assertEquals(
            record,
            recordType.recordDeclaration,
            "Construction type should resolve back to the Record variant record",
        )

        // ------------------------------------------------------------------
        // Pattern side: match arms deconstruct the same variants. Both handleTupleStructPat and
        // handleRecordPat resolve the path, so both deconstructions' types resolve back to the
        // matched variant record.
        // ------------------------------------------------------------------
        val switches = main.allChildren<Switch>()
        assertEquals(2, switches.size)

        val tupleCase =
            assertIs<Block>(switches[0].statement).statements.filterIsInstance<Case>().first()
        val tupleDeconstruction = assertIs<ObjectDeconstruction>(tupleCase.caseExpression)
        assertEquals("Call", tupleDeconstruction.type.name.localName)
        assertEquals(
            call,
            assertIs<ObjectType>(tupleDeconstruction.type).recordDeclaration,
            "Tuple-struct pattern's deconstruction type should resolve back to the Call variant record",
        )
        assertEquals(2, tupleDeconstruction.components.size)

        val recordCase =
            assertIs<Block>(switches[1].statement).statements.filterIsInstance<Case>().first()
        val recordDeconstruction = assertIs<ObjectDeconstruction>(recordCase.caseExpression)
        assertEquals("Record", recordDeconstruction.type.name.localName)
        assertEquals(
            record,
            assertIs<ObjectType>(recordDeconstruction.type).recordDeclaration,
            "Record pattern's deconstruction type should resolve back to the Record variant record",
        )
        assertEquals(2, recordDeconstruction.components.size)
    }

    @Test
    fun testTypeAlias() {
        val topLevel = Path.of("src", "test", "resources")
        val result =
            analyze(listOf(topLevel.resolve("trait_imp.rs").toFile()), topLevel, true) {
                it.registerLanguage<RustLanguage>()
            }
        val tu = result.components.flatMap { it.translationUnits }.first()
        assertNotNull(tu)
        val scopeManager = result.finalCtx.scopeManager

        // trait Example { type Alias; } -- an abstract (no underlying type) alias.
        val example = tu.records["Example"]
        assertNotNull(example, "Example trait should exist as a Record")
        assertEquals("trait", example.kind)

        val exampleScope = scopeManager.lookupScope(example)
        assertNotNull(exampleScope, "Example should have its own scope")

        val abstractEntry =
            exampleScope.typedefs.entries.firstOrNull { it.key.localName == "Alias" }
        assertNotNull(
            abstractEntry,
            "Trait's abstract type alias should be registered directly in Example's own scope",
        )
        assertIs<UnknownType>(
            abstractEntry.value.type,
            "Trait's alias has no underlying type, so it resolves to UnknownType",
        )

        // impl Example for MyType { type Alias = u64; } -- a concrete alias.
        val myType = tu.records["MyType"]
        assertNotNull(myType, "MyType struct should exist as a Record")

        val myTypeScope = scopeManager.lookupScope(myType)
        assertNotNull(myTypeScope, "MyType should have its own scope")
        assertNotEquals(exampleScope, myTypeScope, "MyType and Example should have distinct scopes")

        val concreteEntry = myTypeScope.typedefs.entries.firstOrNull { it.key.localName == "Alias" }
        assertNotNull(
            concreteEntry,
            "Impl's concrete type alias should be registered directly in MyType's own scope",
        )
        val concreteAlias = concreteEntry.value.type
        assertEquals(
            "u64",
            concreteAlias.name.localName,
            "Alias should resolve to the underlying u64 type",
        )

        // Unlike Record, Extension's declaration list is a generic bucket that accepts any
        // Declaration, so the impl's typedef *is* reachable through normal AST traversal.
        val typedefs = tu.allChildren<Typedef>()
        assertEquals(1, typedefs.size, "Only the impl's typedef is attached to the AST")
        val aliasDeclaration = typedefs.first()
        assertEquals("u64", aliasDeclaration.type.name.localName)
        assertEquals(
            concreteAlias,
            aliasDeclaration.type,
            "The scope-registered type should be the same type the Typedef node itself carries",
        )
    }
}
