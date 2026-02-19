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
package de.fraunhofer.aisec.cpg

import java.io.File
import org.semanticweb.owlapi.model.OWLOntologyCreationException

object SemanticNodeGenerator {
    @Throws(OWLOntologyCreationException::class)
    @JvmStatic
    fun main(args: Array<String>) {

        var outputBaseGo = "output/go/"
        var packageNameGo = "voc"
        var outputBaseJava = "output/java/"
        var packageNameJava = "de.fraunhofer.aisec.cpg.graph.concepts.ontology"
        var outputBaseKotlin =
            "cpg-concepts/src/main/kotlin/de/fraunhofer/aisec/cpg/graph/concepts/ontology"
        var packageNameKotlin = "de.fraunhofer.aisec.cpg.graph.concepts.ontology"
        val resourceNameFromOwlFile = "Resource"

        // IMPORTANT: Only OWL/XML and RDF/XML are supported

        var owlInputPath = "external/security-metrics/ontology/v1/ontology-merged.owx"

        val file = File(owlInputPath)
        if (!file.exists()) {
            println("File not found at ${file.absolutePath}")
        } else {
            println("File found at ${file.absolutePath}")
        }

        if (args.isEmpty()) {
            print(
                """
    Please use the following parameters:  
    
    1st parameter: Ontology Input File (Only OWL/XML and RDF/XML are supported)2st parameter: Java package name
    3nd parameter: Output path for generated Java files (optional, but the order must be respected)
    4th parameter: Go package name
    5th parameter: Output path for generated Go Files (optional, but the order must be respected)
    """
                    .trimIndent()
            )
        }
        when (args.size) {
            5 -> {
                owlInputPath = args[0]
                packageNameJava = args[1]
                outputBaseJava = checkPath(args[2])
                packageNameGo = args[3]
                outputBaseGo = checkPath(args[4])
            }
            4 -> {
                owlInputPath = args[0]
                packageNameJava = args[1]
                outputBaseJava = checkPath(args[2])
                packageNameGo = args[3]
            }
            3 -> {
                owlInputPath = args[0]
                packageNameJava = args[1]
                outputBaseJava = checkPath(args[2])
            }
            2 -> {
                owlInputPath = args[0]
                packageNameJava = args[1]
            }
            1 -> {
                owlInputPath = args[0]
            }
        }

        // Clear contents of previous output folders to guarantee full rebuild
        java.io.File(outputBaseJava).deleteRecursively()
        java.io.File(outputBaseGo).deleteRecursively()
        java.io.File(outputBaseKotlin).deleteRecursively()

        val owl3 = OWLCloudOntologyReader(owlInputPath, resourceNameFromOwlFile)

        // Create java class sources
        val jcs = owl3.getJavaClassSources(packageNameJava)
        // disabled Java code generation
        // writeJavaClassesToFolder(jcs, outputBaseJava)

        // Create Go and Kotlin sources
        val ontologyDescription = owl3.getAbstractRepresentationOfOWL(packageNameKotlin)
        // disabled Go code generation
        // writeGoStringsToFolder(ontologyDescription, outputBaseGo, owl3)

        // Create Kotlin classes
        writeKotlinClassesToFolder(ontologyDescription, outputBaseKotlin, owl3)
    }

    private fun checkPath(outputBase: String): String {
        var tmpOutputBase = outputBase
        return if (tmpOutputBase[tmpOutputBase.length - 1] != '/')
            "/"
                .let {
                    tmpOutputBase += it
                    tmpOutputBase
                }
        else tmpOutputBase
    }
}
