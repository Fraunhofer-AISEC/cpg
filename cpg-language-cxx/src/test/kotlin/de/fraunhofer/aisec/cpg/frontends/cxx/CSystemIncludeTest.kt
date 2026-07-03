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
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.condition.EnabledIf
import org.junit.jupiter.api.condition.EnabledOnOs
import org.junit.jupiter.api.condition.OS

/**
 * Exercises the C frontend against a real system-header chain (macOS SDK) to catch regressions in
 * symbol resolution when a translation unit pulls in the full stdio.h transitive include tree.
 *
 * The macOS Command Line Tools SDK is fixed at
 * `/Library/Developer/CommandLineTools/SDKs/MacOSX.sdk`, which is a stable symlink to whatever
 * concrete SDK is installed. The clang preprocessor predefines and compiler-intrinsic typedefs live
 * inside the frontend itself as classpath resources under `builtins/darwin-arm64/cpg_builtins.h`
 * (with cross-target pieces in a sibling `builtins/common.h`); the test only has to enable the
 * feature via `.injectCompilerBuiltins("darwin-arm64")`.
 */
internal class CSystemIncludeTest : BaseTest() {

    @Test
    @EnabledOnOs(OS.MAC)
    @EnabledIf("macOSSdkAvailable")
    fun testPrintfWithSystemStdio() {
        val file = File("src/test/resources/c/system_include/hello.c")

        val result =
            analyze(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CLanguage>()
                it.includePath(MACOS_SDK_INCLUDE_PATH)
                it.injectCompilerBuiltins("darwin-arm64")
            }
        assertNotNull(result)

        // Every call in hello.c should resolve to the *real* declaration from its respective
        // system header (not an inferred stand-in). Covering one function per header keeps the
        // guardrail broad without becoming a tautology.
        val expectedResolvedCalls =
            mapOf(
                "printf" to "stdio.h",
                "malloc" to "stdlib.h",
                "free" to "stdlib.h",
                "strlen" to "string.h",
                "strcpy" to "string.h",
                "sqrt" to "math.h",
            )
        for ((callName, header) in expectedResolvedCalls) {
            val call = result.calls[callName]
            assertNotNull(call, "expected a call to $callName from <$header> in hello.c")
            assertTrue(
                call.invokes.isNotEmpty(),
                "$callName call did not resolve to any declaration",
            )
            val decl = call.invokes.single()
            assertFalse(
                decl.isInferred,
                "$callName should resolve to the real declaration in <$header>, not be inferred",
            )
        }

        // Every reference in the translation unit tree should have been resolved by the
        // SymbolResolver. Any leftover null refersTo is a "symbol resolver error" for our
        // purposes — except for PointerDereference / PointerReference nodes that wrap a
        // non-Reference sub-expression (e.g. `*(_p++)`): the frontend deliberately leaves those
        // nameless and SymbolResolver's empty-name guard early-returns without setting refersTo.
        // Filter them out of the assertion so the check doesn't fire on that legitimate case.
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
        // synthesize a stand-in. The allow-list below enumerates the ones we knowingly accept
        // from the macOS system-header chain — everything else (an anonymous record, a phantom
        // variable named `++`, a `__builtin_va_list` "struct", a synthetic `struct::struct`
        // constructor in C, …) is a precision regression.
        val inferredDecls = result.allChildren<Declaration> { it.isInferred }
        val allowedInferredNames =
            setOf(
                // Opaque forward-declarations whose bodies deliberately never ship in the SDK
                // headers (private ABI, defined only inside Apple's libc/libmalloc).
                "__sFILEX", // <stdio.h> — extension part of FILE
                "_malloc_zone_t", // <malloc/_malloc.h> — pulled in by <stdlib.h>
                // CDT parser quirk: prototypes like `int radixsort(..., unsigned __endbyte);` in
                // _stdlib.h use a single-word `unsigned` (== `unsigned int`) plus a parameter
                // name, and CDT classifies the parameter name as an unknown type — we then infer
                // a Record for it. Not a symbol-resolver bug per se, but not trivially fixable
                // in the parser layer either. Allow-listed so the test stays useful as a
                // resolver-regression canary.
                "__endbyte",
                "__minval",
                "__maxval",
            )
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

        /**
         * True only when we can safely run this test:
         * - the macOS Command Line Tools / Xcode SDK is installed (usual dev-machine case), AND
         * - the current process is running on Apple silicon.
         *
         * The predefines snapshot we ship is captured from `clang -E -dM -x c /dev/null` on
         * `arm64-apple-darwin`; feeding it to a preprocessor on a different architecture would
         * fabricate the wrong `__ARM_*` / `__x86_64__` / `__LP64__` layout and the test would
         * exercise a fiction. Skip cleanly instead.
         */
        @JvmStatic
        @Suppress("unused") // used by @EnabledIf
        fun macOSSdkAvailable(): Boolean {
            val arch = System.getProperty("os.arch").orEmpty()
            val isAppleSilicon = arch == "aarch64" || arch == "arm64"
            return isAppleSilicon && File("$MACOS_SDK_INCLUDE_PATH/stdio.h").isFile
        }
    }
}
