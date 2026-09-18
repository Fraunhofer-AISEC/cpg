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
package de.fraunhofer.aisec.cpg

import de.fraunhofer.aisec.cpg.frontends.TranslationException
import de.fraunhofer.aisec.cpg.graph.Component
import de.fraunhofer.aisec.cpg.graph.EOGStarterHolder
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.allChildrenWithOverlays
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnit
import de.fraunhofer.aisec.cpg.graph.firstParentOrNull
import de.fraunhofer.aisec.cpg.passes.ComponentPass
import de.fraunhofer.aisec.cpg.passes.EOGStarterPass
import de.fraunhofer.aisec.cpg.passes.Pass
import de.fraunhofer.aisec.cpg.passes.TranslationResultPass
import de.fraunhofer.aisec.cpg.passes.TranslationUnitPass
import de.fraunhofer.aisec.cpg.passes.configuration.PassOrderingHelper
import de.fraunhofer.aisec.cpg.passes.consumeTargets
import java.util.Collections
import java.util.IdentityHashMap
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("de.fraunhofer.aisec.cpg.PartialPassExecution")

/**
 * Re-runs exactly the passes/targets that [TranslationResult.dirtyNodes] says need re-running,
 * instead of re-running whole pass classes over the entire graph like
 * [de.fraunhofer.aisec.cpg.passes.executePassesSequentially] (the batch driver) does. This is the
 * counterpart used after incremental updates (see [TranslationManager.addSource] /
 * [updateIncrementally]), where only a small part of a large, already-analyzed graph changed.
 *
 * For each distinct dirty pass class, the correct execution order (respecting [DependsOn] /
 * [ExecuteBefore] / etc. between the *dirty* passes) is computed via [PassOrderingHelper],
 * mirroring the pattern already proven for single-pass execution in cpg-ai's `addRunPass`. For each
 * pass class, the dirty nodes are resolved to the actual granularity the pass operates on (walking
 * up the AST, exactly like cpg-ai's `runPassForNodeInternal`/`runPassForNode` do), deduplicated,
 * and handed to the very same [consumeTargets] used by the batch driver -- so `@ReplacePass` and
 * `@RequiresLanguage(Trait)` gating (both implemented inside `consumeTarget`) apply unchanged.
 *
 * This mirrors [de.fraunhofer.aisec.cpg.passes.executePassesSequentially]'s dirty/clean fixpoint
 * loop (including its [TranslationConfiguration.maxPassExecutions] guard against infinite loops),
 * but only ever touches the targets that actually contain dirty nodes.
 *
 * Note: the target-resolution logic below intentionally duplicates (in a simplified, single-node
 * form) the AST-walking logic in cpg-ai's `CpgAnalyzeTool.kt` (`runPassForNodeInternal`/
 * `runPassForNode`). Since cpg-ai depends on cpg-core (not the other way around), sharing it would
 * require moving that logic here and have cpg-ai call into it -- a reasonable follow-up, but out of
 * scope for this change.
 */
fun TranslationManager.runDirtyPasses(result: TranslationResult) {
    val ctx = result.finalCtx
    val maxExecutions = ctx.config.maxPassExecutions
    val executions = mutableMapOf<KClass<out Pass<out Node>>, Int>()

    fun currentDirtyPassClasses(): Set<KClass<out Pass<out Node>>> =
        result.dirtyNodes.values.flatten().toSet()

    val initiallyDirty = currentDirtyPassClasses()
    if (initiallyDirty.isEmpty()) {
        return
    }

    val orderedDirtyPasses =
        try {
            PassOrderingHelper(initiallyDirty.toList()).order().flatten().filter {
                it in initiallyDirty
            }
        } catch (e: ConfigurationException) {
            log.warn(
                "Could not determine a valid pass order for the dirty pass set {}; falling back " +
                    "to an unordered execution. Results may be incorrect if these passes have " +
                    "dependencies on each other.",
                initiallyDirty.map { it.simpleName },
                e,
            )
            initiallyDirty.toList()
        }

    val queue = ArrayDeque<KClass<out Pass<out Node>>>()
    queue.addAll(orderedDirtyPasses)

    while (queue.isNotEmpty()) {
        val passClass = queue.removeFirst()

        val numExec = executions[passClass] ?: 0
        if (numExec >= maxExecutions) {
            TranslationManager.log.warn(
                "Pass {} reached max executions, skipping",
                passClass.simpleName,
            )
            continue
        }

        val dirtyForPass =
            result.dirtyNodes.entries.filter { (_, passes) -> passClass in passes }.map { it.key }
        if (dirtyForPass.isEmpty()) {
            // Nothing left to do for this pass class (e.g. already cleaned up as a side effect of
            // an earlier pass in this same run).
            continue
        }

        runPassOnDirtyNodes(passClass, ctx, result, dirtyForPass)
        executions[passClass] = numExec + 1

        // Defensive: not every pass is guaranteed to call `markClean` on every dirty node it was
        // handed (e.g. if the node did not resolve to a target at all), so clear the entries we
        // just attempted here as well to avoid looping forever on them.
        for (node in dirtyForPass) {
            result.markClean(node, passClass)
        }

        val scheduledPasses = currentDirtyPassClasses()
        for (scheduledPass in scheduledPasses) {
            if (scheduledPass in queue) {
                continue
            }
            queue.addFirst(scheduledPass)
        }

        if (result.isCancelled) {
            TranslationManager.log.warn("Analysis interrupted, stopping Pass evaluation")
            break
        }
    }
}

