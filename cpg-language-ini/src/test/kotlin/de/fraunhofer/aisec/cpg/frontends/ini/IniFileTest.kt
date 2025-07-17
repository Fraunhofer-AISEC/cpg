/*
 * Copyright (c) 2024, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.frontends.ini

import de.fraunhofer.aisec.cpg.graph.dRecords
import de.fraunhofer.aisec.cpg.graph.declarations.FieldDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.RecordDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.graph.get
import de.fraunhofer.aisec.cpg.test.BaseTest
import de.fraunhofer.aisec.cpg.test.analyzeAndGetFirstTU
import de.fraunhofer.aisec.cpg.test.assertFullName
import de.fraunhofer.aisec.cpg.test.assertLiteralValue
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class IniFileTest : BaseTest() {

    @Test
    fun testSimpleINIFile() {
        val topLevel = Path.of("src", "test", "resources")
        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("config.ini").toFile()), topLevel, true) {
                it.registerLanguage<IniFileLanguage>()
            }
        assertIs<TranslationUnitDeclaration>(tu)

        val namespace = tu.namespaces.firstOrNull()
        assertNotNull(namespace)
        assertFullName(
            "config",
            namespace,
            "Namespace name mismatch.",
        ) // analyzeAndGetFirstTU does not provide the full path

        assertEquals(2, tu.dRecords.size, "Expected two records")

        val sectionA = tu.dRecords["SectionA"]
        assertIs<RecordDeclaration>(sectionA)
        assertEquals(2, sectionA.fields.size, "Expected two fields")

        val sectionAEntry1 = sectionA.fields["key1"]
        assertIs<FieldDeclaration>(sectionAEntry1)
        assertLiteralValue("value1", sectionAEntry1.initializer)

        val sectionAEntry2 = sectionA.fields["key2"]
        assertIs<FieldDeclaration>(sectionAEntry2)
        assertLiteralValue("value2", sectionAEntry2.initializer)

        val sectionB = tu.dRecords["SectionB"]
        assertIs<RecordDeclaration>(sectionB)
        assertEquals(3, sectionB.fields.size, "Expected three fields")

        val sectionBEntry1 = sectionB.fields["key1"]
        assertIs<FieldDeclaration>(sectionBEntry1)
        assertLiteralValue("123", sectionBEntry1.initializer)

        val sectionBEntry2 = sectionB.fields["key2"]
        assertIs<FieldDeclaration>(sectionBEntry2)
        assertLiteralValue("1.2.3.4", sectionBEntry2.initializer)

        val sectionBEntry3 = sectionB.fields["key3"]
        assertIs<FieldDeclaration>(sectionBEntry3)
        assertLiteralValue("\"abc\"", sectionBEntry3.initializer)
    }
}
