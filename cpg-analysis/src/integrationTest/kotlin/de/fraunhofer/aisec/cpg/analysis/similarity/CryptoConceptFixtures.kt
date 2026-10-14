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
package de.fraunhofer.aisec.cpg.analysis.similarity

import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.frontends.cxx.CLanguage
import de.fraunhofer.aisec.cpg.frontends.rust.RustLanguage
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.Visibility
import de.fraunhofer.aisec.cpg.graph.calls
import de.fraunhofer.aisec.cpg.graph.concepts.Concept
import de.fraunhofer.aisec.cpg.graph.concepts.Operation
import de.fraunhofer.aisec.cpg.graph.concepts.crypto.encryption.Cipher
import de.fraunhofer.aisec.cpg.graph.concepts.crypto.encryption.Decrypt
import de.fraunhofer.aisec.cpg.graph.concepts.crypto.encryption.Encrypt
import de.fraunhofer.aisec.cpg.graph.concepts.crypto.encryption.GetSecret
import de.fraunhofer.aisec.cpg.graph.concepts.crypto.encryption.newCipher
import de.fraunhofer.aisec.cpg.graph.concepts.crypto.encryption.newEncryptOperation
import de.fraunhofer.aisec.cpg.graph.concepts.crypto.encryption.newGetSecret
import de.fraunhofer.aisec.cpg.graph.concepts.crypto.encryption.newSecret
import de.fraunhofer.aisec.cpg.graph.concepts.file.WriteFile
import de.fraunhofer.aisec.cpg.graph.concepts.file.newFile
import de.fraunhofer.aisec.cpg.graph.concepts.file.newFileWrite
import de.fraunhofer.aisec.cpg.graph.concepts.logging.newLogWrite
import de.fraunhofer.aisec.cpg.graph.concepts.logging.newLogging
import de.fraunhofer.aisec.cpg.graph.concepts.newOperation
import de.fraunhofer.aisec.cpg.graph.concepts.ontology.LogLevel
import de.fraunhofer.aisec.cpg.graph.concepts.ontology.LogWrite
import de.fraunhofer.aisec.cpg.graph.declarations.Function
import de.fraunhofer.aisec.cpg.graph.expressions.Literal
import de.fraunhofer.aisec.cpg.graph.functions
import de.fraunhofer.aisec.cpg.graph.get
import de.fraunhofer.aisec.cpg.passes.ControlDependenceGraphPass
import de.fraunhofer.aisec.cpg.passes.ProgramDependenceGraphPass
import de.fraunhofer.aisec.cpg.test.analyze
import java.nio.file.Path

/**
 * Parses [fileName] (C) from [resourceDir], with the default passes plus
 * [ControlDependenceGraphPass]/[ProgramDependenceGraphPass], and tags the calls recognized by
 * [tagCryptoConcepts].
 */
fun analyzeC(resourceDir: Path, fileName: String): TranslationResult =
    analyze(listOf(resourceDir.resolve(fileName).toFile()), resourceDir, usePasses = true) {
            it.registerLanguage<CLanguage>()
            it.registerPass<ControlDependenceGraphPass>()
            it.registerPass<ProgramDependenceGraphPass>()
        }
        .also(::tagCryptoConcepts)

/** Same as [analyzeC], but for a Rust [fileName]. */
fun analyzeRust(resourceDir: Path, fileName: String): TranslationResult =
    analyze(listOf(resourceDir.resolve(fileName).toFile()), resourceDir, usePasses = true) {
            it.registerLanguage<RustLanguage>()
            it.registerPass<ControlDependenceGraphPass>()
            it.registerPass<ProgramDependenceGraphPass>()
        }
        .also(::tagCryptoConcepts)

