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

import kotlin.script.experimental.api.ScriptDiagnostic

private const val ESC = "\u001B"
private const val RESET = "$ESC[0m"
private const val DIM = "$ESC[2m"
private const val BOLD = "$ESC[1m"
private const val RED = "$ESC[31m"
private const val YELLOW = "$ESC[33m"
private const val BLUE = "$ESC[34m"

/**
 * Formats [ScriptDiagnostic] entries in a rustc-flavored layout:
 * ```
 * error: Only safe (?.) calls allowed on a nullable receiver of type Function?
 *   1 | result.functions["main"].prevDFG
 *     |                         ^^^^^^^^
 * ```
 *
 * The gutter shows the line number, the offending source line is reproduced verbatim, and a
 * caret-underline marks the column range from the diagnostic. Multiple diagnostics are stacked with
 * a blank line between them.
 *
 * The formatter is deliberately tolerant of missing data — if [ScriptDiagnostic.location] is null,
 * we just print "error: message" without the source span; if `location.end` is null, we underline a
 * single column.
 */
class DiagnosticFormatter(private val color: Boolean = true) {

    /**
     * Renders all [diagnostics] at or above [minSeverity] against [source] (the script text the
     * user just submitted). Returns an empty string if nothing matches.
     */
    fun format(
        source: String,
        diagnostics: List<ScriptDiagnostic>,
        minSeverity: ScriptDiagnostic.Severity = ScriptDiagnostic.Severity.WARNING,
    ): String {
        val relevant = diagnostics.filter { it.severity >= minSeverity }
        if (relevant.isEmpty()) return ""
        val sourceLines = source.lines()
        return relevant.joinToString("\n\n") { formatOne(sourceLines, it) }
    }

    private fun formatOne(sourceLines: List<String>, diag: ScriptDiagnostic): String {
        val header =
            "${severityColor(diag.severity)}${BOLD}${severityLabel(diag.severity)}${reset()}: ${diag.message}"
        val loc = diag.location ?: return header

        val line = loc.start.line
        val startCol = loc.start.col
        val endCol = loc.end?.col?.takeIf { it > startCol } ?: (startCol + 1)
        val sourceLine = sourceLines.getOrNull(line - 1) ?: return header

        val lineLabel = line.toString()
        val gutter = " ${lineLabel} | "
        val padding = " ".repeat(lineLabel.length + 2) + "| "
        val underline = buildString {
            // Indent past the source line's leading content to land under the offending span.
            append(" ".repeat(maxOf(0, startCol - 1)))
            append("^".repeat(maxOf(1, endCol - startCol)))
        }

        return buildString {
            append(header).append('\n')
            append(blue()).append(gutter).append(reset()).append(sourceLine).append('\n')
            append(blue())
                .append(padding)
                .append(reset())
                .append(severityColor(diag.severity))
                .append(underline)
                .append(reset())
        }
    }

    private fun severityLabel(severity: ScriptDiagnostic.Severity): String =
        when (severity) {
            ScriptDiagnostic.Severity.FATAL -> "fatal"
            ScriptDiagnostic.Severity.ERROR -> "error"
            ScriptDiagnostic.Severity.WARNING -> "warning"
            ScriptDiagnostic.Severity.INFO -> "info"
            ScriptDiagnostic.Severity.DEBUG -> "debug"
        }

    private fun severityColor(severity: ScriptDiagnostic.Severity): String {
        if (!color) return ""
        return when (severity) {
            ScriptDiagnostic.Severity.FATAL,
            ScriptDiagnostic.Severity.ERROR -> RED
            ScriptDiagnostic.Severity.WARNING -> YELLOW
            else -> DIM
        }
    }

    private fun blue() = if (color) BLUE else ""

    private fun reset() = if (color) RESET else ""
}
