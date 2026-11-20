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

import de.fraunhofer.aisec.cpg.graph.functions
import de.fraunhofer.aisec.cpg.project.Project
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import org.junit.jupiter.api.io.TempDir

class PythonBuildProjectTest {
    @Test
    fun testOnlyImportedDependenciesAreAnalyzed(@TempDir tmp: Path) {
        fun Path.file(name: String, content: String) {
            val file = resolve(name)
            file.parent.createDirectories()
            file.writeText(content)
        }

        tmp.file("pyproject.toml", "")
        tmp.file(
            "main.py",
            """
            from fakecrypto import encrypt

            def main():
                encrypt(b"data")
            """
                .trimIndent(),
        )

        val sitePackages = ".venv/lib/python3.12/site-packages"
        tmp.file(".venv/pyvenv.cfg", "")
        tmp.file("$sitePackages/fakecrypto/__init__.py", "def encrypt(data):\n    return data\n")
        tmp.file("$sitePackages/unused/__init__.py", "def never_imported():\n    pass\n")

        val project =
            Project.from(tmp) {
                registerLanguage<PythonLanguage>()
                detector(PythonBuildDetector())
            }
        println(
            "SRC=" +
                project.config.softwareComponents +
                " INC=" +
                project.config.includePaths +
                " LOAD=" +
                project.config.loadIncludes
        )
        val result = project.analyze()
        println(
            "COMPONENTS=" +
                result.components.map {
                    it.name.toString() +
                        ":" +
                        it.functions.map { f ->
                            f.name.toString() + (if (f.isInferred) "(inferred)" else "")
                        }
                }
        )

        val application =
            assertNotNull(
                result.components.firstOrNull { it.name.localName == tmp.fileName.toString() }
            )
        assertEquals(listOf("main"), application.functions.map { it.name.localName })

        // The imported module is parsed into a component of its own, named after the include path
        val dependencies =
            assertNotNull(result.components.firstOrNull { it.name.localName == "site-packages" })
        val encrypt = dependencies.functions.single()
        assertEquals("fakecrypto.encrypt", encrypt.name.toString())
        assertFalse(encrypt.isInferred)

        // `unused` is never imported, so it is not part of the graph
        assertNull(result.functions.firstOrNull { it.name.localName == "never_imported" })
    }
}
