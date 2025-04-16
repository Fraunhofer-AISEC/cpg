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

import com.squareup.kotlinpoet.ARRAY
import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.DOUBLE
import com.squareup.kotlinpoet.FLOAT
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.MUTABLE_MAP
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.SHORT
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import de.fraunhofer.aisec.cpg.models.ClassAbstractRepresentation
import de.fraunhofer.aisec.cpg.models.Properties
import java.io.File
import java.io.FileWriter
import java.io.IOException

fun writeKotlinClassesToFolder(
    sources: List<ClassAbstractRepresentation>,
    outputBase: String,
    owl3: OWLCloudOntologyReader,
) {
    var filepath: String
    for (ktSource in sources) {
        filepath = outputBase + "/" + ktSource.name + ".kt"
        val f = File(filepath)
        val directory = f.parentFile
        if (!directory.exists()) {
            if (!directory.mkdirs()) {
                println("Could not create base directory for file $outputBase")
            }
        }
        try {
            val fileWriter = FileWriter(f)
            fileWriter.write(createKtSourceCodeString(ktSource, owl3, sources))
            fileWriter.close()
            println("File written to: $filepath")
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}

private fun createKtSourceCodeString(
    ktSource: ClassAbstractRepresentation,
    owl3: OWLCloudOntologyReader,
    otherClassAbstractions: List<ClassAbstractRepresentation>,
): String {
    if (ktSource.name == "owl:Thing") {
        return ""
    }

    // build parent class in order to extend from it
    // Get the parent interface/class using its fully-qualified name.
    var classBuilder = TypeSpec.classBuilder(ktSource.name)
    if (ktSource.parentClass != "" && ktSource.parentClass != "owl:Thing") {
        val parentClass = ClassName("de.fraunhofer.aisec.cpg.graph", ktSource.parentClass)
        classBuilder = TypeSpec.classBuilder(ktSource.name).addSuperinterface(parentClass)
    }

    // Create a constructor builder for adding parameters.
    val constructorBuilder = FunSpec.constructorBuilder()

    // writes own class properties into the constructor
    addPropertiesToClass(
        ktSource.dataProperties,
        ktSource.objectProperties,
        constructorBuilder,
        classBuilder,
        ktSource.name,
    )

    // Retrieve parents recursively and add them to the class.
    findAllParentsAndSaveToClassAbstraction(ktSource, otherClassAbstractions)

    // Now we have to add each object and data property of all parents to the constructor of
    // ktSource
    for (parent in ktSource.allParents) {
        // Write properties of parents into the constructor
        addPropertiesToClass(
            parent.dataProperties,
            parent.objectProperties,
            constructorBuilder,
            classBuilder,
            ktSource.name,
        )
    }

    // Set the primary constructor for the class.
    classBuilder.primaryConstructor(constructorBuilder.build())

    // Build the final file (for example, writing to System.out).TODO: Change com.example to the
    // correct package name
    val file = FileSpec.builder("com.example", ktSource.name).addType(classBuilder.build()).build()
    return file.toString()
}

private fun addPropertiesToClass(
    dataProperties: List<Properties>,
    objectProperties: List<Properties>,
    constructorBuilder: FunSpec.Builder,
    classBuilder: TypeSpec.Builder,
    className: String,
) {
    // Loop over the data properties.
    for (dataProp in dataProperties) {
        // Determine the correct TypeName based on the property type string.
        val typeName: TypeName = extractDataProperties(dataProp)

        if (typeName == ClassName("unknown", "unknown")) {
            println(
                "Unknown type: ${dataProp.propertyType} for property ${dataProp.propertyName} of class ${className}"
            )
        }

        // Add a constructor parameter for the property.
        constructorBuilder.addParameter(dataProp.propertyName, typeName)

        // Add a property to the class, initialized from the constructor parameter.
        classBuilder.addProperty(
            PropertySpec.builder(dataProp.propertyName, typeName)
                .initializer(dataProp.propertyName)
                .build()
        )
    }

    // Loop over the object properties.
    for (objectProp in objectProperties) {
        val typeName = extractObjectProperties(objectProp)

        if (typeName == ClassName("unknown", "unknown")) {
            println("Unknown type: ${objectProp.propertyType}")
        }

        // Add a constructor parameter for the property.
        constructorBuilder.addParameter(objectProp.propertyName, typeName)

        // Add a property to the class, initialized from the constructor parameter.
        classBuilder.addProperty(
            PropertySpec.builder(objectProp.propertyName, typeName)
                .initializer(objectProp.propertyName)
                .build()
        )
    }
}

private fun findAllParentsAndSaveToClassAbstraction(
    abstraction: ClassAbstractRepresentation,
    classAbstractions: List<ClassAbstractRepresentation>,
): ClassAbstractRepresentation? {
    // Get all parents of abstraction and parents of parents...
    for (clazz in classAbstractions) {
        if (clazz.name == abstraction.parentClass) {
            var parentClass = classAbstractions.find { it.name == clazz.name }
            if (parentClass != null) {
                var parentsOfParent =
                    findAllParentsAndSaveToClassAbstraction(parentClass, classAbstractions)
                if (parentsOfParent != null) {
                    abstraction.allParents += parentsOfParent
                }
            }

            abstraction.allParents += clazz
            println("All parents of ${abstraction.name} are: ")
            for (parent in abstraction.allParents) {
                print("${parent.name} ")
            }
            println()
            return clazz
        }
    }
    return null
}

private fun extractDataProperties(dataProp: Properties): TypeName {
    val typeName: TypeName =
        when (dataProp.propertyType) {
            "java.time.Duration" -> ClassName("java.time", "Duration")
            "java.time.ZonedDateTime" -> ClassName("java.time", "ZonedDateTime")
            "Short" -> SHORT
            "String" -> STRING
            "int" -> INT
            "Double" -> DOUBLE
            "Float",
            "float" -> FLOAT
            "boolean" -> BOOLEAN
            "de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration" ->
                ClassName("de.fraunhofer.aisec.cpg.graph.declarations", "FunctionDeclaration")
            "de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression" ->
                ClassName("de.fraunhofer.aisec.cpg.graph.statements.expressions", "Expression")
            "de.fraunhofer.aisec.cpg.graph.Node" ->
                ClassName("de.fraunhofer.aisec.cpg.graph", "Node")
            "de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression" ->
                ClassName("de.fraunhofer.aisec.cpg.graph.statements.expressions", "CallExpression")
            "java.util.List<de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration>" ->
                ClassName("java.util", "List")
                    .parameterizedBy(
                        ClassName(
                            "de.fraunhofer.aisec.cpg.graph.declarations",
                            "TranslationUnitDeclaration",
                        )
                    )
            "java.util.List<de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression>" ->
                ClassName("java.util", "List")
                    .parameterizedBy(
                        ClassName(
                            "de.fraunhofer.aisec.cpg.graph.statements.expressions",
                            "CallExpression",
                        )
                    )
            "java.util.Map<String, String>" -> MUTABLE_MAP.parameterizedBy(STRING, STRING)
            "java.util.ArrayList<String>" -> ARRAY.parameterizedBy(STRING)
            "java.util.ArrayList<Short>" -> ARRAY.parameterizedBy(SHORT)

            "dateTime" -> ClassName("java.time", "ZonedDateTime") // TODO: Check if this is correct
            "listString" ->
                ClassName("java.util", "List")
                    .parameterizedBy(STRING) // TODO: Check if this is correct
            else -> ClassName("unknown", "unknown")
        }
    return typeName
}

private fun extractObjectProperties(objectProp: Properties): TypeName {
    val typeName =
        when (objectProp.propertyType) {
            "ApplicationLogging" -> ClassName("de.fraunhofer.aisec.cpg.graph", "ApplicationLogging")
            "Functionality" -> ClassName("de.fraunhofer.aisec.cpg.graph", "Functionality")
            "Compute" -> ClassName("de.fraunhofer.aisec.cpg.graph", "Compute")
            "GeoLocation" -> ClassName("de.fraunhofer.aisec.cpg.graph", "GeoLocation")
            "[]Resource" ->
                ARRAY.parameterizedBy(ClassName("de.fraunhofer.aisec.cpg.graph", "Resource"))
            "NetworkInterface" -> ClassName("de.fraunhofer.aisec.cpg.graph", "NetworkInterface")
            "Image" -> ClassName("de.fraunhofer.aisec.cpg.graph", "Image")
            "ResourceLogging" -> ClassName("de.fraunhofer.aisec.cpg.graph", "ResourceLogging")
            "Container" -> ClassName("de.fraunhofer.aisec.cpg.graph", "Container")
            "DatabaseStorage" -> ClassName("de.fraunhofer.aisec.cpg.graph", "DatabaseStorage")
            "DatabaseService" -> ClassName("de.fraunhofer.aisec.cpg.graph", "DatabaseService")
            "Storage" -> ClassName("de.fraunhofer.aisec.cpg.graph", "Storage")
            "Authenticity" -> ClassName("de.fraunhofer.aisec.cpg.graph", "Authenticity")
            "TransportEncryption" ->
                ClassName("de.fraunhofer.aisec.cpg.graph", "TransportEncryption")
            "HttpEndpoint" -> ClassName("de.fraunhofer.aisec.cpg.graph", "HttpEndpoint")
            "Application" -> ClassName("de.fraunhofer.aisec.cpg.graph", "Application")
            "HttpRequestHandler" -> ClassName("de.fraunhofer.aisec.cpg.graph", "HttpRequestHandler")
            "Authorization" -> ClassName("de.fraunhofer.aisec.cpg.graph", "Authorization")
            "AccessRestriction" -> ClassName("de.fraunhofer.aisec.cpg.graph", "AccessRestriction")
            "NetworkService" -> ClassName("de.fraunhofer.aisec.cpg.graph", "NetworkService")
            "Logging" -> ClassName("de.fraunhofer.aisec.cpg.graph", "Logging")
            "LoggingService" -> ClassName("de.fraunhofer.aisec.cpg.graph", "LoggingService")
            "IsAuthenticity" -> ClassName("de.fraunhofer.aisec.cpg.graph", "IsAuthenticity")
            "ObjectStorage" -> ClassName("de.fraunhofer.aisec.cpg.graph", "ObjectStorage")
            "IsAtRestEncryption" -> ClassName("de.fraunhofer.aisec.cpg.graph", "IsAtRestEncryption")
            "Immutability" -> ClassName("de.fraunhofer.aisec.cpg.graph", "Immutability")
            "BlockStorage" -> ClassName("de.fraunhofer.aisec.cpg.graph", "BlockStorage")
            "AutomaticUpdates" -> ClassName("de.fraunhofer.aisec.cpg.graph", "AutomaticUpdates")
            "BootLogging" -> ClassName("de.fraunhofer.aisec.cpg.graph", "BootLogging")
            "MalwareProtection" -> ClassName("de.fraunhofer.aisec.cpg.graph", "MalwareProtection")
            "OSLogging" -> ClassName("de.fraunhofer.aisec.cpg.graph", "OSLogging")
            "Redundancy" -> ClassName("de.fraunhofer.aisec.cpg.graph", "Redundancy")
            "UsageStatistics" -> ClassName("de.fraunhofer.aisec.cpg.graph", "UsageStatistics")
            "EncryptionInUse" -> ClassName("de.fraunhofer.aisec.cpg.graph", "EncryptionInUse")
            "RemoteAttestation" -> ClassName("de.fraunhofer.aisec.cpg.graph", "RemoteAttestation")
            "ActivityLogging" -> ClassName("de.fraunhofer.aisec.cpg.graph", "ActivityLogging")
            "Resource" -> ClassName("de.fraunhofer.aisec.cpg.graph", "Resource")
            "ServiceMetadataDocument" ->
                ClassName("de.fraunhofer.aisec.cpg.graph", "ServiceMetadataDocument")
            "Backup" -> ClassName("de.fraunhofer.aisec.cpg.graph", "Backup")
            "CodeModule" -> ClassName("de.fraunhofer.aisec.cpg.graph", "CodeModule")
            "CodeRepository" -> ClassName("de.fraunhofer.aisec.cpg.graph", "CodeRepository")
            "DataLocation" -> ClassName("de.fraunhofer.aisec.cpg.graph", "DataLocation")
            "SchemaValidation" -> ClassName("de.fraunhofer.aisec.cpg.graph", "SchemaValidation")
            "DocumentChecksum" -> ClassName("de.fraunhofer.aisec.cpg.graph", "DocumentChecksum")
            "SecurityFeature" -> ClassName("de.fraunhofer.aisec.cpg.graph", "SecurityFeature")
            "DocumentSignature" -> ClassName("de.fraunhofer.aisec.cpg.graph", "DocumentSignature")
            "SecurityAdvisoryFeed" ->
                ClassName("de.fraunhofer.aisec.cpg.graph", "SecurityAdvisoryFeed")
            "CipherSuite" -> ClassName("de.fraunhofer.aisec.cpg.graph", "CipherSuite")
            "Error" -> ClassName("de.fraunhofer.aisec.cpg.graph", "Error") // TODO: recheck
            "AnomalyDetection" -> ClassName("de.fraunhofer.aisec.cpg.graph", "AnomalyDetection")
            "CodeRegion" -> ClassName("de.fraunhofer.aisec.cpg.graph", "CodeRegion")
            "IsAccessRestriction" ->
                ClassName("de.fraunhofer.aisec.cpg.graph", "IsAccessRestriction")
            "MachineLearning" -> ClassName("de.fraunhofer.aisec.cpg.graph", "MachineLearning")
            "Key" -> ClassName("de.fraunhofer.aisec.cpg.graph", "Key")
            "Vulnerability" -> ClassName("de.fraunhofer.aisec.cpg.graph", "Vulnerability")
            "Infrastructure" -> ClassName("de.fraunhofer.aisec.cpg.graph", "Infrastructure")
            "AtRestEncryption" -> ClassName("de.fraunhofer.aisec.cpg.graph", "AtRestEncryption")
            "Library" -> ClassName("de.fraunhofer.aisec.cpg.graph", "Library")
            "Credential" -> ClassName("de.fraunhofer.aisec.cpg.graph", "Credential")
            "Function" -> ClassName("de.fraunhofer.aisec.cpg.graph", "Function")
            "IsTransportEncryption" ->
                ClassName("de.fraunhofer.aisec.cpg.graph", "IsTransportEncryption")
            "SecurityAdvisoryDocument" ->
                ClassName("de.fraunhofer.aisec.cpg.graph", "SecurityAdvisoryDocument")

            else -> ClassName("unknown", "unknown")
        }
    return typeName
}
