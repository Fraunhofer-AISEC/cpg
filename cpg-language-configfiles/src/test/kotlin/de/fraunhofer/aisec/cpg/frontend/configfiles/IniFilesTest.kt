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
package de.fraunhofer.aisec.cpg.frontend.configfiles

import de.fraunhofer.aisec.cpg.graph.declarations.FieldDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.RecordDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.graph.get
import de.fraunhofer.aisec.cpg.graph.records
import de.fraunhofer.aisec.cpg.test.BaseTest
import de.fraunhofer.aisec.cpg.test.analyzeAndGetFirstTU
import de.fraunhofer.aisec.cpg.test.assertLiteralValue
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class IniFilesTest : BaseTest() {

    @Test
    fun gettingStartedWithINIConfigfiles() {
        val topLevel = Path.of("src", "test", "resources")
        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("config.ini").toFile()), topLevel, true) {
                it.registerLanguage<IniFilesLanguage>()
            }
        assertIs<TranslationUnitDeclaration>(tu)

        assertEquals(2, tu.records.size, "Expected two records")

        val ownerRecord = tu.records["owner"]
        assertIs<RecordDeclaration>(ownerRecord)
        assertEquals(2, ownerRecord.fields.size, "Expected two fields")

        val nameField = ownerRecord.fields["name"]
        assertIs<FieldDeclaration>(nameField)
        assertLiteralValue("John Doe", nameField.initializer)

        val organizationField = ownerRecord.fields["organization"]
        assertIs<FieldDeclaration>(organizationField)
        assertLiteralValue("Acme Widgets Inc.", organizationField.initializer)

        val databaseRecord = tu.records["database"]
        assertIs<RecordDeclaration>(databaseRecord)
        assertEquals(3, databaseRecord.fields.size, "Expected three fields")

        val serverField = databaseRecord.fields["server"]
        assertIs<FieldDeclaration>(serverField)
        assertLiteralValue("192.0.2.62", serverField.initializer)

        val portField = databaseRecord.fields["port"]
        assertIs<FieldDeclaration>(portField)
        assertLiteralValue("143", portField.initializer)

        val fileField = databaseRecord.fields["file"]
        assertIs<FieldDeclaration>(fileField)
        assertLiteralValue("\"payroll.dat\"", fileField.initializer)
    }
}
