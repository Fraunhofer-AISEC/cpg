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
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.DOUBLE
import com.squareup.kotlinpoet.FLOAT
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MUTABLE_MAP
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.SHORT
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName
import de.fraunhofer.aisec.cpg.models.ClassAbstractRepresentation
import de.fraunhofer.aisec.cpg.models.Properties
import java.io.File
import java.io.FileWriter
import java.io.IOException
import kotlin.collections.plus
import org.jboss.forge.roaster._shade.org.eclipse.jdt.internal.compiler.ast.NullAnnotationMatching.CheckMode.OVERRIDE

fun writeKotlinClassesToFolder(
    sources: LinkedHashSet<ClassAbstractRepresentation>,
    outputBase: String,
    owl3: OWLCloudOntologyReader,
) {
    // This method adds the Concept class to the ontology, which is necessary for the root nodes to
    // extend from it.
    // The Concept class is not defined in the ontology, so we have to add it manually.
    // For Code generation this class will be ignored. Can be seen as a helper
    addConceptClassToOntology(sources)

    // remove the current version of Operation class and define a new one since the current version
    // is inheriting
    // from concept which is not intended
    sources.removeIf { it.name == "Operation" }
    // Add non-inheriting Operation class. This class will not be generated and ignored in
    // generation
    addOperationClassToOntology(sources)

    var filepath: String

    for (ktSource in sources) {
        findAllParentsAndSaveToClassAbstraction(ktSource, sources)
    }

    // sort all data props and object props of each class
    for (ktSource in sources) {
        ktSource.dataProperties =
            ktSource.dataProperties.sortedBy { it.propertyName }.toCollection(linkedSetOf())
        ktSource.objectProperties =
            ktSource.objectProperties.sortedBy { it.propertyName }.toCollection(linkedSetOf())
    }

    for (ktSource in sources) {

        // This is necessary to avoid writing the Concept class (which is already defined in another
        // package)
        if (ktSource.name.equals("Concept")) continue
        if (ktSource.name.equals("Operation")) continue

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

// This function is needed to add let the root nodes inherit from concept class since this is not
// reflected in the ontology
private fun addConceptClassToOntology(sources: LinkedHashSet<ClassAbstractRepresentation>) {
    // Create a new ClassAbstractRepresentation for the Concept class
    val conceptClass = ClassAbstractRepresentation(name = "Concept", parentClass = "")

    // Add the underlyingNode property to the Concept class
    conceptClass.objectProperties += Properties("Node", "underlyingNode")
    conceptClass.packageName = "de.fraunhofer.aisec.cpg.graph"

    sources.add(conceptClass)

    // Iterate over all sources and add the Concept class to every class which does not have a
    // parent class
    for (ktSource in sources) {
        if (
            (ktSource.parentClass == "" || ktSource.parentClass == "owl:Thing") &&
                ktSource.name != "Concept"
        ) {
            // If the class has no parent, add the Concept class as a parent
            ktSource.allParents += conceptClass
        }
    }
}

private fun addOperationClassToOntology(sources: LinkedHashSet<ClassAbstractRepresentation>) {
    // Create a new ClassAbstractRepresentation for the Operation class
    val operationClass = ClassAbstractRepresentation(name = "Operation", parentClass = "")

    // Add the underlyingNode property to the Operation class
    operationClass.objectProperties += Properties("Concept", "concept")
    operationClass.objectProperties += Properties("Node", "underlyingNode")
    operationClass.packageName = "de.fraunhofer.aisec.cpg.graph.concepts"

    sources.add(operationClass)
}

private fun createKtSourceCodeString(
    ktSource: ClassAbstractRepresentation,
    owl3: OWLCloudOntologyReader,
    otherClassAbstractions: LinkedHashSet<ClassAbstractRepresentation>,
): String {
    if (ktSource.name == "owl:Thing") {
        return ""
    }

    val fileSpecBuilder = FileSpec.builder(ktSource.packageName!!, ktSource.name)

    // build parent class in order to extend from it
    // Get the parent interface/class using its fully-qualified name.
    var classBuilder = TypeSpec.classBuilder(ktSource.name).addModifiers(KModifier.ABSTRACT)

    if (ktSource.parentClass != "" && ktSource.parentClass != "owl:Thing") {
        val parentClass =
            ClassName("de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated", ktSource.parentClass)
        classBuilder =
            TypeSpec.classBuilder(ktSource.name)
                .superclass(parentClass)
                .addModifiers(KModifier.ABSTRACT)
    }

    if (
        ktSource.allParents.find { it.name == "Concept" } != null && ktSource.allParents.size == 1
    ) {
        // All root nodes have to inherit from the Concept class
        classBuilder =
            TypeSpec.classBuilder(ktSource.name)
                .superclass(ClassName("de.fraunhofer.aisec.cpg.graph.concepts", "Concept"))
                .addModifiers(KModifier.ABSTRACT)
    }

    if (
        ktSource.allParents.find { it.name == "Operation" } != null && ktSource.allParents.size == 1
    ) {
        // All operation nodes have to inherit from the Operation class
        classBuilder =
            TypeSpec.classBuilder(ktSource.name)
                .superclass(ClassName("de.fraunhofer.aisec.cpg.graph.concepts", "Operation"))
                .addModifiers(KModifier.ABSTRACT)
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
        fileSpecBuilder,
    )

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
            fileSpecBuilder,
            isParent = true,
        )
    }

    // Set the primary constructor for the class.
    classBuilder.primaryConstructor(constructorBuilder.build())

    // If current class is a operation we have to add equals method and hashCode method
    if (ktSource.allParents.find { it.name == "Operation" } != null) {
        // TODO: Call here equals and hash code method
        addEqualsMethod(classBuilder, ktSource)
        addHashCodeMethod(classBuilder, ktSource)
    }

    // Build the final file
    val file = fileSpecBuilder.addType(classBuilder.build()).build()
    return file.toString()
}

