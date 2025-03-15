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
package de.fraunhofer.aisec.codyze.compliance

import de.fraunhofer.aisec.codyze.*
import de.fraunhofer.aisec.cpg.TranslationResult
import io.github.detekt.sarif4k.MultiformatMessageString
import io.github.detekt.sarif4k.ReportingDescriptor
import io.github.detekt.sarif4k.Result

/** Loads the security goals from the project directory. */
fun AnalysisProject.loadSecurityGoals(): List<SecurityGoal> {
    return securityGoalsFolder?.let { loadSecurityGoals(it) } ?: listOf()
}

/**
 * Executes the security goals queries and returns the security goals as SARIF rules and the query
 * results as SARIF results.
 */
fun AnalysisProject.executeSecurityGoalsQueries(
    tr: TranslationResult
): Pair<List<ReportingDescriptor>, List<Result>> {
    val rules = mutableListOf<ReportingDescriptor>()
    val results = mutableListOf<Result>()
    val goals = loadSecurityGoals()

    // Connect the security goals to the translation result for now. Later we will add them
    // to individual concepts
    for (goal in goals) {
        goal.underlyingNode = tr

        // Load and execute queries associated to the goals
        for (objective in goal.objectives) {
            val objectiveID = objective.name.localName.lowercase().replace(" ", "-")
            objective.underlyingNode = tr

            projectDir?.let {
                val scriptFile = it.resolve("queries").resolve("${objectiveID}.query.kts")
                for (stmt in objective.statements.withIndex()) {
                    val idx1 = stmt.index + 1
                    val statementID = "statement${idx1}"
                    val rule =
                        ReportingDescriptor(
                            id = "${objectiveID}-${statementID}",
                            name = "${objective.name.localName}: Statement $idx1",
                            shortDescription = MultiformatMessageString(text = stmt.value),
                        )
                    val queryResult = tr.evalQuery(scriptFile.toFile(), statementID, rule.id)
                    results += queryResult.sarif

                    rules += rule
                }
            }
        }
    }

    return Pair(rules, results)
}
