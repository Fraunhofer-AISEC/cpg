/*
 * Copyright (c) 2021, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.frontends

import de.fraunhofer.aisec.cpg.sarif.Region
import kotlin.math.min
import org.slf4j.LoggerFactory

class FrontendUtils {

    companion object {

        private val LOGGER = LoggerFactory.getLogger(FrontendUtils::class.java)

        fun parseColumnPositionsFromFile(
            fileContent: String,
            nodeLength: Int,
            nodeOffset: Int,
            startingLineNumber: Int,
            endingLineNumber: Int
        ): Region? {
            // Get start column by stepping backwards from begin of node to first occurrence of
            // '\n'
            var startColumn = 1
            for (i in nodeOffset - 1 downTo 2) {
                if (i >= fileContent.length) {
                    // Fail gracefully, so that we can at least find out why this fails
                    LOGGER.warn(
                        "Requested index {} exceeds length of translation unit code ({})",
                        i,
                        fileContent.length
                    )
                    return null
                }
                if (fileContent[i] == '\n') {
                    break
                }
                startColumn++
            }

            val endColumn = getEndColumnIndex(fileContent, nodeOffset + nodeLength)
            val region = Region(startingLineNumber, startColumn, endingLineNumber, endColumn)
            return region
        }

        /**
         * Searches in posPrefix to the left until first occurrence of line break and returns the
         * number of characters.
         *
         * This corresponds to the column number of "end" within "posPrefix".
         *
         * @param posPrefix
         * - the positional prefix, which is the string before the column and contains the column
         * defining newline.
         */
        private fun getEndColumnIndex(posPrefix: String, end: Int): Int {
            var mutableEnd = end
            var column = 1

            // In case the current element goes until EOF, we need to back up "end" by one.
            try {
                if (mutableEnd - 1 >= posPrefix.length || posPrefix[mutableEnd - 1] == '\n') {
                    mutableEnd = min(mutableEnd - 1, posPrefix.length - 1)
                }
            } catch (e: ArrayIndexOutOfBoundsException) {
                LanguageFrontend.log.error("could not update end ", e)
            }
            for (i in mutableEnd - 1 downTo 2) {
                if (posPrefix[i] == '\n') {
                    break
                }
                column++
            }
            return column
        }
    }
}