private fun addEqualsMethod(classBuilder: TypeSpec.Builder, ktSource: ClassAbstractRepresentation) {
    val equalsMethodBuilder =
        FunSpec.builder("equals")
            .addModifiers(KModifier.OVERRIDE)
            .addParameter("other", Any::class.asTypeName().copy(nullable = true))
            .returns(Boolean::class)

    // Build the equals condition
    val conditions = mutableListOf<String>()
    conditions.add("other is ${ktSource.name}")
    conditions.add("super.equals(other)")

    // Add conditions for all data and object properties of the current class
    for (property in ktSource.dataProperties + ktSource.objectProperties) {
        conditions.add("other.${property.propertyName} == this.${property.propertyName}")
    }

    val returnStatement = "return " + conditions.joinToString(" &&\n            ")

    equalsMethodBuilder.addStatement(returnStatement)
    classBuilder.addFunction(equalsMethodBuilder.build())
}

private fun addHashCodeMethod(
    classBuilder: TypeSpec.Builder,
    ktSource: ClassAbstractRepresentation,
) {
    val hashCodeMethodBuilder =
        FunSpec.builder("hashCode").addModifiers(KModifier.OVERRIDE).returns(Int::class)

    // Collect all property names for hashing
    val hashProperties = mutableListOf<String>()
    hashProperties.add("super.hashCode()")

    // Add all data and object properties of the current class
    for (property in ktSource.dataProperties + ktSource.objectProperties) {
        hashProperties.add(property.propertyName)
    }

    val hashStatement =
        "return %T.hash(\n            ${hashProperties.joinToString(",\n            ")},\n        )"

    hashCodeMethodBuilder.addStatement(hashStatement, ClassName("java.util", "Objects"))

    classBuilder.addFunction(hashCodeMethodBuilder.build())
}

