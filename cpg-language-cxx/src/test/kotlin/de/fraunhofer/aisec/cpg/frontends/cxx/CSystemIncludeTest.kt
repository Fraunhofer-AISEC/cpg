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
package de.fraunhofer.aisec.cpg.frontends.cxx

import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.Declaration
import de.fraunhofer.aisec.cpg.test.*
import java.io.File
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIf
import org.junit.jupiter.api.condition.EnabledOnOs
import org.junit.jupiter.api.condition.OS

/**
 * Exercises the C frontend against a real system-header chain (macOS SDK) to catch regressions in
 * symbol resolution when a translation unit pulls in the full stdio.h transitive include tree.
 *
 * The macOS Command Line Tools SDK is fixed at
 * `/Library/Developer/CommandLineTools/SDKs/MacOSX.sdk`, which is a stable symlink to whatever
 * concrete SDK is installed. The clang preprocessor predefines are checked in at
 * `src/test/resources/c/system_include/clang_predefines.txt` so the test does not shell out to the
 * host toolchain.
 */
internal class CSystemIncludeTest : BaseTest() {

    @Test
    @EnabledOnOs(OS.MAC)
    @EnabledIf("macOSSdkAvailable")
    fun testPrintfWithSystemStdio() {
        val file = File("src/test/resources/c/system_include/hello.c")
        val predefinesFile = File("src/test/resources/c/system_include/clang_predefines.txt")

        val predefines = parseClangPredefines(predefinesFile)

        val result =
            analyze(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CLanguage>()
                it.symbols(predefines)
                it.includePath(MACOS_SDK_INCLUDE_PATH)
                it.injectCompilerBuiltins(true)
            }
        assertNotNull(result)

        val printfCall = result.calls["printf"]
        assertNotNull(printfCall, "expected a call to printf in hello.c")
        assertTrue(
            printfCall.invokes.isNotEmpty(),
            "printf call did not resolve to any declaration",
        )
        val printfDecl = printfCall.invokes.single()
        assertFalse(
            printfDecl.isInferred,
            "printf should be resolved to the declaration from stdio.h, not inferred",
        )

        // Every reference in the translation unit tree should have been resolved by the
        // SymbolResolver. Any leftover null refersTo is a "symbol resolver error" for our purposes.
        // PointerDereference / PointerReference are Reference subclasses only for historical
        // reasons; they represent `*expr` / `&expr` expressions, not symbol lookups, so the
        // resolver deliberately doesn't touch them.
        val unresolved =
            result.refs.filter {
                it.refersTo == null &&
                    it !is de.fraunhofer.aisec.cpg.graph.expressions.PointerDereference &&
                    it !is de.fraunhofer.aisec.cpg.graph.expressions.PointerReference
            }
        assertTrue(
            unresolved.isEmpty(),
            "expected 0 unresolved references, but got ${unresolved.size}:\n" +
                unresolved.joinToString("\n", limit = 20) { "  ${it.name} @ ${it.location}" },
        )

        // Inferred declarations mean the SymbolResolver could not find a real one and had to
        // synthesize a stand-in. The only one we accept from a real stdio.h chain is
        // `struct __sFILEX;` — a forward-declared opaque struct in _stdio.h whose body is
        // deliberately hidden (private ABI, defined only inside Apple's libc, never in the SDK
        // headers). Any other inferred declaration — an anonymous record, a phantom variable
        // named `++`, a `__builtin_va_list` "struct", a synthetic `struct::struct` constructor
        // in C — is a precision regression.
        val inferredDecls = result.allChildren<Declaration> { it.isInferred }
        val allowedInferredNames = setOf("__sFILEX")
        val unexpected = inferredDecls.filter { it.name.toString() !in allowedInferredNames }
        assertTrue(
            unexpected.isEmpty(),
            "unexpected inferred declarations (${unexpected.size}):\n" +
                unexpected.joinToString("\n") {
                    "  ${it::class.simpleName} '${it.name}' @ ${it.location}"
                },
        )
    }

    companion object {
        private const val MACOS_SDK_INCLUDE_PATH =
            "/Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/usr/include"

        @JvmStatic
        @Suppress("unused") // used by @EnabledIf
        fun macOSSdkAvailable(): Boolean = File("$MACOS_SDK_INCLUDE_PATH/stdio.h").isFile

        private val DEFINE_PATTERN = Regex("""^#define\s+(\S+)(?:\s+(.*))?$""")

        private fun parseClangPredefines(file: File): Map<String, String> {
            require(file.isFile) { "clang predefines file not found: $file" }
            return file.useLines { lines ->
                lines
                    .mapNotNull { DEFINE_PATTERN.matchEntire(it) }
                    .associate { m -> m.groupValues[1] to m.groupValues[2] }
            }
        }
    }
}
