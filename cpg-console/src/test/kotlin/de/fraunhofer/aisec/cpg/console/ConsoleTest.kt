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
package de.fraunhofer.aisec.cpg.console

import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.console.CpgConsole.configureREPL
import kotlin.script.experimental.api.ResultValue
import kotlin.script.experimental.api.onFailure
import kotlin.script.experimental.api.valueOrNull
import kotlin.script.experimental.jvm.KJvmEvaluatedSnippet
import kotlin.script.experimental.util.LinkedSnippet
import kotlin.test.Test
import kotlin.test.assertIs
import org.junit.jupiter.api.Tag

@Tag("integration")
class ConsoleTest {

    @Test
    fun testShell() {
        var repl = configureREPL()
        repl.initEngine()
        repl.eval("import de.fraunhofer.aisec.cpg.TranslationConfiguration")
        var result = repl.eval("TranslationConfiguration.builder().build()")
        result.result.onFailure { println(it) }

        var snippet = result.result.valueOrNull()
        assertIs<LinkedSnippet<*>>(snippet)
        val evaluated = snippet.get()
        assertIs<KJvmEvaluatedSnippet>(evaluated)
        val resultValue = evaluated.result
        assertIs<ResultValue.Value>(resultValue)
        val value = resultValue.value
        assertIs<TranslationConfiguration>(value)
    }
}
