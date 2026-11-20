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
package de.fraunhofer.aisec.cpg.frontends.python

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PythonRequirementsTest {
    @Test
    fun testPlainPackagesAreSupported() {
        val requirements =
            """
            # a comment
            cryptography==44.0.0
            requests>=2.0,<3  # trailing comment
            PyNaCl[extra]~=1.5 ; python_version >= "3.9"
            Some_Package.name
            six===1.16.0 \
                --hash=sha256:8abb2f1d86890a2dfb989f9a77cfcfd3e47c2a354b01111771326f8aa26e0254 \
                --hash=sha256:1234

            """
                .trimIndent()

        assertNull(PythonRequirements.findUnsupported(requirements))
        assertNull(PythonRequirements.findUnsupported(""))
    }

    @Test
    fun testDirectReferencesAreNotSupported() {
        // None of these can be installed without building something that is not from an index
        for (requirement in
            listOf(
                "./localpkg",
                "../localpkg",
                "/abs/path/pkg",
                "~/pkg",
                "localpkg/",
                "-e .",
                "--editable ./pkg",
                "git+https://example.com/pkg.git",
                "pkg @ https://example.com/pkg-1.0.tar.gz",
                "pkg@https://example.com/pkg-1.0.tar.gz",
                "https://example.com/pkg-1.0-py3-none-any.whl",
                "file:///tmp/pkg",
                "C:\\\\pkg",
            )) {
            assertEquals(
                requirement,
                PythonRequirements.findUnsupported("six==1.16.0\n$requirement\n"),
                requirement,
            )
        }
    }

    @Test
    fun testOptionsAreNotSupported() {
        for (option in
            listOf(
                "-r other.txt",
                "-c constraints.txt",
                "--index-url https://example.com/simple",
                "--extra-index-url https://example.com/simple",
                "--find-links ./wheels",
                "-f ./wheels",
                "--no-binary :all:",
            )) {
            assertEquals(option, PythonRequirements.findUnsupported("$option\nsix==1.16.0\n"))
        }
    }
}