private fun addPropertiesToClass(
    dataProperties: LinkedHashSet<Properties>,
    objectProperties: LinkedHashSet<Properties>,
    constructorBuilder: FunSpec.Builder,
    classBuilder: TypeSpec.Builder,
    className: String,
    fileSpecBuilder: FileSpec.Builder,
    isParent: Boolean = false,
) {
    // Track if we need to add an init block
    var needsInitBlock = false
    val initBlockBuilder = CodeBlock.builder()

    // Loop over the data properties.
    for (dataProp in dataProperties) {
        // Determine the correct TypeName based on the property type string.
        val typeName: TypeName = extractDataProperties(dataProp)

        if (typeName == ClassName("unknown", "unknown")) {
            println(
                "Unknown type: ${dataProp.propertyType} for property ${dataProp.propertyName} of class ${className}"
            )
        }
        // Special handling for 'name' and 'code' properties in object properties too
        val paramName =
            when (dataProp.propertyName) {
                "name" -> {
                    needsInitBlock = true
                    fileSpecBuilder.addImport("de.fraunhofer.aisec.cpg.graph", "Name")
                    initBlockBuilder.addStatement(
                        "this.name = %T(localName = name)",
                        ClassName("de.fraunhofer.aisec.cpg.graph", "Name"),
                    )
                    "nameString"
                }
                "code" -> {
                    needsInitBlock = true
                    initBlockBuilder.addStatement("this.code = code")
                    "codeString"
                }
                else -> dataProp.propertyName
            }

        if (dataProp.propertyName == "id") continue

        // Add a constructor parameter for the property
        constructorBuilder.addParameter(dataProp.propertyName, typeName)

        // For code and id we do not want to store these values since Class "Node" from the cpg
        // already have them
        if (!isParent && dataProp.propertyName != "code" && dataProp.propertyName != "name") {
            // Add a property to the class, initialized from the constructor parameter.
            classBuilder.addProperty(
                PropertySpec.builder(dataProp.propertyName, typeName)
                    .initializer(dataProp.propertyName)
                    .build()
            )
        }

        // Add a property to the class, initialized from the constructor parameter.
        classBuilder.primaryConstructor(constructorBuilder.build())

        if (isParent) {
            // Add SuperclassConstructorParameter for the property.
            classBuilder.addSuperclassConstructorParameter(dataProp.propertyName).build()
        }
    }

    // Loop over the object properties.
    for (objectProp in objectProperties) {
        val typeName = extractObjectProperties(objectProp)

        if (typeName == ClassName("unknown", "unknown")) {
            println("Unknown type: ${objectProp.propertyType}")
        }

        // Add a constructor parameter for the property.
        constructorBuilder.addParameter(objectProp.propertyName, typeName)

        if (!isParent) {
            // Add a property to the class, initialized from the constructor parameter.
            classBuilder.addProperty(
                PropertySpec.builder(objectProp.propertyName, typeName)
                    .initializer(objectProp.propertyName)
                    .build()
            )
        }

        // Add a property to the class, initialized from the constructor parameter.
        classBuilder.primaryConstructor(constructorBuilder.build())

        if (isParent) {
            // Add SuperclassConstructorParameter for the property.
            classBuilder.addSuperclassConstructorParameter(objectProp.propertyName).build()
        }

        // Add init block if needed
        if (needsInitBlock) {
            classBuilder.addInitializerBlock(initBlockBuilder.build())
        }
    }
}

