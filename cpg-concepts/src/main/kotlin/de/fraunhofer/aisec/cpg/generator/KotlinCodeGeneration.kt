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
import com.squareup.kotlinpoet.ParameterSpec
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
            ktSource.objectProperties
                .sortedWith(
                    compareBy<Properties> {
                            it.propertyName ==
                                "linkedConcept" // linkedConcept always last Property.
                        } // Hacky but needed because we ignore concept and always replace it with
                        // linkedConcept.
                        // When generating Operations, we always inherit from Operation which has a
                        // concept and underlying node property. When now linkedConcept is directly
                        // processed before concept then we skip linkedConcept and replace the
                        // processing of concept with linkedConcept. When doing that the order stays
                        // the same.
                        .thenBy { it.propertyName }
                )
                .toCollection(linkedSetOf())
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
    val generatedEnumNames = mutableSetOf<String>()
    val autoGeneratedTypeLookup: Map<String, ClassName> =
        otherClassAbstractions
            .asSequence()
            .filter { !it.packageName.isNullOrBlank() }
            .associate { it.name to ClassName(it.packageName!!, it.name) }

    // build parent class in order to extend from it
    // Get the parent interface/class using its fully-qualified name.
    var classBuilder = TypeSpec.classBuilder(ktSource.name).addModifiers(KModifier.OPEN)

    if (ktSource.parentClass != "" && ktSource.parentClass != "owl:Thing") {
        val parentClass =
            ClassName("de.fraunhofer.aisec.cpg.graph.concepts.ontology", ktSource.parentClass)
        classBuilder =
            TypeSpec.classBuilder(ktSource.name)
                .superclass(parentClass)
                .addModifiers(KModifier.OPEN)
    }

    if (
        ktSource.allParents.find { it.name == "Concept" } != null && ktSource.allParents.size == 1
    ) {
        // All root nodes have to inherit from the Concept class
        classBuilder =
            TypeSpec.classBuilder(ktSource.name)
                .superclass(ClassName("de.fraunhofer.aisec.cpg.graph.concepts", "Concept"))
                .addModifiers(KModifier.OPEN)
    }

    if (
        ktSource.allParents.find { it.name == "Operation" } != null && ktSource.allParents.size == 1
    ) {
        // All operation nodes have to inherit from the Operation class
        classBuilder =
            TypeSpec.classBuilder(ktSource.name)
                .superclass(ClassName("de.fraunhofer.aisec.cpg.graph.concepts", "Operation"))
                .addModifiers(KModifier.OPEN)
    }

    // Add docstring in front of constructor if structDescription is available
    if (!ktSource.structDescription.isNullOrBlank()) {
        classBuilder.addKdoc("%L\n", ktSource.structDescription!!)
    }

    // Create a constructor builder for adding parameters.
    val constructorBuilder = FunSpec.constructorBuilder()

    // writes own class properties into the constructor
    addEnumDefinitions(ktSource.dataProperties, fileSpecBuilder, generatedEnumNames)

    addPropertiesToClass(
        ktSource.dataProperties,
        ktSource.objectProperties,
        constructorBuilder,
        classBuilder,
        ktSource.name,
        fileSpecBuilder,
        ktSource.packageName!!,
        autoGeneratedTypeLookup,
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
            ktSource.packageName!!,
            autoGeneratedTypeLookup,
            isParent = true,
        )
    }

    // Set the primary constructor for the class.
    classBuilder.primaryConstructor(constructorBuilder.build())

    // We have to add equals method and hashCode method to concepts and operations
    addEqualsMethod(classBuilder, ktSource)
    addHashCodeMethod(classBuilder, ktSource)

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
    for (property in
        ktSource.dataProperties +
            ktSource.objectProperties.filter { !it.propertyName.equals("linkedConcept") }) {
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
    for (property in
        ktSource.dataProperties +
            ktSource.objectProperties.filter { !it.propertyName.equals("linkedConcept") }) {
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
    classPackage: String,
    autoGeneratedTypeLookup: Map<String, ClassName>,
    isParent: Boolean = false,
) {
    // Track if we need to add an init block
    var needsInitBlock = false
    val initBlockBuilder = CodeBlock.builder()

    // Loop over the data properties.
    for (dataProp in dataProperties) {
        // Determine the correct TypeName based on the property type string.
        val typeName: TypeName = extractDataProperties(dataProp, classPackage)

        if (typeName == ClassName("unknown", "unknown")) {
            println(
                "Unknown type: ${dataProp.propertyType} for property ${dataProp.propertyName} of class ${className}"
            )
        }
        if (dataProp.propertyName == "id") continue

        // Special handling for 'name' and 'code' properties
        if (dataProp.propertyName == "name") {
            needsInitBlock = true
            fileSpecBuilder.addImport("de.fraunhofer.aisec.cpg.graph", "Name")
            initBlockBuilder.addStatement(
                "name?.let { this.name = %T(localName = it) }",
                ClassName("de.fraunhofer.aisec.cpg.graph", "Name"),
            )
            // Always pass name through without storing it
            constructorBuilder.addParameter("name", typeName)
        } else if (dataProp.propertyName == "code") {
            needsInitBlock = true
            initBlockBuilder.addStatement("code?.let { this.code = it }")
            // Always pass code through without storing it
            constructorBuilder.addParameter("code", typeName)
        } else if (!isParent) {
            // For own properties: add as val in constructor (stored in class)
            classBuilder.addProperty(
                PropertySpec.builder(dataProp.propertyName, typeName)
                    .initializer(dataProp.propertyName)
                    .build()
            )
            constructorBuilder.addParameter(dataProp.propertyName, typeName)
        } else {
            // For parent properties: add as regular parameter (not stored in class)
            constructorBuilder.addParameter(dataProp.propertyName, typeName)
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

        val typeName = extractObjectProperties(objectProp, autoGeneratedTypeLookup)

        if (typeName == ClassName("unknown", "unknown")) {
            println("Unknown type: ${objectProp.propertyType}")
        }

        // Add a constructor parameter for the property.
        if (objectProp.propertyProperty == "hasMultiple") {
            val parameterType =
                ClassName("kotlin.collections", "MutableList")
                    .parameterizedBy(typeName.copy(nullable = true))
            if (!isParent) {
                // For own properties: add as property stored in class
                classBuilder.addProperty(
                    PropertySpec.builder(objectProp.propertyName, parameterType)
                        .initializer(objectProp.propertyName)
                        .build()
                )
            }
            constructorBuilder.addParameter(objectProp.propertyName, parameterType)
        } else if (objectProp.propertyName != "concept") {
            val parameterType =
                if (objectProp.propertyName == "linkedConcept") typeName
                else typeName.copy(nullable = true)

            // For own properties: add as property stored in class
            // For parent properties: just pass through without storing
            // Special case: linkedConcept should only be stored in Operation class
            if (
                !isParent &&
                    (objectProp.propertyName != "linkedConcept" || className == "Operation")
            ) {
                classBuilder.addProperty(
                    PropertySpec.builder(objectProp.propertyName, parameterType)
                        .initializer(objectProp.propertyName)
                        .build()
                )
            }

            val parameterBuilder =
                ParameterSpec.builder(objectProp.propertyName, parameterType).apply {
                    if (objectProp.propertyName == "underlyingNode") {
                        defaultValue("null")
                    }
                }
            constructorBuilder.addParameter(parameterBuilder.build())
        }

        // Add a property to the class, initialized from the constructor parameter.
        classBuilder.primaryConstructor(constructorBuilder.build())

        // If is parent -> SuperClassConstructorParameters have to be set
        if (isParent) {
            // Add SuperclassConstructorParameter for the property.
            if (objectProp.propertyName == "concept") {
                classBuilder.addSuperclassConstructorParameter("linkedConcept").build()
            } else if (objectProp.propertyName == "linkedConcept") {
                continue
            } else {
                classBuilder.addSuperclassConstructorParameter(objectProp.propertyName).build()
            }
        }
    }

    // Add init block if needed (after all properties are processed)
    if (needsInitBlock) {
        classBuilder.addInitializerBlock(initBlockBuilder.build())
    }
}

private fun addEnumDefinitions(
    dataProperties: LinkedHashSet<Properties>,
    fileSpecBuilder: FileSpec.Builder,
    generatedEnumNames: MutableSet<String>,
) {
    for (property in dataProperties) {
        if (!property.hasEnum) {
            continue
        }

        val enumName = property.enumTypeName ?: continue
        if (!generatedEnumNames.add(enumName)) {
            continue
        }

        val enumBuilder = TypeSpec.enumBuilder(enumName)
        property.enumValues.forEach { enumBuilder.addEnumConstant(it) }
        fileSpecBuilder.addType(enumBuilder.build())
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

private const val AUTO_GENERATED_PACKAGE = "de.fraunhofer.aisec.cpg.graph.concepts.ontology"

private fun autoGeneratedClass(simpleName: String): ClassName =
    ClassName(AUTO_GENERATED_PACKAGE, simpleName)

private val dataPropertyTypeMap: Map<String, TypeName> =
    mutableMapOf<String, TypeName>().apply {
        put("java.time.Duration", ClassName("java.time", "Duration"))
        put("java.time.ZonedDateTime", ClassName("java.time", "ZonedDateTime"))
        put("Short", SHORT)
        put("String", STRING)
        put("int", INT)
        put("Double", DOUBLE)
        put("Float", FLOAT)
        put("float", FLOAT)
        put("boolean", BOOLEAN)
        put(
            "de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration",
            ClassName("de.fraunhofer.aisec.cpg.graph.declarations", "FunctionDeclaration"),
        )
        put(
            "de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression",
            ClassName("de.fraunhofer.aisec.cpg.graph.statements.expressions", "Expression"),
        )
        put(
            "de.fraunhofer.aisec.cpg.graph.Node",
            ClassName("de.fraunhofer.aisec.cpg.graph", "Node"),
        )
        put(
            "de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression",
            ClassName("de.fraunhofer.aisec.cpg.graph.statements.expressions", "CallExpression"),
        )
        put(
            "java.util.List<de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration>",
            ClassName("java.util", "List")
                .parameterizedBy(
                    ClassName(
                        "de.fraunhofer.aisec.cpg.graph.declarations",
                        "TranslationUnitDeclaration",
                    )
                ),
        )
        put(
            "java.util.List<de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression>",
            ClassName("java.util", "List")
                .parameterizedBy(
                    ClassName(
                        "de.fraunhofer.aisec.cpg.graph.statements.expressions",
                        "CallExpression",
                    )
                ),
        )
        put("java.util.Map<String, String>", MUTABLE_MAP.parameterizedBy(STRING, STRING))
        put("java.util.ArrayList<String>", ARRAY.parameterizedBy(STRING))
        put("java.util.ArrayList<Short>", ARRAY.parameterizedBy(SHORT))
        put("dateTime", ClassName("java.time", "ZonedDateTime"))
        put("listString", ClassName("java.util", "List").parameterizedBy(STRING))
    }

private val objectPropertyBooleanTypes =
    setOf("IsAuthenticity", "IsTransportEncryption", "IsAtRestEncryption", "IsAccessRestriction")

private val objectPropertySpecialTypes: Map<String, TypeName> =
    mapOf(
        "Node" to ClassName("de.fraunhofer.aisec.cpg.graph", "Node"),
        "Concept" to ClassName("de.fraunhofer.aisec.cpg.graph.concepts.ontology", "Concept"),
        "Agnostic" to ClassName("de.fraunhofer.aisec.cpg.graph.concepts.ontology", "Agnostic"),
        "LibraryEntryPoint" to
            ClassName("de.fraunhofer.aisec.cpg.graph.concepts.ontology", "LibraryEntryPoint"),
        "OperatingSystemArchitecture" to
            ClassName(
                "de.fraunhofer.aisec.cpg.graph.concepts.ontology",
                "OperatingSystemArchitecture",
            ),
        "Operation" to ClassName("de.fraunhofer.aisec.cpg.graph.concepts", "Operation"),
    )

private fun extractDataProperties(dataProp: Properties, classPackage: String): TypeName {
    if (dataProp.hasEnum) {
        val enumType = dataProp.enumTypeName
        if (!enumType.isNullOrEmpty()) {
            return ClassName(classPackage, enumType).copy(nullable = true)
        }
    }

    val typeName = dataPropertyTypeMap[dataProp.propertyType] ?: ClassName("unknown", "unknown")
    return typeName.copy(nullable = true)
}

private fun extractObjectProperties(
    objectProp: Properties,
    autoGeneratedTypeLookup: Map<String, ClassName>,
): TypeName {
    val type = objectProp.propertyType
    if (type.isBlank()) {
        return ClassName("unknown", "unknown")
    }

    if (type in objectPropertyBooleanTypes) {
        return BOOLEAN
    }

    objectPropertySpecialTypes[type]?.let {
        return it
    }

    return when {
        type == "[]Resource" ->
            autoGeneratedTypeLookup["Resource"]?.let { ARRAY.parameterizedBy(it) }
                ?: ARRAY.parameterizedBy(autoGeneratedClass("Resource"))
        autoGeneratedTypeLookup.containsKey(type) -> autoGeneratedTypeLookup.getValue(type)
        else -> ClassName("unknown", "unknown")
    }
}
