/*
 * Copyright (c) 2025, Fraunhofer AISEC. All rights reserved.
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

import de.fraunhofer.aisec.cpg.graph.calls
import de.fraunhofer.aisec.cpg.graph.importedFrom
import de.fraunhofer.aisec.cpg.graph.imports
import de.fraunhofer.aisec.cpg.graph.invoke
import de.fraunhofer.aisec.cpg.test.analyze
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ExtensionsTest {

    /** Test the [importedFrom] extension with Python imports. */
    @Test
    fun testImportedFromExtension() {
        val topLevel = Path.of("src", "test", "resources", "python")
        val result =
            analyze(listOf(topLevel.resolve("importedFrom.py").toFile()), topLevel, true) {
                it.registerLanguage<PythonLanguage>()
            }
        assertNotNull(result)

        /*
        ```python
        import foo
        foo.bar() # This should be mapped to the `import foo` import
         */
        val normalFooImport = result.imports("foo").singleOrNull()
        assertNotNull(normalFooImport)

        val fooBarCall = result.calls.singleOrNull { it.code == "foo.bar()" }
        assertNotNull(fooBarCall)

        assertEquals(
            normalFooImport,
            fooBarCall.importedFrom.singleOrNull(),
            "Expected the import to exactly match `import foo`.",
        )

        /*
        ```python
        import foo as foo_alias
        foo_alias.bar() # This should be mapped to the `import foo as foo_alias`
         */
        val aliasFooImport = result.imports("foo_alias").singleOrNull()
        assertNotNull(aliasFooImport)

        val fooAliasBarCall = result.calls.singleOrNull { it.code == "foo_alias.bar()" }
        assertNotNull(fooAliasBarCall)

        assertEquals(
            aliasFooImport,
            fooAliasBarCall.importedFrom.singleOrNull(),
            "Expected the import to exactly match `import foo as foo_alias`.",
        )

        /*
        ```python
        import os as not_os
        os.times() # This should not be mapped to the `import os as not_os`
         */
        val aliasOsImport = result.imports("not_os").singleOrNull()
        assertNotNull(aliasOsImport)

        val osTimesCall = result.calls.singleOrNull { it.code == "os.times()" }
        assertNotNull(osTimesCall)

        assertEquals(
            0,
            osTimesCall.importedFrom.size,
            "Expected to find exactly 0 matching imports. `import os as not_os` must not match as the alias is wrong.",
        )

        /*
        ```python
        import os as not_os
        not_os.times() # This should be mapped to `import os as not_os`
        ```
        */
        val notOsTimesCall = result.calls.singleOrNull { it.code == "not_os.times()" }
        assertNotNull(notOsTimesCall)

        assertEquals(
            aliasOsImport,
            notOsTimesCall.importedFrom.singleOrNull(),
            "Expected the import to exactly match `import os as not_os`.",
        )

        /*
        ```python
        from os import times as t
        t() # This should be mapped to the `from os import times as t`
         */
        val fromImportAs = result.imports("t").singleOrNull()
        assertNotNull(fromImportAs)

        val tCall = result.calls.singleOrNull { it.code == "t()" }
        assertNotNull(tCall)

        assertEquals(
            fromImportAs,
            tCall.importedFrom.singleOrNull(),
            "Expected the import to exactly match `from os import times as t`.",
        )
    }
}
