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

import de.fraunhofer.aisec.cpg.analysis.abstracteval.LatticeInterval
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.query.QueryTree

private const val ESC = "\u001B"
private const val OSC8 = "$ESC]8;;"
private const val ST = "$ESC\\"
private const val RESET = "$ESC[0m"
private const val DIM = "$ESC[2m"
private const val BOLD = "$ESC[1m"
private const val CYAN = "$ESC[36m"
private const val GREEN = "$ESC[32m"
private const val RED = "$ESC[31m"

/**
 * URL scheme used for the clickable file:line hyperlinks emitted by [NodeLinkRenderer].
 *
 * Different terminals/editors honor different schemes:
 * * [VSCODE] (`vscode://file<path>:<line>:<col>`) — VS Code's own URL handler always navigates to
 *   the exact line/column. Works whenever VS Code is the default handler for the scheme — i.e. when
 *   running inside VS Code's integrated terminal, or when the user has VS Code installed and
 *   configured as the `vscode://` handler.
 * * [FILE] (`file://<path>#L<line>`) — universal but most OS handlers (macOS `open`, `xdg-open`)
 *   strip the `#L<line>` fragment, so the editor opens at line 1.
 * * [NONE] — disables the hyperlink wrapper entirely; only plain `file:line` text.
 *
 * The default is auto-detected from `TERM_PROGRAM` (set by most modern terminals) and can be
 * overridden with the `CODYZE_LINK_SCHEME` env var.
 */
enum class LinkScheme {
    FILE,
    VSCODE,
    NONE;

    fun urlFor(path: String, line: Int, column: Int): String? =
        when (this) {
            NONE -> null
            FILE -> "file://$path#L$line"
            VSCODE -> "vscode://file$path:$line${if (column > 0) ":$column" else ""}"
        }

    companion object {
        /**
         * Pick a scheme from env vars. Explicit `CODYZE_LINK_SCHEME` wins; otherwise detect from
         * `TERM_PROGRAM` (vscode → VSCODE); otherwise fall back to FILE.
         */
        fun detect(env: (String) -> String? = System::getenv): LinkScheme {
            env("CODYZE_LINK_SCHEME")?.let {
                return when (it.lowercase()) {
                    "vscode" -> VSCODE
                    "file" -> FILE
                    "none" -> NONE
                    else -> FILE
                }
            }
            return when (env("TERM_PROGRAM")) {
                "vscode" -> VSCODE
                else -> FILE
            }
        }
    }
}

/**
 * Renders REPL eval results for terminal output.
 *
 * Single [Node] values and collections of nodes get formatted with OSC 8 hyperlinks pointing to the
 * configured [LinkScheme]. The link is clickable in modern terminals (iTerm2, VS Code integrated
 * terminal, Ghostty, WezTerm) — the OS handler for the scheme decides what happens on click.
 *
 * If [color] is `false`, all ANSI escapes (including the OSC 8 hyperlink wrapper) are stripped so
 * the same renderer works in non-TTY/CI output.
 */
