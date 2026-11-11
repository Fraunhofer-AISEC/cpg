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
package de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils

import de.fraunhofer.aisec.cpg.frontends.cxx.CLanguage
import de.fraunhofer.aisec.cpg.graph.calls
import de.fraunhofer.aisec.cpg.graph.functions
import de.fraunhofer.aisec.cpg.graph.records
import de.fraunhofer.aisec.cpg.test.analyze
import java.nio.file.Path
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.io.TempDir

/**
 * The compact listing entries keep the signature clean and carry the location in a field of its
 * own, which is left out of the JSON when a node has none.
 */
class SignatureInfoTest {

    @Test
    fun signatureAndLocationAreSeparate(@TempDir dir: Path) {
        val source = dir.resolve("net.c")
        source.writeText(
            """
            struct Connection { int fd; };
            int helper(int x) { return x; }
            int main() { helper(1); undeclared(2); return 0; }
            """
                .trimIndent()
        )
        val result =
            analyze(listOf(source.toFile()), dir, true) { it.registerLanguage<CLanguage>() }

        val helper = result.functions.single { it.name.localName == "helper" }.toSignatureInfo()
        assertEquals(
            helper.signature,
            result.functions.single { it.name.localName == "helper" }.signature,
        )
        assertEquals("net.c:2", helper.location)

        val call = result.calls.single { it.name.localName == "helper" }.toSignatureInfo()
        assertEquals("helper(_)", call.signature)
        assertEquals("net.c:3", call.location)

        val record = result.records.single { it.name.localName == "Connection" }.toSignatureInfo()
        assertEquals("struct Connection", record.signature)
        assertEquals("net.c:1", record.location)

        val inferred =
            result.functions.single { it.name.localName == "undeclared" }.toSignatureInfo()
        assertNull(inferred.location)
        assertFalse("location" in Json.encodeToString(inferred), Json.encodeToString(inferred))
    }
}