public fun findAllParentsAndSaveToClassAbstraction(
    abstraction: ClassAbstractRepresentation,
    classAbstractions: LinkedHashSet<ClassAbstractRepresentation>,
): ClassAbstractRepresentation? {
    // Find direct parent
    val directParent = classAbstractions.find { it.name == abstraction.parentClass }

    if (directParent != null) {
        // Add direct parent to allParents
        abstraction.allParents += directParent

        // Find and add parent's parents recursively
        findAllParentsAndSaveToClassAbstraction(directParent, classAbstractions)

        // Add all parents of parent to this abstraction's allParents
        val parentsToAdd = directParent.allParents
        abstraction.allParents += parentsToAdd

        return directParent
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
            "ApplicationLogging" ->
                ClassName(
                    "de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated",
                    "ApplicationLogging",
                )
            "Functionality" ->
                ClassName("de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated", "Functionality")
            "Compute" ->
                ClassName("de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated", "Compute")
            "GeoLocation" ->
                ClassName("de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated", "GeoLocation")
            "[]Resource" ->
                ARRAY.parameterizedBy(
                    ClassName("de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated", "Resource")
                )
            "NetworkInterface" ->
                ClassName(
                    "de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated",
                    "NetworkInterface",
                )
            "Image" -> ClassName("de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated", "Image")
            "ResourceLogging" ->
                ClassName("de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated", "ResourceLogging")
            "Container" ->
                ClassName("de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated", "Container")
            "DatabaseStorage" ->
                ClassName("de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated", "DatabaseStorage")
            "DatabaseService" ->
                ClassName("de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated", "DatabaseService")
            "Storage" ->
                ClassName("de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated", "Storage")
            "Authenticity" ->
                ClassName("de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated", "Authenticity")
            "TransportEncryption" ->
                ClassName(
                    "de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated",
                    "TransportEncryption",
                )
            "HttpEndpoint" ->
                ClassName("de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated", "HttpEndpoint")
            "Application" ->
                ClassName("de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated", "Application")
            "HttpRequestHandler" ->
                ClassName(
                    "de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated",
                    "HttpRequestHandler",
                )
            "Authorization" ->
                ClassName("de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated", "Authorization")
            "AccessRestriction" ->
                ClassName(
                    "de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated",
                    "AccessRestriction",
                )
            "NetworkService" ->
                ClassName("de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated", "NetworkService")
            "Logging" ->
                ClassName("de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated", "Logging")
            "LoggingService" ->
                ClassName("de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated", "LoggingService")
            "IsAuthenticity" -> BOOLEAN
            "ObjectStorage" ->
                ClassName("de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated", "ObjectStorage")
            "IsTransportEncryption" -> BOOLEAN
            "IsAtRestEncryption" -> BOOLEAN
            "Immutability" ->
                ClassName("de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated", "Immutability")
            "BlockStorage" ->
                ClassName("de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated", "BlockStorage")
            "AutomaticUpdates" ->
                ClassName(
                    "de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated",
                    "AutomaticUpdates",
                )
            "BootLogging" ->
                ClassName("de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated", "BootLogging")
            "MalwareProtection" ->
                ClassName(
                    "de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated",
                    "MalwareProtection",
                )
            "OSLogging" ->
                ClassName("de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated", "OSLogging")
            "Redundancy" ->
                ClassName("de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated", "Redundancy")
            "UsageStatistics" ->
                ClassName("de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated", "UsageStatistics")
            "EncryptionInUse" ->
                ClassName("de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated", "EncryptionInUse")
            "RemoteAttestation" ->
                ClassName(
                    "de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated",
                    "RemoteAttestation",
                )
            "ActivityLogging" ->
                ClassName("de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated", "ActivityLogging")
            "Resource" ->
                ClassName("de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated", "Resource")
            "ServiceMetadataDocument" ->
                ClassName(
                    "de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated",
                    "ServiceMetadataDocument",
                )
            "Backup" -> ClassName("de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated", "Backup")
            "CodeModule" ->
                ClassName("de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated", "CodeModule")
            "CodeRepository" ->
                ClassName("de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated", "CodeRepository")
            "DataLocation" ->
                ClassName("de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated", "DataLocation")
            "SchemaValidation" ->
                ClassName(
                    "de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated",
                    "SchemaValidation",
                )
            "DocumentChecksum" ->
                ClassName(
                    "de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated",
                    "DocumentChecksum",
                )
            "SecurityFeature" ->
                ClassName("de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated", "SecurityFeature")
            "DocumentSignature" ->
                ClassName(
                    "de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated",
                    "DocumentSignature",
                )
            "SecurityAdvisoryFeed" ->
                ClassName(
                    "de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated",
                    "SecurityAdvisoryFeed",
                )
            "CipherSuite" ->
                ClassName("de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated", "CipherSuite")
            "Error" -> ClassName("de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated", "Error")
            "AnomalyDetection" ->
                ClassName(
                    "de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated",
                    "AnomalyDetection",
                )
            "CodeRegion" ->
                ClassName("de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated", "CodeRegion")
            "IsAccessRestriction" -> BOOLEAN
            "MachineLearning" ->
                ClassName("de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated", "MachineLearning")
            "Key" -> ClassName("de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated", "Key")
            "Vulnerability" ->
                ClassName("de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated", "Vulnerability")
            "Infrastructure" ->
                ClassName("de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated", "Infrastructure")
            "AtRestEncryption" ->
                ClassName(
                    "de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated",
                    "AtRestEncryption",
                )
            "Library" ->
                ClassName("de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated", "Library")
            "Credential" ->
                ClassName("de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated", "Credential")
            "Function" ->
                ClassName("de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated", "Function")
            "SecurityAdvisoryDocument" ->
                ClassName(
                    "de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated",
                    "SecurityAdvisoryDocument",
                )
            "Node" -> ClassName("de.fraunhofer.aisec.cpg.graph", "Node")
            "Concept" -> ClassName("de.fraunhofer.aisec.cpg.graph.concepts", "Concept")
            "Agnostic" -> ClassName("de.fraunhofer.aisec.cpg.graph.autoGenerated", "Agnostic")
            "Allocate" ->
                ClassName("de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated", "Allocate")
            "AndRule" ->
                ClassName("de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated", "AndRule")
            "Authenticate" ->
                ClassName("de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated", "Authenticate")
            "AuthenticationOperation" ->
                ClassName(
                    "de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated",
                    "AuthenticationOperation",
                )
            "AuthorizeJwt" ->
                ClassName("de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated", "AuthorizeJwt")
            "BlockStorageOperation" ->
                ClassName(
                    "de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated",
                    "BlockStorageOperation",
                )
            "Boundary" ->
                ClassName("de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated", "Boundary")
            "CheckAccess" ->
                ClassName("de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated", "CheckAccess")
            "Cipher" -> ClassName("de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated", "Cipher")
            "CipherOperation" ->
                ClassName("de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated", "CipherOperation")
            "Configuration" ->
                ClassName("de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated", "Configuration")
            "ConfigurationDocument" ->
                ClassName(
                    "de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated",
                    "ConfigurationDocument",
                )
            "ConfigurationGroup" ->
                ClassName(
                    "de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated",
                    "ConfigurationGroup",
                )
            "ConfigurationGroupSource" ->
                ClassName(
                    "de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated",
                    "ConfigurationGroupSource",
                )
            "ConfigurationOperation" ->
                ClassName(
                    "de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated",
                    "ConfigurationOperation",
                )
            "ConfigurationOptionSource" ->
                ClassName(
                    "de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated",
                    "ConfigurationOptionSource",
                )
            "ConfigurationOption" ->
                ClassName(
                    "de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated",
                    "ConfigurationOption",
                )
            "ConfigurationSource" ->
                ClassName(
                    "de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated",
                    "ConfigurationSource",
                )
            "Context" ->
                ClassName("de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated", "Context")
            "CreateEncryptedDisk" ->
                ClassName(
                    "de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated",
                    "CreateEncryptedDisk",
                )
            "CreateSecret" ->
                ClassName("de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated", "CreateSecret")
            "Darwin" -> ClassName("de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated", "Darwin")
            "DeAllocate" ->
                ClassName("de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated", "DeAllocate")
            "DiskEncryption" ->
                ClassName("de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated", "DiskEncryption")
            "DiskEncryptionOperation" ->
                ClassName(
                    "de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated",
                    "DiskEncryptionOperation",
                )
            "DynamicLoading" ->
                ClassName("de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated", "DynamicLoading")
            "DynamicLoadingOperation" ->
                ClassName(
                    "de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated",
                    "DynamicLoadingOperation",
                )
            "EncryptionOperation" ->
                ClassName(
                    "de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated",
                    "EncryptionOperation",
                )
            "EntryPoint" ->
                ClassName("de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated", "EntryPoint")
            "EqualityCheck" ->
                ClassName("de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated", "EqualityCheck")
            "ExitBoundaryOperation" ->
                ClassName(
                    "de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated",
                    "ExitBoundaryOperation",
                )
            "File" -> ClassName("de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated", "File")
            "FileHandle" ->
                ClassName("de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated", "FileHandle")
            "FileLikeObject" ->
                ClassName("de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated", "FileLikeObject")
            "FileOperation" ->
                ClassName("de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated", "FileOperation")
            "GetSecret" ->
                ClassName("de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated", "GetSecret")
            "Http" -> ClassName("de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated", "Http")
            "HttpClient" ->
                ClassName("de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated", "HttpClient")
            "HttpClientOperation" ->
                ClassName(
                    "de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated",
                    "HttpClientOperation",
                )
            "HttpEndpointOperation" ->
                ClassName(
                    "de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated",
                    "HttpEndpointOperation",
                )
            "HttpRequest" ->
                ClassName("de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated", "HttpRequest")
            "HttpRequestContext" ->
                ClassName(
                    "de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated",
                    "HttpRequestContext",
                )
            "HttpRequestHandlerOperation" ->
                ClassName(
                    "de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated",
                    "HttpRequestHandlerOperation",
                )
            "IssueJwt" ->
                ClassName("de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated", "IssueJwt")
            "JwtAuthentication" ->
                ClassName(
                    "de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated",
                    "JwtAuthentication",
                )
            "LibraryEntryPoint" ->
                ClassName("de.fraunhofer.aisec.cpg.graph.concepts.arch", "LibraryEntryPoint")
            "LoadConfiguration" ->
                ClassName(
                    "de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated",
                    "LoadConfiguration",
                )
            "LoadLibrary" ->
                ClassName("de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated", "LoadLibrary")
            "LoadSymbol" ->
                ClassName("de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated", "LoadSymbol")
            "LocalEntryPoint" ->
                ClassName("de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated", "LocalEntryPoint")
            "LogDocument" ->
                ClassName("de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated", "LogDocument")
            "LogGet" -> ClassName("de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated", "LogGet")
            "LogWrite" ->
                ClassName("de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated", "LogWrite")
            "Main" -> ClassName("de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated", "Main")
            "Memory" -> ClassName("de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated", "Memory")
            "MemoryOperation" ->
                ClassName("de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated", "MemoryOperation")
            "OperatingSystemArchitecture" ->
                ClassName(
                    "de.fraunhofer.aisec.cpg.graph.concepts.arch",
                    "OperatingSystemArchitecture",
                )
            "Policy" -> ClassName("de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated", "Policy")
            "PolicyOperation" ->
                ClassName("de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated", "PolicyOperation")
            "PolicyRule" ->
                ClassName("de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated", "PolicyRule")
            "POSIX" -> ClassName("de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated", "POSIX")
            "Principal" ->
                ClassName("de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated", "Principal")
            "ProtectedAsset" ->
                ClassName("de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated", "ProtectedAsset")
            "ProtectedAssetOperation" ->
                ClassName(
                    "de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated",
                    "ProtectedAssetOperation",
                )
            "ProvideConfiguration" ->
                ClassName(
                    "de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated",
                    "ProvideConfiguration",
                )
            "ProvideConfigurationGroup" ->
                ClassName(
                    "de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated",
                    "ProvideConfigurationGroup",
                )
            "ProvideConfigurationOption" ->
                ClassName(
                    "de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated",
                    "ProvideConfigurationOption",
                )
            "ReadConfigurationGroup" ->
                ClassName(
                    "de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated",
                    "ReadConfigurationGroup",
                )
            "ReadConfigurationOption" ->
                ClassName(
                    "de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated",
                    "ReadConfigurationOption",
                )
            "RegisterConfigurationGroup" ->
                ClassName(
                    "de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated",
                    "RegisterConfigurationGroup",
                )
            "RegisterConfigurationOption" ->
                ClassName(
                    "de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated",
                    "RegisterConfigurationOption",
                )
            "RegisterHttpEndpoint" ->
                ClassName(
                    "de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated",
                    "RegisterHttpEndpoint",
                )
            "RemoteEntryPoint" ->
                ClassName(
                    "de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated",
                    "RemoteEntryPoint",
                )
            "ReportDocument" ->
                ClassName("de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated", "ReportDocument")
            "SecretOperation" ->
                ClassName("de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated", "SecretOperation")
            "SelectorKey" ->
                ClassName("de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated", "SelectorKey")
            "Token" -> ClassName("de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated", "Token")
            "UnlockEncryptedDisk" ->
                ClassName(
                    "de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated",
                    "UnlockEncryptedDisk",
                )
            "ValidateJwt" ->
                ClassName("de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated", "ValidateJwt")
            "Value" -> ClassName("de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated", "Value")
            "Win32" -> ClassName("de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated", "Win32")
            "Secret" -> ClassName("de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated", "Secret")
            "Operation" -> ClassName("de.fraunhofer.aisec.cpg.graph.concepts", "Operation") //
            "Confidentiality" ->
                ClassName("de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated", "Confidentiality")
            "Encryption" ->
                ClassName("de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated", "Encryption")
            "LogOperation" ->
                ClassName("de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated", "LogOperation")
            "LogOutput" ->
                ClassName("de.fraunhofer.aisec.cpg.graph.concepts.autoGenerated", "LogOutput")

            else -> ClassName("unknown", "unknown")
        }
    return typeName
}