class NodeLinkRenderer(
    private val color: Boolean = true,
    private val linkScheme: LinkScheme = LinkScheme.detect(),
) {

    fun render(value: Any?): String =
        when (value) {
            null -> "${dim()}null${reset()}"
            is QueryTree<*> -> renderQueryTree(value)
            is Node -> renderNode(value)
            is Collection<*> -> renderCollection(value)
            is Map<*, *> -> renderMap(value)
            is Array<*> -> renderCollection(value.toList())
            else -> value.toString()
        }

    /**
     * Renders a [QueryTree] as an indented tree using `├─`/`└─` connectors. Each line shows:
     * * a colored ✓/✗ for boolean values (other types just print their toString),
     * * the operator name (`EVALUATE`, `EXISTS`, …),
     * * the [QueryTree.stringRepresentation] if non-empty,
     * * a clickable file:line link to the associated [Node] when present.
     *
     * Children are rendered recursively with proper tree-line continuation so deeply nested results
     * stay readable. We cap each level at [MAX_ITEMS] siblings to avoid drowning the terminal on
     * wide trees.
     */
    private fun renderQueryTree(tree: QueryTree<*>): String = buildString {
        renderQueryTreeInto(this, tree, prefix = "", isLast = true, isRoot = true)
    }

    private fun renderQueryTreeInto(
        out: StringBuilder,
        tree: QueryTree<*>,
        prefix: String,
        isLast: Boolean,
        isRoot: Boolean,
    ) {
        val connector =
            when {
                isRoot -> ""
                isLast -> "└─ "
                else -> "├─ "
            }
        out.append(dim()).append(prefix).append(connector).append(reset())

        // Value badge: ✓/✗ for booleans, the full node renderer for Node values, a recursive
        // walk for Collection-of-Node (so each step of a flow path renders with its file:line
        // link), otherwise just toString.
        renderTreeValue(out, tree.value, prefix, isRoot, isLast)

        // Operator label + human-readable summary.
        out.append(' ')
            .append(dim())
            .append('[')
            .append((tree.operator as? Enum<*>)?.name ?: tree.operator.toString())
            .append(']')
            .append(reset())
        // Show stringRepresentation only when it isn't the auto-generated noise that
        // embeds a full Node toString — those substrings (`[name=…]`, `[location=…]`) hide
        // any useful info under a wall of class internals. User-written strings like
        // "overflow risk" / "ok" pass through and add real context.
        val repr = tree.stringRepresentation.trim()
        if (repr.isNotEmpty() && !repr.contains("[name=") && !repr.contains("[location=")) {
            out.append(' ').append(repr.take(120))
        }
        tree.node?.let { node ->
            val loc = formatLocation(node)
            if (loc.isNotEmpty()) out.append("  ").append(loc)
        }

        val childPrefix = prefix + if (isRoot) "" else if (isLast) "   " else "│  "
        val children = tree.children.take(MAX_ITEMS)
        val truncated = tree.children.size - children.size
        children.forEachIndexed { i, child ->
            out.append('\n')
            renderQueryTreeInto(
                out,
                child,
                childPrefix,
                isLast = (i == children.size - 1 && truncated == 0),
                isRoot = false,
            )
        }
        if (truncated > 0) {
            out.append('\n')
                .append(dim())
                .append(childPrefix)
                .append("… and $truncated more")
                .append(reset())
        }
    }

    private fun renderTreeValue(
        out: StringBuilder,
        value: Any?,
        prefix: String,
        isRoot: Boolean,
        isLast: Boolean,
    ) {
        when (value) {
            is Boolean ->
                if (value) out.append(green()).append("✓").append(reset())
                else out.append(red()).append("✗").append(reset())
            is Node -> out.append(renderNode(value))
            is LatticeInterval -> out.append(renderInterval(value))
            is Collection<*> ->
                if (value.isNotEmpty() && value.all { it is Node }) {
                    // Render Node-collections as nested numbered steps under this tree line.
                    out.append(dim()).append("(${value.size} node(s))").append(reset())
                    val childPrefix = prefix + if (isRoot) "" else if (isLast) "   " else "│  "
                    value.forEachIndexed { i, item ->
                        if (item is Node) {
                            out.append('\n')
                                .append(dim())
                                .append(childPrefix)
                                .append(if (i == value.size - 1) "└─ " else "├─ ")
                                .append("step ${i + 1}: ")
                                .append(reset())
                                .append(renderNode(item))
                        }
                    }
                } else {
                    out.append(bold()).append(value.toString().take(120)).append(reset())
                }
            else -> out.append(bold()).append(value.toString().take(120)).append(reset())
        }
    }

    private fun renderNode(node: Node): String {
        val name = node.name.localName.ifEmpty { node.javaClass.simpleName }
        val kind = node.javaClass.simpleName
        val location = formatLocation(node)
        val tags = nodeTags(node)
        return buildString {
            append(bold()).append(name).append(reset())
            append(dim()).append(" : ").append(kind).append(reset())
            if (tags.isNotEmpty()) {
                append(' ').append(dim()).append(tags).append(reset())
            }
            if (location.isNotEmpty()) {
                append("  ").append(location)
            }
        }
    }

    private fun renderCollection(items: Collection<*>): String {
        if (items.isEmpty()) return "${dim()}[]${reset()}"
        val capped = items.take(MAX_ITEMS)
        val remainder = items.size - capped.size
        return buildString {
            append(dim()).append("(${items.size} items)").append(reset()).append('\n')
            capped.forEachIndexed { i, item ->
                append(dim()).append("  [$i] ").append(reset())
                append(render(item))
                if (i < capped.size - 1 || remainder > 0) append('\n')
            }
            if (remainder > 0) {
                append(dim()).append("  … and $remainder more").append(reset())
            }
        }
    }

    private fun renderMap(map: Map<*, *>): String {
        if (map.isEmpty()) return "${dim()}{}${reset()}"
        return buildString {
            append(dim()).append("{").append(reset()).append('\n')
            map.entries.take(MAX_ITEMS).forEach { (k, v) ->
                append("  ").append(k).append(" -> ").append(render(v)).append('\n')
            }
            if (map.size > MAX_ITEMS) {
                append(dim()).append("  … and ${map.size - MAX_ITEMS} more\n").append(reset())
            }
            append(dim()).append("}").append(reset())
        }
    }

    /**
     * Collapses auto-generated `stringRepresentation` text into something humans can scan.
     *
     * Many `QueryTree`s are built from helpers (e.g. `sizeof(node)`) that include the node's full
     * `toString()` — `Reference[name=p,location=…,type=PointerType[name=char*],refersTo=
     * Variable[…]]` — which is mostly noise once the tree already shows a file:line link. We
     * rewrite occurrences of `ClassName[name=foo, … nested junk …]` to just `ClassName 'foo'`,
     * walking matched bracket depth so we skip the nested portion correctly.
     */
    private fun condenseRepr(repr: String): String {
        if (repr.isBlank()) return ""
        val trimmed = repr.trim()
        // Find every `ClassName[...]` blob (with possible nested brackets), pull out an
        // identifier from inside (preferring `name=`, falling back to `value=`), and rewrite
        // the whole blob to `ClassName 'identifier'`. Bare `ClassName[…]` with no usable
        // key is dropped to `ClassName`.
        val classStart = Regex("""\b(\w+)\[""")
        val sb = StringBuilder()
        var i = 0
        while (i < trimmed.length) {
            val match =
                classStart.find(trimmed, i)
                    ?: run {
                        sb.append(trimmed, i, trimmed.length)
                        return@condenseRepr sb.toString()
                    }
            sb.append(trimmed, i, match.range.first)
            val cls = match.groupValues[1]
            // Find the matching closing bracket so we know the blob's extent.
            val open = match.range.last
            var j = open + 1
            var depth = 1
            while (j < trimmed.length && depth > 0) {
                when (trimmed[j]) {
                    '[' -> depth++
                    ']' -> depth--
                }
                if (depth == 0) break
                j++
            }
            val blob = trimmed.substring(open + 1, j) // contents between [ and ]
            val ident = extractIdent(blob)
            sb.append(cls)
            if (ident != null) sb.append(" '").append(ident).append('\'')
            i = j + 1
        }
        return sb.toString()
    }

    /**
     * Pull a short identifier out of a comma-separated `key=value, key=value` blob. Prefer `name=`
     * (variables/calls/references) over `value=` (literals). Returns null if neither is present.
     */
    private fun extractIdent(blob: String): String? {
        for (key in listOf("name=", "value=")) {
            val idx = blob.indexOf(key)
            if (idx < 0) continue
            var j = idx + key.length
            // Read up to next comma or end, ignoring nested brackets.
            var depth = 0
            val sb = StringBuilder()
            while (j < blob.length) {
                val c = blob[j]
                if (depth == 0 && c == ',') break
                if (c == '[') depth++
                if (c == ']') depth--
                sb.append(c)
                j++
            }
            val ident = sb.toString().trim()
            if (ident.isNotEmpty()) return ident
        }
        return null
    }

    /**
     * Pretty-prints a [LatticeInterval] as `8`, `[1, 64]`, `[0, ∞]`, `⊥` etc. Single-point
     * intervals collapse to just the number so the common case (`Bounded(8, 8)`) doesn't look like
     * a range. Bounded ranges show their `[lower, upper]` form so the audience can see what the
     * abstract evaluator narrowed the value to.
     */
    private fun renderInterval(interval: LatticeInterval): String =
        when (interval) {
            LatticeInterval.BOTTOM -> "${dim()}⊥${reset()}"
            is LatticeInterval.Bounded -> {
                val lo = renderBound(interval.lower)
                val up = renderBound(interval.upper)
                if (lo == up) "${bold()}$lo${reset()}" else "${bold()}[$lo, $up]${reset()}"
            }
        }

    private fun renderBound(b: LatticeInterval.Bound): String =
        when (b) {
            LatticeInterval.Bound.INFINITE -> "∞"
            LatticeInterval.Bound.NEGATIVE_INFINITE -> "-∞"
            is LatticeInterval.Bound.Value -> b.value.toString()
        }

    /**
     * Produces a parenthesized tag string like `(inferred, implicit)` for nodes that have flags set
     * indicating they weren't directly written by the user. We surface:
     * * `inferred` — the CPG synthesised the declaration (e.g. libc symbols seen via use)
     * * `implicit` — the CPG inserted this node for semantic completeness (e.g. compiler- generated
     *   calls); not present in the source text
     */
    private fun nodeTags(node: Node): String {
        val tags = buildList {
            if (node.isInferred) add("inferred")
            if (node.isImplicit) add("implicit")
        }
        return if (tags.isEmpty()) "" else tags.joinToString(", ", prefix = "(", postfix = ")")
    }

    private fun formatLocation(node: Node): String {
        val loc = node.location ?: return ""
        val uri = loc.artifactLocation.uri ?: return ""
        val line = loc.region.startLine.takeIf { it > 0 } ?: return uri.toString()
        val column = loc.region.startColumn.takeIf { it > 0 } ?: 0
        val path = uri.path ?: return uri.toString()
        val displayPath = path.substringAfterLast('/')
        val display = "$displayPath:$line"
        val target = linkScheme.urlFor(path, line, column) ?: return display
        return hyperlink(target, display)
    }

    private fun hyperlink(target: String, display: String): String {
        if (!color) return display
        return OSC8 + target + ST + CYAN + display + RESET + OSC8 + ST
    }

    private fun dim() = if (color) DIM else ""

    private fun bold() = if (color) BOLD else ""

    private fun reset() = if (color) RESET else ""

    private fun green() = if (color) GREEN else ""

    private fun red() = if (color) RED else ""

    companion object {
        const val MAX_ITEMS = 50
    }
}
