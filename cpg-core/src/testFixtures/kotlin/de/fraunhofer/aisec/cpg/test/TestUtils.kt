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
package de.fraunhofer.aisec.cpg.test

import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.TranslationManager
import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.frontends.CompilationDatabase
import de.fraunhofer.aisec.cpg.graph.ContextProvider
import de.fraunhofer.aisec.cpg.graph.LanguageProvider
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.ast.declarations.Declaration
import de.fraunhofer.aisec.cpg.graph.ast.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.ast.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.graph.ast.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.graph.ast.statements.expressions.Expression
import de.fraunhofer.aisec.cpg.graph.ast.statements.expressions.Literal
import de.fraunhofer.aisec.cpg.graph.ast.statements.expressions.MemberExpression
import de.fraunhofer.aisec.cpg.graph.ast.statements.expressions.Reference
import de.fraunhofer.aisec.cpg.graph.get
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.test.TestUtils.ENFORCE_MEMBER_EXPRESSION
import de.fraunhofer.aisec.cpg.test.TestUtils.ENFORCE_REFERENCES
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.function.Consumer
import java.util.function.Predicate
import java.util.stream.Collectors
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.test.*

object TestUtils {

    /**
     * Currently when variables or fields are used they can be stored in the expression itself, in
     * the future, a reference as usage indicator pointing to the used ValueDeclaration is planed.
     */
    var ENFORCE_REFERENCES = false

    /**
     * Currently there is no unified enforced structure when using fields, this field is used set
     * whether the tests enforce the presence of a member expression
     */
    var ENFORCE_MEMBER_EXPRESSION = false
}

fun <S : Node?> findByUniquePredicate(nodes: Collection<S>, predicate: Predicate<S>): S {
    val results = nodes.filter { predicate.test(it) }

    assertEquals(
        1,
        results.size,
        "Expected exactly one node matching the predicate: ${results.joinToString(",") { it.toString() }}",
    )

    val node = results.firstOrNull()
    assertNotNull(node)

    return node
}

fun <S : Node> findByUniqueName(nodes: Collection<S>, name: String): S {
    return findByUniquePredicate(nodes) { m: S -> m.name.lastPartsMatch(name) }
}

fun <S : Node> findByName(nodes: Collection<S>, name: String): Collection<S> {
    return nodes.filter { m: S -> m.name.lastPartsMatch(name) }
}

/**
 * Like [TestUtils.analyze], but for all files in a directory tree having a specific file extension
 *
 * @param fileExtension All files found in the directory must end on this String. An empty string
 *   matches all files
 * @param topLevel The directory to traverse while looking for files to parse
 * @param usePasses Whether the analysis should run passes after the initial phase
 * @param configModifier An optional modifier for the config
 * @return A list of [TranslationUnitDeclaration] nodes, representing the CPG roots
 * @throws Exception Any exception thrown during the parsing process
 */
@JvmOverloads
@Throws(Exception::class)
fun analyze(
    fileExtension: String?,
    topLevel: Path,
    usePasses: Boolean,
    configModifier: Consumer<TranslationConfiguration.Builder>? = null,
): TranslationResult {
    val files =
        Files.walk(topLevel, Int.MAX_VALUE)
            .map { it.toFile() }
            .filter { it.isFile && (fileExtension == null || it.name.endsWith(fileExtension)) }
            .sorted()
            .collect(Collectors.toList())
    return analyze(files, topLevel, usePasses, configModifier)
}

/**
 * Default way of parsing a list of files into a full CPG. All default passes are applied
 *
 * @param topLevel The directory to traverse while looking for files to parse
 * @param usePasses Whether the analysis should run passes after the initial phase
 * @param configModifier An optional modifier for the config
 * @return A list of [TranslationUnitDeclaration] nodes, representing the CPG roots
 * @throws Exception Any exception thrown during the parsing process
 */