/**
 * Tags every recognized call in every function of [result] with a *real*, pre-existing
 * `cpg-concepts` overlay, standing in for what a real concept-inference pass would have attached:
 * - `get_secret(...)` -> [GetSecret] (belonging to an unconnected
 *   [de.fraunhofer.aisec.cpg.graph.concepts.crypto.encryption.Secret], since we don't need the
 *   concept itself to show up as a separate hit next to the operation).
 * - `new_cipher("<name>")` -> a [Cipher] concept, with [Cipher.cipherName] taken from the string
 *   literal argument. This call has no paired operation, so the concept itself is the visible
 *   overlay.
 * - `do_encrypt(cipher, ...)` / `do_decrypt(cipher, ...)` -> [Encrypt] / [Decrypt], linked to the
 *   [Cipher] created for the same function's `new_cipher(...)` call.
 * - `log_event(...)` -> [LogWrite] (belonging to an unconnected `Logging` concept).
 * - `write_output(...)` -> [WriteFile] (belonging to an unconnected `File` concept).
 *
 * Every recognized call ends up with *exactly one* overlay attached directly to it (never both a
 * concept and its operation), so [conceptOrOperationOf] has exactly one thing to find per node.
 */
fun tagCryptoConcepts(result: TranslationResult) {
    result.functions.forEach { function ->
        function.calls["get_secret"]?.let { call ->
            val secret = call.newSecret(underlyingNode = call, connect = false)
            call.newGetSecret(underlyingNode = call, concept = secret, connect = true)
        }

        val cipher =
            function.calls["new_cipher"]?.let { call ->
                val cipherName = (call.arguments.firstOrNull() as? Literal<*>)?.value as? String
                call.newCipher(underlyingNode = call, connect = true).also {
                    it.cipherName = cipherName
                }
            }

        if (cipher != null) {
            function.calls["do_encrypt"]?.let { call ->
                val key = call.newSecret(underlyingNode = call, connect = false)
                call.newEncryptOperation(
                    underlyingNode = call,
                    concept = cipher,
                    key = key,
                    connect = true,
                )
            }
            function.calls["do_decrypt"]?.let { call ->
                val key = call.newSecret(underlyingNode = call, connect = false)
                call.newOperation(
                    { concept -> Decrypt(concept = concept, key = key) },
                    underlyingNode = call,
                    concept = cipher,
                    connect = true,
                )
            }
        }

        function.calls["log_event"]?.let { call ->
            val logging = call.newLogging(underlyingNode = call, name = "audit", connect = false)
            call.newLogWrite(
                underlyingNode = call,
                concept = logging,
                level = LogLevel.INFO,
                logArguments = call.arguments,
                connect = true,
            )
        }

        function.calls["write_output"]?.let { call ->
            val file = call.newFile(underlyingNode = call, fileName = "output.bin", connect = false)
            call.newFileWrite(
                underlyingNode = call,
                file = file,
                what = call.arguments.firstOrNull() ?: call,
                connect = true,
            )
        }
    }
}

/**
 * Whether [function] should be treated as an entry point ("public function").
 *
 * C never gets an explicit "public" modifier (there is no such keyword; non-`static` is already
 * externally visible by default), so [de.fraunhofer.aisec.cpg.graph.Visibility.INTERNAL] (which the
 * C frontend *does* set for `static` functions) is used as the negative signal instead.
 *
 * The Rust frontend does not populate [Function.modifiers]/[Function.visibility] for `pub` at all
 * yet (a real gap, not a design choice) -- as a workaround *for this test's own resource files
 * only*, visibility is read directly off the source text instead.
 */
fun isEntryPoint(function: Function): Boolean =
    if (function.language is CLanguage) {
        function.visibility != Visibility.INTERNAL
    } else {
        function.code?.trimStart()?.startsWith("pub") == true
    }

/**
 * The [Concept]/[Operation] overlay directly attached to [node], if any. A `(Node) -> T?`
 * predicate.
 */
fun conceptOrOperationOf(node: Node): Node? =
    node.overlays.firstOrNull { it is Concept || it is Operation }

/** Formats a tagged overlay (see [tagCryptoConcepts]) as a label for [lexicographicSimilarity]. */
fun conceptLabel(overlay: Node): String =
    when (overlay) {
        is Cipher -> "Cipher:${overlay.cipherName}"
        is Encrypt -> "Encrypt"
        is Decrypt -> "Decrypt"
        is GetSecret -> "GetSecret"
        is LogWrite -> "LogWrite:${overlay.logLevel}"
        is WriteFile -> "WriteFile:${overlay.file.fileName}"
        else -> overlay::class.simpleName ?: "?"
    }