/**
 * Resolves [dirtyNodes] to the actual targets [cls] operates on, deduplicates them (by identity),
 * and runs [cls] on exactly those targets via [consumeTargets].
 */
@Suppress("USELESS_CAST")
private fun runPassOnDirtyNodes(
    cls: KClass<out Pass<out Node>>,
    ctx: TranslationContext,
    result: TranslationResult,
    dirtyNodes: Collection<Node>,
) {
    val prototype =
        cls.primaryConstructor?.call(ctx)
            ?: throw TranslationException("Could not create prototype pass")

    val seen = Collections.newSetFromMap(IdentityHashMap<Node, Boolean>())

    when (prototype) {
        is TranslationResultPass -> {
            val targets =
                dirtyNodes.flatMap { resolveTargets<TranslationResult>(it) }.filter { seen.add(it) }
            if (targets.isNotEmpty()) {
                consumeTargets((prototype as TranslationResultPass)::class, ctx, targets, result)
            }
        }
        is ComponentPass -> {
            val targets =
                dirtyNodes.flatMap { resolveTargets<Component>(it) }.filter { seen.add(it) }
            if (targets.isNotEmpty()) {
                consumeTargets((prototype as ComponentPass)::class, ctx, targets, result)
            }
        }
        is TranslationUnitPass -> {
            val targets =
                dirtyNodes.flatMap { resolveTargets<TranslationUnit>(it) }.filter { seen.add(it) }
            if (targets.isNotEmpty()) {
                consumeTargets((prototype as TranslationUnitPass)::class, ctx, targets, result)
            }
        }
        is EOGStarterPass -> {
            val targets =
                dirtyNodes.flatMap { resolveEOGStarterTargets(it) }.filter { seen.add(it) }
            if (targets.isNotEmpty()) {
                consumeTargets((prototype as EOGStarterPass)::class, ctx, targets, result)
            }
        }
    }

    prototype.finalCleanup()
}

/**
 * Resolves [node] to the nearest enclosing (or, failing that, contained) node of type [T],
 * mirroring cpg-ai's `runPassForNodeInternal`: the node itself if it already is a [T], otherwise
 * its nearest [T] ancestor, otherwise all [T] descendants.
 *
 * Known limitation: for [EOGStarterPass]es (see [resolveEOGStarterTargets]), which operate at
 * function granularity, this genuinely narrows the work to the dirty function(s). For
 * [ComponentPass]/[TranslationUnitPass]/[TranslationResultPass] granularity, though, [T] here is
 * [Component]/[TranslationUnit]/[TranslationResult] -- once *any* node inside one of those is
 * dirty, the *entire* target (e.g. the whole [Component], every translation unit and function in
 * it) is handed to [consumeTargets], which calls the pass's `accept()` on it exactly like the batch
 * driver would. Dirty-marking at this granularity only avoids rerunning *unaffected* components/
 * translation units, not unaffected parts *within* an affected one -- the pass itself (e.g.
 * [de.fraunhofer.aisec.cpg.passes.DFGPass]) has no notion of "only visit the dirty subset" and
 * re-walks everything it is given. Making those passes genuinely dirty-aware internally is future
 * work and out of scope here (it would require changing pass analysis logic, not just this driver).
 */
private inline fun <reified T : Node> resolveTargets(node: Node): List<T> {
    (node as? T)?.let {
        return listOf(it)
    }
    node.firstParentOrNull<T>()?.let {
        return listOf(it)
    }
    return node.allChildrenWithOverlays<T>()
}

/**
 * Resolves [node] to the nearest enclosing (or, failing that, contained) [EOGStarterHolder] with no
 * incoming EOG edges, mirroring cpg-ai's `runPassForNode`'s handling of [EOGStarterPass].
 */
private fun resolveEOGStarterTargets(node: Node): List<Node> {
    ((node as? EOGStarterHolder) as? Node)?.let {
        return listOf(it)
    }
    node
        .firstParentOrNull<Node> { it is EOGStarterHolder && it.prevEOG.isEmpty() }
        ?.let {
            return listOf(it)
        }
    return node.allChildrenWithOverlays<Node> { it is EOGStarterHolder && it.prevEOG.isEmpty() }
}