@JvmOverloads
@Throws(Exception::class)
fun analyze(
    files: List<File>,
    topLevel: Path,
    usePasses: Boolean,
    configModifier: Consumer<TranslationConfiguration.Builder>? = null,
): TranslationResult {
    val builder =
        TranslationConfiguration.builder()
            .sourceLocations(files)
            .topLevel(topLevel.toFile())
            .loadIncludes(true)
            .disableCleanup()
            .debugParser(true)
            .failOnError(true)
            .useParallelFrontends(true)
    if (usePasses) {
        builder.defaultPasses()
    }
    configModifier?.accept(builder)
    val config = builder.build()
    val analyzer = TranslationManager.builder().config(config).build()
    return analyzer.analyze().get()
}

/**
 * Default way of parsing a list of files into a full CPG. All default passes are applied
 *
 * @param builder A [TranslationConfiguration.Builder] which contains the configuration
 * @return A list of [TranslationUnitDeclaration] nodes, representing the CPG roots
 * @throws Exception Any exception thrown during the parsing process
 */
@Throws(Exception::class)
fun analyzeWithBuilder(
    builder: TranslationConfiguration.Builder
): List<TranslationUnitDeclaration> {
    val config = builder.build()
    val analyzer = TranslationManager.builder().config(config).build()
    val result = analyzer.analyze().get()

    return result.components["application"]?.translationUnits ?: listOf()
}

@JvmOverloads
@Throws(Exception::class)
fun analyzeAndGetFirstTU(
    files: List<File>,
    topLevel: Path,
    usePasses: Boolean,
    configModifier: Consumer<TranslationConfiguration.Builder>? = null,
): TranslationUnitDeclaration {
    val result = analyze(files, topLevel, usePasses, configModifier)
    return result.components.flatMap { it.translationUnits }.first()
}

@Throws(Exception::class)
fun analyzeWithCompilationDatabase(
    jsonCompilationDatabase: File,
    usePasses: Boolean,
    filterComponents: List<String>? = null,
    configModifier: Consumer<TranslationConfiguration.Builder>? = null,
): TranslationResult {
    val top = jsonCompilationDatabase.parentFile.toPath().toAbsolutePath()
    return analyze(listOf(), top, usePasses) {
        val db = CompilationDatabase.fromFile(jsonCompilationDatabase, filterComponents)
        if (db.isNotEmpty()) {
            it.useCompilationDatabase(db)
            @Suppress("UNCHECKED_CAST")
            it.softwareComponents(db.components as MutableMap<String, List<File>>)
            // We need to set the top level for all components as well, since the compilation
            // database might
            // have relative paths based on our "top" location
            it.topLevels(db.components.map { Pair(it.key, top.toFile()) }.toMap())
            configModifier?.accept(it)
        }
        configModifier?.accept(it)
    }
}

/**
 * Compare the given parameter `toCompare` to the start- or end-line of the given node. If the node
 * has no location `false` is returned. `startLine` is used to specify if the start-line or end-line
 * of a location is supposed to be used.
 *
 * @param n
 * @param startLine
 * @param toCompare
 * @return
 */
fun compareLineFromLocationIfExists(n: Node, startLine: Boolean, toCompare: Int): Boolean {
    val loc = n.location ?: return false
    return if (startLine) {
        loc.region.startLine == toCompare
    } else {
        loc.region.endLine == toCompare
    }
}

/** Asserts, that the expression given in [expression] refers to the expected declaration [b]. */
fun assertRefersTo(expression: Expression?, b: Declaration?, message: String? = null) {
    if (expression is Reference) {
        assertEquals(b, (expression as Reference?)?.refersTo, message)
    } else {
        fail("not a reference")
    }
}

/** Asserts, that the expression given in [expression] does not refer to the declaration [b]. */
fun assertNotRefersTo(expression: Expression?, b: Declaration?, message: String? = null) {
    if (expression is Reference) {
        assertNotEquals(b, (expression as Reference?)?.refersTo, message)
    } else {
        fail("not a reference")
    }
}

