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
package de.fraunhofer.aisec.codyze.console.repl

import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.SourceCode
import kotlin.script.experimental.api.SourceCodeCompletionVariant
import kotlin.script.experimental.host.toScriptSource
import kotlinx.coroutines.runBlocking
import org.jline.reader.Candidate
import org.jline.reader.Completer
import org.jline.reader.LineReader
import org.jline.reader.ParsedLine

/**
 * JLine [Completer] that defers to the Kotlin scripting IDE-services REPL compiler.
 *
 * For every TAB press, JLine calls [complete] with the full current line and a cursor position. We
 * hand both to
 * [KJvmReplCompilerWithIdeServices.complete][org.jetbrains.kotlin.scripting.ide_services.compiler.KJvmReplCompilerWithIdeServices.complete],
 * which runs the same semantic analysis the Kotlin compiler uses — so the candidates are type-aware
 * against the actual `result: TranslationResult` binding and any vals declared in previous REPL
 * lines.
 *
 * The IDE-services completer returns a sequence of [SourceCodeCompletionVariant]; we project each
 * into a JLine [Candidate] keeping the original variant text as the completion value and the type
 * info ("tail") as the description for the menu.
 */
class KotlinReplCompleter(private val replService: ReplService) : Completer {

    private val completionCounter = java.util.concurrent.atomic.AtomicInteger(0)

    override fun complete(
        reader: LineReader,
        parsedLine: ParsedLine,
        candidates: MutableList<Candidate>,
    ) {
        val line = parsedLine.line() ?: return
        val cursor = parsedLine.cursor()
        if (line.isBlank()) return

        // The compiler needs a fresh "source name" so it doesn't confuse this with a real snippet.
        // We use a separate counter from the eval line counter to keep them distinct.
        val source: SourceCode =
            line.toScriptSource("Completion_${completionCounter.incrementAndGet()}.cpg.query.kts")
        val position = SourceCode.Position(line = 0, col = 0, absolutePos = cursor)

        val result = runBlocking {
            replService.compiler.complete(source, position, replService.compilationConfig())
        }

        if (result !is ResultWithDiagnostics.Success) return

        // After a member-access dot (e.g. `result.`), Kotlin keywords like `do`, `if`, `when`
        // are syntactically invalid — the IDE-services compiler still emits them as a fallback
        // list, so we filter them out in that position.
        val afterDot = isAfterMemberAccess(line, cursor)
        result.value.forEach { variant ->
            if (afterDot && variant.icon == "keyword") return@forEach
            candidates.add(toCandidate(variant))
        }
    }

    /** True if the character right before [cursor] is a `.` (ignoring intervening word chars). */
    private fun isAfterMemberAccess(line: String, cursor: Int): Boolean {
        var i = cursor - 1
        while (i >= 0 && (line[i].isLetterOrDigit() || line[i] == '_')) i--
        return i >= 0 && line[i] == '.'
    }

    private fun toCandidate(variant: SourceCodeCompletionVariant): Candidate {
        // SourceCodeCompletionVariant exposes:
        //   text       — replacement text (what gets inserted)
        //   displayText — what to show in the menu
        //   tail       — usually the type signature (e.g. "(String): Boolean")
        //   icon       — category hint ("method", "property", "class", "keyword", …)
        val display = if (variant.displayText.isNotEmpty()) variant.displayText else variant.text
        val desc = listOfNotNull(variant.tail.takeIf { it.isNotEmpty() }).joinToString(" ")
        return Candidate(
            /* value = */ variant.text,
            /* displ = */ display,
            /* group = */ variant.icon.ifEmpty { null },
            /* descr = */ desc.ifEmpty { null },
            /* suffix = */ "",
            /* key = */ null,
            // false: don't treat the completion as terminal — JLine otherwise appends a
            // trailing space, which is annoying for identifiers you usually continue with
            // `.foo`, `[i]`, or `(...)`.
            /* complete = */ false,
        )
    }
}
