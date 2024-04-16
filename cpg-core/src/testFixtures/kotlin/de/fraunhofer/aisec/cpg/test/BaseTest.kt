/*
<<<<<<<< HEAD:cpg-core/src/testFixtures/kotlin/de/fraunhofer/aisec/cpg/test/BaseTest.kt
 * Copyright (c) 2021, Fraunhofer AISEC. All rights reserved.
========
 * Copyright (c) 2024, Fraunhofer AISEC. All rights reserved.
>>>>>>>> ff28e4eebf (Concepts for symbols):cpg-core/src/main/kotlin/de/fraunhofer/aisec/cpg/graph/scopes/FileScope.kt
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
<<<<<<<< HEAD:cpg-core/src/testFixtures/kotlin/de/fraunhofer/aisec/cpg/test/BaseTest.kt
package de.fraunhofer.aisec.cpg.test

import kotlin.jvm.javaClass
import org.slf4j.Logger
import org.slf4j.LoggerFactory

abstract class BaseTest {
    protected var log: Logger = LoggerFactory.getLogger(this.javaClass)
}
========
package de.fraunhofer.aisec.cpg.graph.scopes

import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration

/**
 * Represents a scope that is only visible in the current file. This is usually used in programming
 * languages for file-level imports.
 *
 * The only supported AST node is a [TranslationUnitDeclaration].
 */
class FileScope(astNode: TranslationUnitDeclaration?) : Scope(astNode)
>>>>>>>> ff28e4eebf (Concepts for symbols):cpg-core/src/main/kotlin/de/fraunhofer/aisec/cpg/graph/scopes/FileScope.kt