/**
 * Asserts, that the call expression given in [call] refers to the expected function declaration
 * [func].
 */
fun assertInvokes(call: CallExpression?, func: FunctionDeclaration?, message: String? = null) {
    assertNotNull(call)
    assertContains(call.invokes, func, message)
}

/**
 * Asserts equality or containing of the expected usedNode in the usingNode. If [ENFORCE_REFERENCES]
 * is true, `usingNode` must be a [Reference] where [Reference.refersTo] is or contains `usedNode`.
 * If this is not the case, usage can also be interpreted as equality of the two.
 *
 * @param usingNode
 * - The node that shows usage of another node.
 *
 * @param usedNode
 * - The node that is expected to be used.
 */
fun assertUsageOf(usingNode: Node?, usedNode: Node?) {
    assertNotNull(usingNode)
    if (usingNode !is Reference && !ENFORCE_REFERENCES) {
        assertSame(usedNode, usingNode)
    } else {
        assertTrue(usingNode is Reference)
        val reference = usingNode as? Reference
        assertEquals(usedNode, reference?.refersTo)
    }
}

/**
 * Asserts that `usingNode` uses/references the provided `usedBase` and `usedMember`. If
 * [ENFORCE_MEMBER_EXPRESSION] is true, `usingNode` must be a
 * [de.fraunhofer.aisec.cpg.graph.ast.statements.expressions.MemberExpression] where
 * [de.fraunhofer.aisec.cpg.graph.ast.statements.expressions.MemberExpression.base] uses `usedBase`
 * and [ ][MemberExpression.refersTo] uses `usedMember`. Using is checked as preformed per
 * [assertUsageOf]
 *
 * @param usingNode
 * - Node that uses some member
 *
 * @param usedBase
 * - The expected base that is used
 *
 * @param usedMember
 * - THe expected member that is used
 */
fun assertUsageOfMemberAndBase(usingNode: Node?, usedBase: Node?, usedMember: Declaration?) {
    assertNotNull(usingNode)
    if (usingNode !is MemberExpression && !ENFORCE_MEMBER_EXPRESSION) {
        // Assumption here is that the target of the member portion of the expression and not the
        // base is resolved
        assertUsageOf(usingNode, usedMember)
    } else {
        assertTrue(usingNode is MemberExpression)
        val memberExpressionExpression = usingNode as MemberExpression?
        assertNotNull(memberExpressionExpression)

        val base = memberExpressionExpression.base
        assertUsageOf(base, usedBase)
        assertUsageOf(memberExpressionExpression.refersTo, usedMember)
    }
}

fun assertFullName(fqn: String, node: Node?, message: String? = null) {
    assertNotNull(node, message)
    assertEquals(fqn, node.name.toString(), message)
}

fun assertLocalName(localName: String, node: Node?, message: String? = null) {
    assertNotNull(node)
    assertEquals(localName, node.name.localName, message)
}

/**
 * Asserts that
 * - the expression in [expr] is a
 *   [de.fraunhofer.aisec.cpg.graph.ast.statements.expressions.Literal] and
 * - that it's value is equal to [expected].
 *
 * Guarantees that [expr] is not null if the assertion on the value succeeds.
 */
@OptIn(ExperimentalContracts::class)
fun <T : Any?> assertLiteralValue(expected: T, expr: Expression?, message: String? = null) {
    contract {
        returns() implies (expr != null)
    } // the not-null contract holds as the call to `assertIs` is not accepting `null`
    assertEquals(expected, assertIs<Literal<T>>(expr).value, message)
}

fun ContextProvider.assertResolvedType(fqn: String): Type {
    var type =
        ctx.typeManager.lookupResolvedType(fqn, language = (this as? LanguageProvider)?.language)
    return assertNotNull(type)
}
