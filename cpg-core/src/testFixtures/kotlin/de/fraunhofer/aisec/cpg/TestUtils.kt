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
package de.fraunhofer.aisec.cpg

import de.fraunhofer.aisec.cpg.frontends.CompilationDatabase
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.TypeManager
import de.fraunhofer.aisec.cpg.graph.declarations.Declaration
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.function.Consumer
import java.util.function.Predicate
import java.util.stream.Collectors
import kotlin.test.*
import org.apache.commons.lang3.reflect.FieldUtils
import org.mockito.Mockito

object TestUtils {

    /**
     * Currently when variables or fields are used they can be stored in the expression itself, in
     * the future, a reference as usage indicator pointing to the used ValueDeclaration is planed.
     */
    var ENFORCE_REFERENCES = false

    /**
     * Currently there is no unified enforced structure when using fields, this field is used set
     * whether or not the tests enforce the presence of a member expression
     */
    var ENFORCE_MEMBER_EXPRESSION = false

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
     * Like [TestUtils.analyze], but for all files in a directory tree having a specific file
     * extension
     *
     * @param fileExtension All files found in the directory must end on this String. An empty
     *   string matches all files
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
        configModifier: Consumer<TranslationConfiguration.Builder>? = null
    ): TranslationResult {
        val files =
            Files.walk(topLevel, Int.MAX_VALUE)
                .map(Path::toFile)
                .filter { it.isFile }
                .filter { it.name.endsWith(fileExtension!!) }
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
        configModifier: Consumer<TranslationConfiguration.Builder>? = null
    ): TranslationResult {
        val builder =
            TranslationConfiguration.builder()
                .sourceLocations(files)
                .topLevel(topLevel.toFile())
                .loadIncludes(true)
                .disableCleanup()
                .debugParser(true)
                .failOnError(true)
                .typeSystemActiveInFrontend(false)
                .useParallelFrontends(true)
                .defaultLanguages()
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
        return analyzer.analyze().get().translationUnits
    }

    @JvmOverloads
    @Throws(Exception::class)
    fun analyzeAndGetFirstTU(
        files: List<File>,
        topLevel: Path,
        usePasses: Boolean,
        configModifier: Consumer<TranslationConfiguration.Builder>? = null
    ): TranslationUnitDeclaration {
        val result = analyze(files, topLevel, usePasses, configModifier)
        return result.translationUnits.first()
    }

    @Throws(Exception::class)
    fun analyzeWithCompilationDatabase(
        jsonCompilationDatabase: File,
        usePasses: Boolean,
        configModifier: Consumer<TranslationConfiguration.Builder>? = null
    ): TranslationResult {
        return analyze(listOf(), jsonCompilationDatabase.parentFile.toPath(), usePasses) {
            val db = CompilationDatabase.fromFile(jsonCompilationDatabase)
            if (db.isNotEmpty()) {
                it.useCompilationDatabase(db)
                it.softwareComponents(db.components as MutableMap<String, List<File>>)
                configModifier?.accept(it)
            }
            configModifier?.accept(it)
        }
    }

    @Throws(IllegalAccessException::class)
    fun disableTypeManagerCleanup() {
        val spy = Mockito.spy(TypeManager.getInstance())
        Mockito.doNothing().`when`(spy).cleanup()
        FieldUtils.writeStaticField(TypeManager::class.java, "instance", spy, true)
    }

    /**
     * Compare the given parameter `toCompare` to the start- or end-line of the given node. If the
     * node has no location `false` is returned. `startLine` is used to specify if the start-line or
     * end-line of a location is supposed to be used.
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

    /**
     * Asserts, that the expression given in [expression] refers to the expected declaration [b].
     */
    fun assertRefersTo(expression: Expression?, b: Declaration?) {
        if (expression is DeclaredReferenceExpression) {
            assertEquals(b, (expression as DeclaredReferenceExpression?)?.refersTo)
        } else {
            fail("not a reference")
        }
    }

    /**
     * Asserts, that the call expression given in [call] refers to the expected function declaration
     * [func].
     */
    fun assertInvokes(call: CallExpression, func: FunctionDeclaration?) {
        assertContains(call.invokes, func)
    }

    /**
     * Asserts equality or containing of the expected usedNode in the usingNode. If
     * [ENFORCE_REFERENCES] is true, `usingNode` must be a [DeclaredReferenceExpression] where
     * [DeclaredReferenceExpression.refersTo] is or contains `usedNode`. If this is not the case,
     * usage can also be interpreted as equality of the two.
     *
     * @param usingNode
     * - The node that shows usage of another node.
     *
     * @param usedNode
     * - The node that is expected to be used.
     */
    fun assertUsageOf(usingNode: Node?, usedNode: Node?) {
        assertNotNull(usingNode)
        if (usingNode !is DeclaredReferenceExpression && !ENFORCE_REFERENCES) {
            assertSame(usedNode, usingNode)
        } else {
            assertTrue(usingNode is DeclaredReferenceExpression)
            val reference = usingNode as? DeclaredReferenceExpression
            assertEquals(usedNode, reference?.refersTo)
        }
    }

    /**
     * Asserts that `usingNode` uses/references the provided `usedBase` and `usedMember`. If
     * [ENFORCE_MEMBER_EXPRESSION] is true, `usingNode` must be a [MemberExpression] where
     * [MemberExpression.base] uses `usedBase` and [ ][MemberExpression.refersTo] uses `usedMember`.
     * Using is checked as preformed per [assertUsageOf]
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
    fun assertUsageOfMemberAndBase(usingNode: Node?, usedBase: Node?, usedMember: Node?) {
        assertNotNull(usingNode)
        if (usingNode !is MemberExpression && !ENFORCE_MEMBER_EXPRESSION) {
            // Assumtion here is that the target of the member portion of the expression and not the
            // base is resolved
            assertUsageOf(usingNode, usedMember)
        } else {
            assertTrue(usingNode is MemberExpression)
            val memberExpression = usingNode as MemberExpression?
            assertNotNull(memberExpression)

            val base = memberExpression.base
            assertUsageOf(base, usedBase)
            assertUsageOf(memberExpression.refersTo, usedMember)
        }
    }
}

fun assertFullName(fqn: String, node: Node?, message: String? = null) {
    assertNotNull(node)
    assertEquals(fqn, node.name.toString(), message)
}

fun assertLocalName(localName: String, node: Node?, message: String? = null) {
    assertNotNull(node)
    assertEquals(localName, node.name.localName, message)
}

/**
 * Asserts that a) the expression in [expr] is a [Literal] and b) that it's value is equal to
 * [expected].
 */
fun <T : Any?> assertLiteralValue(expected: T, expr: Expression?, message: String? = null) {
    assertEquals(expected, assertIs<Literal<T>>(expr).value, message)
}
