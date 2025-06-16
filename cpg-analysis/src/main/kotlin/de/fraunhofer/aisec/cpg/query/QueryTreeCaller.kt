/*
 * Copyright (c) 2022, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.query

/**
 * Represents information about the caller method of a QueryTree
 *
 * @property className The name of the class containing the method
 * @property methodName The name of the method
 * @property fileName The name of the file containing the method
 * @property lineNumber The line number of the method call
 */
data class CallerInfo(
    val className: String,
    val methodName: String,
    val fileName: String,
    val lineNumber: Int,
)

/**
 * Retrieves information about the method that invokes a QueryTree by analyzing the current stack
 * trace. This function looks for the first FlowQueriesKt method from the bottom of the stack trace
 * and returns the caller method that appears just before it.
 *
 * @return CallerInfo containing class, method and file name, or null if unable to determine the
 *   caller
 */
fun getQueryTreeCaller(): CallerInfo? {
    // Filter out lambda functions from the stacktrace first
    val stackTrace =
        Thread.currentThread().stackTrace.filter { !it.methodName.contains("\$lambda") }

    // Find the index of the first reference to a query tree method from the bottom
    val flowQueriesIndex =
        stackTrace.indexOfLast {
            it.className.contains("FlowQueriesKt") || it.className.contains("QueryTreeKt")
        }

    if (flowQueriesIndex <= 0) {
        return null
    }

    // Return the element just before the FlowQueriesKt method
    val element = stackTrace[flowQueriesIndex + 1]

    return CallerInfo(
        className = element.className,
        methodName = element.methodName,
        fileName = element.fileName ?: "Unknown",
        lineNumber = element.lineNumber,
    )
}
