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
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnit
import de.fraunhofer.aisec.cpg.graph.declarations.Variable
import de.fraunhofer.aisec.cpg.graph.expressions.*
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker
import de.fraunhofer.aisec.cpg.test.analyzeAndGetFirstTU
import java.nio.file.Path
import kotlin.test.*

class PatternTest {

    /** Parses `patterns.rs` once per test and returns its translation unit. */
    private fun parsePatterns(): TranslationUnit {
        val topLevel = Path.of("src", "test", "resources")
        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("patterns.rs").toFile()), topLevel, true) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)
        return tu
    }

    /** Returns the `Case` statements of a match's case block, in source order. */
    private fun casesIn(switch: Switch): List<Case> {
        val block = assertIs<Block>(switch.statement)
        return block.statements.filterIsInstance<Case>()
    }

    /** Unwraps the `Variable` declared by a fresh binding pattern (e.g. plain `IdentPat`). */
    private fun declaredVariable(expression: Expression?): Variable {
        val declarationStatement = assertIs<DeclarationStatement>(expression)
        return assertIs<Variable>(declarationStatement.declarations.first())
    }

    @Test
    fun testBoxPat() {
        val tu = parsePatterns()
        val function = tu.functions["example_box_pat"]
        assertNotNull(function)

        val switches = SubgraphWalker.flattenAST(function).filterIsInstance<Switch>()
        assertEquals(2, switches.size)

        // match boxed { box value => ... } -- one component holding the boxed value
        val simpleCases = casesIn(switches[0])
        assertEquals(1, simpleCases.size)
        val simpleBox = assertIs<ObjectDeconstruction>(simpleCases[0].caseExpression)
        assertEquals(1, simpleBox.components.size)
        assertEquals("value", declaredVariable(simpleBox.components[0]).name.localName)

        // match boxed_tuple { box (a, b) => ... } -- the box wraps a nested tuple pattern
        val nestedCases = casesIn(switches[1])
        assertEquals(1, nestedCases.size)
        val nestedBox = assertIs<ObjectDeconstruction>(nestedCases[0].caseExpression)
        assertEquals(1, nestedBox.components.size)
        val innerTuple = assertIs<ObjectDeconstruction>(nestedBox.components[0])
        assertEquals(2, innerTuple.components.size)
        assertEquals("a", declaredVariable(innerTuple.components[0]).name.localName)
        assertEquals("b", declaredVariable(innerTuple.components[1]).name.localName)

        // Data flow: the boxed value being matched should carry incoming DFG edges
        val selector = switches[0].selector
        assertNotNull(selector)
        assertTrue(selector.prevDFGEdges.isNotEmpty(), "Selector should have incoming DFG edges")
    }

    @Test
    fun testLiteralPat() {
        val tu = parsePatterns()
        val function = tu.functions["example_literal_pat"]
        assertNotNull(function)

        val switches = SubgraphWalker.flattenAST(function).filterIsInstance<Switch>()
        assertEquals(2, switches.size)

        // match value { 10 => ..., _ => {} }
        val intCases = casesIn(switches[0])
        assertEquals(2, intCases.size)
        val intLiteral = assertIs<Literal<*>>(intCases[0].caseExpression)
        assertEquals(10, intLiteral.value)
        assertIs<Empty>(intCases[1].caseExpression)

        // match text { "hello" => ..., _ => {} }
        val stringCases = casesIn(switches[1])
        assertEquals(2, stringCases.size)
        val stringLiteral = assertIs<Literal<*>>(stringCases[0].caseExpression)
        assertEquals("hello", stringLiteral.value)
        assertIs<Empty>(stringCases[1].caseExpression)

        // Data flow: the matched value should trace back to its literal source
        val selector = switches[0].selector
        assertNotNull(selector)
        val reachableLiterals =
            selector
                .collectAllPrevDFGPaths()
                .flatMap { it.nodes }
                .filterIsInstance<Literal<*>>()
                .map { it.value }
        assertTrue(reachableLiterals.contains(10), "Selector should trace back to literal 10")
    }

    @Test
    fun testParenPat() {
        val tu = parsePatterns()
        val function = tu.functions["example_paren_pat"]
        assertNotNull(function)

        val switches = SubgraphWalker.flattenAST(function).filterIsInstance<Switch>()
        assertEquals(2, switches.size)

        // ((a, b)) -- ParenPat is transparent, so the case expression is the tuple pattern's
        // ObjectDeconstruction directly, without any extra wrapping node for the parens
        val tupleCases = casesIn(switches[0])
        assertEquals(1, tupleCases.size)
        val tuplePattern = assertIs<ObjectDeconstruction>(tupleCases[0].caseExpression)
        assertEquals(2, tuplePattern.components.size)
        assertEquals("a", declaredVariable(tuplePattern.components[0]).name.localName)
        assertEquals("b", declaredVariable(tuplePattern.components[1]).name.localName)

        // (Some(x)) / None -- parens around a tuple-struct pattern are transparent as well
        val optionCases = casesIn(switches[1])
        assertEquals(2, optionCases.size)
        val someDeconstruction = assertIs<ObjectDeconstruction>(optionCases[0].caseExpression)
        assertEquals(1, someDeconstruction.components.size)
        assertEquals("x", declaredVariable(someDeconstruction.components[0]).name.localName)
    }

    @Test
    fun testPathPat() {
        val tu = parsePatterns()
        val function = tu.functions["example_path_pat"]
        assertNotNull(function)

        val switches = SubgraphWalker.flattenAST(function).filterIsInstance<Switch>()
        assertEquals(2, switches.size)

        // Color::Red / Color::Green / Color::Blue -- each becomes a Reference to the variant
        val colorCases = casesIn(switches[0])
        assertEquals(3, colorCases.size)
        val expectedVariants = listOf("Red", "Green", "Blue")
        colorCases.forEachIndexed { index, case ->
            val reference = assertIs<Reference>(case.caseExpression)
            assertEquals(expectedVariants[index], reference.name.localName)
        }

        // ANSWER / _ -- a Reference to the constant, and a wildcard
        val constCases = casesIn(switches[1])
        assertEquals(2, constCases.size)
        val answerReference = assertIs<Reference>(constCases[0].caseExpression)
        assertEquals("ANSWER", answerReference.name.localName)
        assertIs<Empty>(constCases[1].caseExpression)

        // Data flow: the matched value should trace back to literal 42
        val selector = switches[1].selector
        assertNotNull(selector)
        val reachableLiterals =
            selector
                .collectAllPrevDFGPaths()
                .flatMap { it.nodes }
                .filterIsInstance<Literal<*>>()
                .map { it.value }
        assertTrue(reachableLiterals.contains(42), "Selector should trace back to literal 42")
    }

    @Test
    fun testRecordPat() {
        val tu = parsePatterns()
        val function = tu.functions["example_record_pat"]
        assertNotNull(function)

        val switches = SubgraphWalker.flattenAST(function).filterIsInstance<Switch>()
        assertEquals(2, switches.size)

        // Point { x, y } -- a component per named field
        val pointCases = casesIn(switches[0])
        assertEquals(1, pointCases.size)
        val pointRecord = assertIs<ObjectDeconstruction>(pointCases[0].caseExpression)
        assertEquals("Point", pointRecord.type.name.localName)
        assertEquals(2, pointRecord.components.size)
        val xField = assertIs<NamedDeconstruction>(pointRecord.components[0])
        val yField = assertIs<NamedDeconstruction>(pointRecord.components[1])
        assertEquals("x", declaredVariable(xField.value).name.localName)
        assertEquals("y", declaredVariable(yField.value).name.localName)

        // Person { name, .. } -- only the named field becomes a component, ".." doesn't
        val personCases = casesIn(switches[1])
        assertEquals(1, personCases.size)
        val personRecord = assertIs<ObjectDeconstruction>(personCases[0].caseExpression)
        assertEquals("Person", personRecord.type.name.localName)
        assertEquals(1, personRecord.components.size)
        val nameField = assertIs<NamedDeconstruction>(personRecord.components[0])
        assertEquals("name", declaredVariable(nameField.value).name.localName)

        // Data flow: the matched struct value should carry incoming DFG edges
        val selector = switches[0].selector
        assertNotNull(selector)
        assertTrue(selector.prevDFGEdges.isNotEmpty(), "Selector should have incoming DFG edges")
    }

    @Test
    fun testRefPat() {
        val tu = parsePatterns()
        val function = tu.functions["example_ref_pat"]
        assertNotNull(function)

        val switches = SubgraphWalker.flattenAST(function).filterIsInstance<Switch>()
        assertEquals(2, switches.size)

        // &x -- RefPat wraps the plain binding in an ObjectDeconstruction
        val simpleCases = casesIn(switches[0])
        assertEquals(1, simpleCases.size)
        val simpleRef = assertIs<ObjectDeconstruction>(simpleCases[0].caseExpression)
        assertEquals(1, simpleRef.components.size)
        assertEquals("x", declaredVariable(simpleRef.components[0]).name.localName)

        // &(a, b) -- RefPat wrapping a nested tuple pattern
        val tupleCases = casesIn(switches[1])
        assertEquals(1, tupleCases.size)
        val outerRef = assertIs<ObjectDeconstruction>(tupleCases[0].caseExpression)
        assertEquals(1, outerRef.components.size)
        val innerTuple = assertIs<ObjectDeconstruction>(outerRef.components[0])
        assertEquals(2, innerTuple.components.size)
        assertEquals("a", declaredVariable(innerTuple.components[0]).name.localName)
        assertEquals("b", declaredVariable(innerTuple.components[1]).name.localName)

        // Data flow: the referenced value should carry incoming DFG through the "&"
        val selector = switches[0].selector
        assertNotNull(selector)
        assertTrue(selector.prevDFGEdges.isNotEmpty(), "Selector should have incoming DFG edges")
    }

    @Test
    fun testRestPat() {
        val tu = parsePatterns()
        val function = tu.functions["example_rest_pat"]
        assertNotNull(function)

        val switches = SubgraphWalker.flattenAST(function).filterIsInstance<Switch>()
        assertEquals(2, switches.size)

        // (first, ..) -- the binding, plus an Empty component standing in for the rest
        val tupleCases = casesIn(switches[0])
        assertEquals(1, tupleCases.size)
        val tuplePattern = assertIs<ObjectDeconstruction>(tupleCases[0].caseExpression)
        assertEquals(2, tuplePattern.components.size)
        assertEquals("first", declaredVariable(tuplePattern.components[0]).name.localName)
        assertIs<Empty>(tuplePattern.components[1])

        // [head, ..] -- the same shape for a slice pattern
        val sliceCases = casesIn(switches[1])
        assertEquals(1, sliceCases.size)
        val slicePattern = assertIs<ObjectDeconstruction>(sliceCases[0].caseExpression)
        assertEquals(2, slicePattern.components.size)
        assertEquals("head", declaredVariable(slicePattern.components[0]).name.localName)
        assertIs<Empty>(slicePattern.components[1])
    }

    @Test
    fun testSlicePat() {
        val tu = parsePatterns()
        val function = tu.functions["example_slice_pat"]
        assertNotNull(function)

        val switches = SubgraphWalker.flattenAST(function).filterIsInstance<Switch>()
        assertEquals(3, switches.size)

        // [a, b, c, d] -- fixed length, one component per element
        val fixedCases = casesIn(switches[0])
        assertEquals(1, fixedCases.size)
        val fixedPattern = assertIs<ObjectDeconstruction>(fixedCases[0].caseExpression)
        assertEquals(4, fixedPattern.components.size)
        listOf("a", "b", "c", "d").forEachIndexed { index, name ->
            assertEquals(name, declaredVariable(fixedPattern.components[index]).name.localName)
        }

        // [first, middle @ .., last] -- middle binds the rest via an `@` sub-pattern
        val rangeCases = casesIn(switches[1])
        assertEquals(1, rangeCases.size)
        val rangePattern = assertIs<ObjectDeconstruction>(rangeCases[0].caseExpression)
        assertEquals(3, rangePattern.components.size)
        assertEquals("first", declaredVariable(rangePattern.components[0]).name.localName)
        val middleVar = declaredVariable(rangePattern.components[1])
        assertEquals("middle", middleVar.name.localName)
        assertIs<Empty>(middleVar.initializer)
        assertEquals("last", declaredVariable(rangePattern.components[2]).name.localName)

        // [] -- an empty slice pattern has no components
        val emptyCases = casesIn(switches[2])
        assertEquals(2, emptyCases.size)
        val emptyPattern = assertIs<ObjectDeconstruction>(emptyCases[0].caseExpression)
        assertTrue(
            emptyPattern.components.isEmpty(),
            "Empty slice pattern should have no components",
        )
        assertIs<Empty>(emptyCases[1].caseExpression)
    }

    @Test
    fun testRecordPatField() {
        val tu = parsePatterns()
        val function = tu.functions["example_record_pat_field"]
        assertNotNull(function)

        val switches = SubgraphWalker.flattenAST(function).filterIsInstance<Switch>()
        assertEquals(2, switches.size)

        // Point { x, y } -- shorthand: field name and binding name are identical. The field has
        // no explicit RecordPatField name, so PatternHandler falls back to the bound
        // DeclarationStatement's own name, which is empty -- so the NamedDeconstruction ends up
        // with an empty name here.
        val shorthandCases = casesIn(switches[0])
        assertEquals(1, shorthandCases.size)
        val shorthandRecord = assertIs<ObjectDeconstruction>(shorthandCases[0].caseExpression)
        assertEquals(2, shorthandRecord.components.size)

        val xField = assertIs<NamedDeconstruction>(shorthandRecord.components[0])
        assertEquals("", xField.name.localName)
        assertEquals("x", declaredVariable(xField.value).name.localName)

        val yField = assertIs<NamedDeconstruction>(shorthandRecord.components[1])
        assertEquals("", yField.name.localName)
        assertEquals("y", declaredVariable(yField.value).name.localName)

        // Point { x: renamed_x, y: renamed_y } -- explicit rename: the field keeps its own name,
        // while the bound variable carries the renamed identifier
        val renameCases = casesIn(switches[1])
        assertEquals(1, renameCases.size)
        val renameRecord = assertIs<ObjectDeconstruction>(renameCases[0].caseExpression)
        assertEquals(2, renameRecord.components.size)

        val renamedXField = assertIs<NamedDeconstruction>(renameRecord.components[0])
        assertEquals("x", renamedXField.name.localName)
        assertEquals("renamed_x", declaredVariable(renamedXField.value).name.localName)

        val renamedYField = assertIs<NamedDeconstruction>(renameRecord.components[1])
        assertEquals("y", renamedYField.name.localName)
        assertEquals("renamed_y", declaredVariable(renamedYField.value).name.localName)
    }
}
