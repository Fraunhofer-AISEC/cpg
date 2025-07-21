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

import de.fraunhofer.aisec.cpg.models.ClassAbstractRepresentation
import de.fraunhofer.aisec.cpg.models.Properties
import java.io.File
import java.util.stream.Collectors
import org.apache.commons.lang3.StringUtils
import org.jboss.forge.roaster.Roaster
import org.jboss.forge.roaster.model.source.Import
import org.jboss.forge.roaster.model.source.JavaClassSource
import org.jboss.forge.roaster.model.source.MethodSource
import org.jboss.forge.roaster.model.source.PropertySource
import org.semanticweb.owlapi.apibinding.OWLManager
import org.semanticweb.owlapi.model.*
import org.semanticweb.owlapi.model.parameters.Imports
import org.semanticweb.owlapi.search.EntitySearcher
import uk.ac.manchester.cs.owl.owlapi.*

/**
 * OWLCloudOntologyReader reads an OWL file and generates Java classes or Abstract representations
 * from it.
 */
class OWLCloudOntologyReader(filepath: String, private val resourceNameFromOwlFile: String) {
    private var ontology: OWLOntology? = null
    private var df: OWLDataFactory? = null
    val interfaceList: MutableList<String> =
        ArrayList() // It is assumed, that the classes that are defined as interfaces have a unique

    // name

    init {
        readOwlFile(filepath)
    }

    // Read owl file from filesystem
    @Throws(OWLOntologyCreationException::class)
    private fun readOwlFile(filepath: String) {
        println("Read owl file")
        val manager = OWLManager.createOWLOntologyManager()
        ontology = manager.loadOntologyFromOntologyDocument(File(filepath))
        df = manager.owlDataFactory
    }

    // Get list of AbstractRepresentation of OWL classes
    fun getAbstractRepresentationOfOWL(
        packageName: String?
    ): MutableList<ClassAbstractRepresentation> {
        val classes = ontology!!.classesInSignature
        val abstractRepresentationList: MutableList<ClassAbstractRepresentation> = ArrayList()

        for (clazz in classes) {
            // skip owl:Thing
            if (clazz.isOWLThing) continue
            val gs = getAbstractInformationFromOWLClass(clazz, classes)
            gs.packageName = packageName
            gs.structDescription = getClassDescription(clazz, ontology)
            abstractRepresentationList.add(gs)
        }
        return abstractRepresentationList
    }

    // Get all java classes from OWL file
    fun getJavaClassSources(packageName: String?): List<JavaClassSource> {
        val classes = ontology!!.classesInSignature
        var jcsList: MutableList<JavaClassSource> = ArrayList()
        for (clazz in classes) {
            // skip owl:Thing
            if (clazz.isOWLThing) {
                continue
            }

            val jcs = getJavaClassSourceFromOWLClass(clazz)
            jcs!!.setPackage(packageName)
            jcsList.add(jcs)
        }

        // Set superclass call, must be done here to have the parameters from the superclass
        // constructor
        jcsList = addSuperclassProperties(jcsList)
        return jcsList
    }

    private fun getAbstractInformationFromOWLClass(
        clazz: OWLClass,
        classes: Set<OWLClass>,
    ): ClassAbstractRepresentation {
        var gs = ClassAbstractRepresentation(getFormatedClassName(clazz), getParentClassName(clazz))

        // Set variables by 'OWL object properties'
        gs = setOWLClassObjectProperties(gs, clazz, classes)

        // Set variables by 'OWL data properties'
        gs = setOWLClassDataProperties(gs, clazz)

        // Set resource types, e.g., []string {"BlockStorage", "Storage", "Resource"}
        gs = setResourceTypes(gs, clazz, classes)
        return gs
    }

    // Sets the resource types of an object property, e.g., []string {"BlockStorage", "Storage",
    // "Resource"}
    private fun setResourceTypes(
        gs: ClassAbstractRepresentation,
        clazz: OWLClass,
        classes: Set<OWLClass>,
    ): ClassAbstractRepresentation {
        gs.resourceTypes = getResourceType(clazz, classes).reversed()

        return gs
    }

    // Returns a list of resource types if the object is under the object Resource, e.g.,
    // VirtualMachine belongs to Resource and needs a resource type list, ABAC belongs to
    // SecurityFeature and will not need a resource type list. If the parent of an object has no
    // parent an empty list will be returned.
    private fun getResourceType(clazz: OWLClass, classes: Set<OWLClass>): List<String> {
        val resourceTypes: MutableList<String> = ArrayList()
        if (
            getParentClassName(clazz) == "" ||
                !isRootClassNameResource(clazz, classes) ||
                getParentClassName(clazz) == "owl:Thing"
        ) {
            return emptyList()
        } else if (getParentClassName(clazz) == resourceNameFromOwlFile) {
            resourceTypes.add(resourceNameFromOwlFile)
            resourceTypes.add(getClassName(clazz, ontology))
            return resourceTypes
        }

        resourceTypes.addAll(getParentClass(clazz)?.let { getResourceType(it, classes) }!!)
        resourceTypes.add(getClassName(clazz, ontology))

        return resourceTypes
    }

    private fun setClassName(javaClass: JavaClassSource, clazz: OWLClass): JavaClassSource {
        var className = getClassName(clazz, ontology)
        // Format class name
        if (className.contains("#")) className = className.split("#").toTypedArray()[1]
        className = formatString(className)
        javaClass.setName(className).setPublic()
        return javaClass
    }

    // Sets the super class name. If the super class name is empty or 'owl:Thing' the nodeSuperType
    // is set. Note: It shouldn't happen that the super class name is empty.
    private fun setSuperClassName(javaClass: JavaClassSource, clazz: OWLClass): JavaClassSource {
        val nodeSuperType = "de.fraunhofer.aisec.cpg.graph.Node"
        val superClassName = getParentClassName(clazz)

        if (superClassName == "owl:Thing") {
            javaClass.superType = nodeSuperType
        } else if (superClassName.isNotEmpty()) {
            javaClass.superType = superClassName
        } else {
            javaClass.superType = nodeSuperType
        }
        return javaClass
    }

    private fun addImportsFromSuperclass(
        jcs: JavaClassSource,
        jcsList: List<JavaClassSource>,
    ): JavaClassSource {
        var superXClassImports: MutableList<Import?> = ArrayList()
        superXClassImports = getSuperXClassImports(jcs, jcsList, superXClassImports)
        for (elem in superXClassImports) {
            jcs.addImport(elem)
        }
        return jcs
    }

    private fun getSuperXClassImports(
        jcs: JavaClassSource,
        jcsList: List<JavaClassSource>,
        superXClassImports: MutableList<Import?>,
    ): MutableList<Import?> {
        for (elem in jcs.imports) {
            superXClassImports.add(elem)
        }
        return if (StringUtils.substringAfterLast(jcs.superType, ".") == "Node") superXClassImports
        else {
            getSuperXClassImports(
                jcsList
                    .stream()
                    .filter { a: JavaClassSource ->
                        a.name == StringUtils.substringAfterLast(jcs.superType, ".")
                    }
                    .findFirst()
                    .get(),
                jcsList,
                superXClassImports,
            )
        }
    }

    // Get a Map<name, type> of superClassParameters
    // superClassParameters are the parameters needed to call the super constructor;
    // super(/*parameters*/)
    private fun getSuperXClassParameters(
        jcs: JavaClassSource?,
        jcsList: List<JavaClassSource>,
        superXClassParameters: MutableMap<String, String>,
    ): MutableMap<String, String> {

        // Get sorted properties list
        val javaClassPropertiesList = jcs!!.properties
        javaClassPropertiesList.sortWith(
            Comparator.comparing { obj: PropertySource<JavaClassSource?> -> obj.name }
        )
        for (elem in javaClassPropertiesList) {
            superXClassParameters[elem.name] = elem.type.toString()
        }
        return if (StringUtils.substringAfterLast(jcs.superType, ".") == "Node")
            superXClassParameters
        else {
            getSuperXClassParameters(
                jcsList
                    .stream()
                    .filter { a: JavaClassSource ->
                        a.name == StringUtils.substringAfterLast(jcs.superType, ".")
                    }
                    .findFirst()
                    .get(),
                jcsList,
                superXClassParameters,
            )
        }
    }

    private fun setEmptySuperclassCall(jcs: JavaClassSource): JavaClassSource {
        val javaClassConstructor =
            jcs.methods
                .stream()
                .filter { a: MethodSource<JavaClassSource?> -> a.name == jcs.name }
                .findFirst()
                .get()
        javaClassConstructor.body = "super();"
        return jcs
    }

    private fun addSuperclassCall(
        jcs: JavaClassSource,
        jcsList: List<JavaClassSource>,
    ): JavaClassSource {

        // Get the superClass object
        var superClass: JavaClassSource? = null
        for (elem in jcsList) {
            if (elem.name == StringUtils.substringAfterLast(jcs.superType, ".")) {
                return if (StringUtils.substringAfterLast(elem.name, ".") != "Node") {
                    superClass = elem
                    break
                } else {
                    jcs
                }
            }
        }

        // If superclass is 'Node' set emtpy superclass call
        if (superClass!!.name == "Node") {
            setEmptySuperclassCall(jcs)
            return jcs
        }

        // Get all parameters of superClasses
        var superXClassParameters: MutableMap<String, String> = LinkedHashMap()
        superXClassParameters = getSuperXClassParameters(superClass, jcsList, superXClassParameters)

        // Get javaClass constructor
        val javaClassConstructor =
            jcs.methods
                .stream()
                .filter { a: MethodSource<JavaClassSource?> -> a.name == jcs.name }
                .findFirst()
                .get()

        // Add parameters of superclass to javaClass constructor
        for ((key, value) in superXClassParameters) {
            javaClassConstructor.addParameter(value, key)
        }

        // Add 'super(superClassParameter1, superClassParameter2, ...)' to javaclass constructor
        // body
        javaClassConstructor.body =
            "super(" +
                getListAsCommaSeparatedString(superXClassParameters) +
                ");" +
                javaClassConstructor.body
        return jcs
    }

    private fun getListAsCommaSeparatedString(superClassParameters: Map<String, String>): String {
        var superClassParametersAsCommaSeparatedString = ""
        for ((key) in superClassParameters) {
            if (superClassParametersAsCommaSeparatedString != "")
                superClassParametersAsCommaSeparatedString =
                    "$superClassParametersAsCommaSeparatedString,"
            superClassParametersAsCommaSeparatedString += key
        }
        return superClassParametersAsCommaSeparatedString
    }

    // Set OWl class data properties as java class variable
    private fun getJavaClassSourceFromOWLClass(clazz: OWLClass): JavaClassSource? {
        var javaClass = Roaster.create(JavaClassSource::class.java)

        // Set class name
        javaClass = setClassName(javaClass, clazz)
        println(
            """
                
                Class [$clazz]  	${getClassName(clazz, ontology)}
                """
                .trimIndent()
        )
        if (getClassName(clazz, ontology) == resourceNameFromOwlFile) println("")

        // Set super class name
        javaClass = setSuperClassName(javaClass, clazz)

        // Add constructor shell, need to be here, so that it is the first method
        javaClass = addConstructorShell(javaClass)

        // Set variables by 'OWL object properties'
        javaClass = setOWLClassObjectProperties(javaClass, clazz)

        // Set variables by 'OWL data properties'
        javaClass = setOWLClassDataProperties(javaClass, clazz)

        // Set constructor, superclass constructor is set later, because all class and superclass
        // parameters must
        // be known
        javaClass = setClassConstructor(javaClass)

        // Set description
        val description = getClassDescription(clazz, ontology)
        if (description != "")
            javaClass.javaDoc.text =
                getClassName(clazz, ontology) +
                    " is an entity in our Cloud ontology. " +
                    description

        // Check syntax
        if (javaClass.hasSyntaxErrors()) {
            System.err.println("SyntaxError: " + javaClass.syntaxErrors)
            return null
        }

        return javaClass
    }

    private fun setClassConstructor(javaClass: JavaClassSource): JavaClassSource {
        // Get method of constructor
        val javaClassConstructor = javaClass.getMethod(javaClass.name) ?: return javaClass

        // Get sorted properties list
        val javaClassPropertiesList = javaClass.properties
        javaClassPropertiesList.sortWith(
            Comparator.comparing { obj: PropertySource<JavaClassSource?> -> obj.name }
        )

        // Set parameters and body of constructor
        for (elem in javaClassPropertiesList) {
            javaClassConstructor.addParameter(elem.type.toString(), elem.name)
            javaClassConstructor.body =
                javaClassConstructor.body + elem.mutator.name + "(" + elem.name + ")" + ";"
        }

        return javaClass
    }

    private fun addConstructorShell(javaClass: JavaClassSource): JavaClassSource {
        javaClass.addMethod().setConstructor(true).setBody("").setPublic()
        return javaClass
    }

    // Set OWl class data properties
    private fun setOWLClassDataProperties(
        gs: ClassAbstractRepresentation,
        clazz: OWLClass,
    ): ClassAbstractRepresentation {
        val propertiesList: MutableList<Properties> = ArrayList()

        // Get Set of OWLClassAxioms
        val tempAx = ontology!!.getAxioms(clazz, Imports.EXCLUDED)
        var classRelationshipPropertyName: String
        var classDataPropertyValue: String

        // Currently, it is assumed that there is only one parent, but there can be several
        // relationships
        for (classAxiom in tempAx) {
            if (classAxiom.isOfType(AxiomType.EQUIVALENT_CLASSES)) {
                continue
            }
            val property = Properties()
            val ce = classAxiom as OWLSubClassOfAxiomImpl
            val superClass = ce.superClass

            // If type is DATA_SOME_VALUES_FROM, the 'OWL data property value' is a literal (string)
            if (superClass.classExpressionType == ClassExpressionType.DATA_SOME_VALUES_FROM) {
                classRelationshipPropertyName = getClassDataPropertyName(superClass)
                classDataPropertyValue =
                    getClassDataPropertyValue(superClass as OWLDataSomeValuesFromImpl)
                if (classDataPropertyValue == "string")
                    classDataPropertyValue = StringUtils.capitalize(classDataPropertyValue)
                property.propertyType = classDataPropertyValue
                property.propertyName = classRelationshipPropertyName
                // Set data properties description, e.g., for the property mixedDuties from the RBAC
                // class
                property.propertyDescription =
                    getDataPropertyDescription(ontology, classAxiom.dataPropertiesInSignature)
            } else if (superClass.classExpressionType == ClassExpressionType.DATA_HAS_VALUE) {
                // little but hacky,
                classRelationshipPropertyName = getClassDataPropertyName(superClass)
                classDataPropertyValue = getClassDataPropertyValue(superClass as OWLDataHasValue)
                if (classDataPropertyValue == "string")
                    classDataPropertyValue = StringUtils.capitalize(classDataPropertyValue)
                property.propertyType = classDataPropertyValue
                property.propertyName = classRelationshipPropertyName
                property.propertyDescription =
                    getDataPropertyDescription(ontology, classAxiom.dataPropertiesInSignature)
                //                // check, if the type is a Map, then we need to ignore it in neo4j
                // for now
                //                if (classDataPropertyValue.startsWith("java.util.Map")) {
                //                    property.addAnnotation("org.neo4j.ogm.annotation.Transient");
                //                }
            } else {
                continue
            }
            propertiesList.add(property)
        }
        gs.dataProperties = propertiesList
        return gs
    }

    // Set OWl class data properties as java class variable
    private fun setOWLClassDataProperties(
        javaClass: JavaClassSource,
        clazz: OWLClass,
    ): JavaClassSource {

        // Get Set of OWLClassAxioms
        val tempAx = ontology!!.getAxioms(clazz, Imports.EXCLUDED)
        var classRelationshipPropertyName: String
        var classDataPropertyValue: String

        // Currently, it is assumed that there is only one parent, but there can be several
        // relationships
        for (classAxiom in tempAx) {
            if (classAxiom.isOfType(AxiomType.EQUIVALENT_CLASSES)) {
                continue
            }
            val ce = classAxiom as OWLSubClassOfAxiomImpl
            val superClass = ce.superClass

            // If type is DATA_SOME_VALUES_FROM, the 'OWL data property value' is  a literal
            // (string)
            if (superClass.classExpressionType == ClassExpressionType.DATA_SOME_VALUES_FROM) {
                classRelationshipPropertyName = getClassDataPropertyName(superClass)
                classDataPropertyValue =
                    getClassDataPropertyValue(superClass as OWLDataSomeValuesFromImpl)
                if (classDataPropertyValue == "string")
                    classDataPropertyValue = StringUtils.capitalize(classDataPropertyValue)

                // Set data properties description, e.g., for the property mixedDuties from the RBAC
                // class
                val dataPropertiesDescription =
                    getDataPropertyDescription(ontology, classAxiom.dataPropertiesInSignature)
                if (dataPropertiesDescription != "")
                    javaClass
                        .addProperty(classDataPropertyValue, classRelationshipPropertyName)
                        .field
                        .setProtected()
                        .javaDoc
                        .setText(dataPropertiesDescription)
                else
                    javaClass
                        .addProperty(classDataPropertyValue, classRelationshipPropertyName)
                        .field
                        .setProtected()
            } else if (superClass.classExpressionType == ClassExpressionType.DATA_HAS_VALUE) {
                // little but hacky,
                classRelationshipPropertyName = getClassDataPropertyName(superClass)
                classDataPropertyValue = getClassDataPropertyValue(superClass as OWLDataHasValue)
                if (classDataPropertyValue == "string")
                    classDataPropertyValue = StringUtils.capitalize(classDataPropertyValue)
                val property =
                    javaClass
                        .addProperty(classDataPropertyValue, classRelationshipPropertyName)
                        .field
                        .setProtected()

                // check, if the type is a Map, then we need to ignore it in neo4j for now
                if (classDataPropertyValue.startsWith("java.util.Map")) {
                    property.addAnnotation("org.neo4j.ogm.annotation.Transient")
                }

                // Set data properties description, e.g., for the property interval from the
                // AutomaticUpdates class
                val dataPropertiesDescription =
                    getDataPropertyDescription(ontology, classAxiom.dataPropertiesInSignature)
                if (dataPropertiesDescription != "")
                    property.javaDoc.text = dataPropertiesDescription
            }
        }
        return javaClass
    }

    // Set OWl class object properties
    // These are the ontology class relationship fields in Webprotege.
    // Based on the relationship property it is decided if
    // * it is an interface
    // * the value is written as plural
    // * the value is capitalized
    // * nothing changes
    private fun setOWLClassObjectProperties(
        gs: ClassAbstractRepresentation,
        clazz: OWLClass,
        classes: Set<OWLClass>,
    ): ClassAbstractRepresentation {
        val propertiesList: MutableList<Properties> = ArrayList()

        // Get sorted List of OWLClassAxioms
        val tempAx =
            ontology!!
                .getAxioms(clazz, Imports.EXCLUDED)
                .stream()
                .sorted(Comparator.comparing { e: OWLClassAxiom -> e.axiomWithoutAnnotations })
                .collect(Collectors.toList())
        var classRelationshipPropertyName: String

        // Currently, it is assumed that there is only one parent, but there can be several
        // relationships
        for (classAxiom in tempAx) {
            if (classAxiom.isOfType(AxiomType.EQUIVALENT_CLASSES)) {
                continue
            }
            val property = Properties()
            val ce = classAxiom as OWLSubClassOfAxiomImpl
            val superClass = ce.superClass

            // If type is OBJECT_SOME_VALUES_FROM it is an 'OWL object property' (the webprotege
            // class relationship property)
            if (superClass.classExpressionType == ClassExpressionType.OBJECT_SOME_VALUES_FROM) {
                classRelationshipPropertyName = getClassObjectPropertyName(superClass)
                property.isRootClassNameResource =
                    isRootClassNameResource(
                        (superClass as OWLObjectSomeValuesFromImpl).filler.asOWLClass(),
                        classes,
                    )
                when (classRelationshipPropertyName) {
                    "has",
                    "runsOn",
                    "to",
                    "offers" -> {
                        property.propertyName =
                            decapitalizeString(formatString(getClassName(superClass, ontology)))
                        property.propertyType = formatString(getClassName(superClass, ontology))
                    }
                    "hasMultiple",
                    "offersMultiple" -> {
                        property.propertyName =
                            getPlural(
                                decapitalizeString(formatString(getClassName(superClass, ontology)))
                            )
                        property.propertyType = formatString(getClassName(superClass, ontology))
                    }
                    "collectionOf" -> {
                        property.propertyName =
                            getPlural(
                                decapitalizeString(formatString(getClassName(superClass, ontology)))
                            )
                        property.propertyType =
                            formatString(getSliceClassName(superClass, ontology))
                    }
                    "offersInterface" -> {
                        property.propertyName =
                            decapitalizeString(formatString(getClassName(superClass, ontology)))
                        property.propertyType =
                            "Is" + formatString(getClassName(superClass, ontology))
                        property.isInterface = true
                        interfaceList.add(getClassName(superClass, ontology))
                    }
                    else -> {
                        // TODO: store this information in the property itself, i.e. if it is an
                        // array or not. for now all are arrays
                        property.propertyType = formatString(getClassName(superClass, ontology))
                        property.propertyName = classRelationshipPropertyName
                    }
                }
                property.propertyProperty = classRelationshipPropertyName
                propertiesList.add(property)
            }
        }
        gs.objectProperties = propertiesList
        return gs
    }

    // Set OWl class object properties as java class variable
    private fun setOWLClassObjectProperties(
        javaClass: JavaClassSource,
        clazz: OWLClass,
    ): JavaClassSource {

        // Get sorted List of OWLClassAxioms
        val tempAx =
            ontology!!
                .getAxioms(clazz, Imports.EXCLUDED)
                .stream()
                .sorted(Comparator.comparing { e: OWLClassAxiom -> e.axiomWithoutAnnotations })
                .collect(Collectors.toList())
        var classRelationshipPropertyName: String

        // Currently, it is assumed that there is only one parent, but there can be several
        // relationships
        for (classAxiom in tempAx) {
            if (classAxiom.isOfType(AxiomType.EQUIVALENT_CLASSES)) {
                continue
            }
            val ce = classAxiom as OWLSubClassOfAxiomImpl
            val superClass = ce.superClass

            // If type is OBJECT_SOME_VALUES_FROM it is an 'OWL object property'
            if (superClass.classExpressionType == ClassExpressionType.OBJECT_SOME_VALUES_FROM) {
                classRelationshipPropertyName = getClassObjectPropertyName(superClass)

                // Add property
                val property: PropertySource<JavaClassSource?>? =
                    when (classRelationshipPropertyName) {
                        "has",
                        "offers" ->
                            javaClass.addProperty(
                                formatString(getClassName(superClass, ontology)),
                                decapitalizeString(formatString(getClassName(superClass, ontology))),
                            )
                        "hasMultiple",
                        "offersMultiple" ->
                            javaClass.addProperty(
                                formatString(getArrayClassName(superClass, ontology)),
                                decapitalizeString(
                                    formatString(getPlural(getClassName(superClass, ontology)))
                                ),
                            )
                        else -> // TODO: store this information in the property itself, i.e. if it
                            // is an array or not. for now all are arrays
                            javaClass.addProperty(
                                formatString(getArrayClassName(superClass, ontology)),
                                classRelationshipPropertyName,
                            )
                    }
                property?.field?.setProtected()
            }
        }
        return javaClass
    }

    // Returns the plural of a string, except the string contains the word 'Storage' (case
    // insensitive).
    private fun getPlural(s: String): String {
        return if (s.contains("storage", true)) {
            return s
        } else if (s[s.length - 1] == 'y') {
            s.substring(0, s.length - 1) + "ies"
        } else s + "s"
    }

    // decapitalizes strings and decapitalizes the first 2 characters if the first 3 characters are
    // upper case
    private fun decapitalizeString(string: String?): String {
        return if (string.isNullOrEmpty()) {
            ""
        } else if (string[0].isUpperCase() && string[1].isUpperCase() && string[2].isUpperCase()) {
            string[0].lowercaseChar().toString() +
                string[1].lowercaseChar().toString() +
                string.substring(2)
        } else {
            string[0].lowercaseChar().toString() + string.substring(1)
        }
    }

    // Returns true if the root class name of an object is Resource.
    // Note: It doesn't matter in which level it is located, it only checks if the object furthest
    // up is a Resource.
    private fun isRootClassNameResource(clazz: OWLClass, classes: Set<OWLClass>): Boolean {
        var rootClassName: String
        rootClassName = getParentClassName(clazz)
        if (rootClassName == resourceNameFromOwlFile) {
            return true
        } else if (clazz.isOWLThing) {
            return false
        } else if (rootClassName == "") {
            return false
        }
        for (claz in classes) {
            if (getFormatedClassName(claz) == rootClassName) {
                rootClassName = getParentClassName(claz)
                if (isRootClassNameResource(claz, classes)) {
                    return true
                }
            }
        }
        return false
    }

    // Returns the parent class object (one level above), e.g., for the OWLClass VirtualMachine it
    // returns the OWLClass Compute
    private fun getParentClass(clazz: OWLClass): OWLClass? {
        // Get Set of OWLClassAxioms
        val tempAx = ontology!!.getAxioms(clazz, Imports.EXCLUDED)
        var superClassEntity: OWLClass? = null

        // Currently, it is assumed that there is only one 'OWL parent', but there can be several
        // 'OWL relationships'
        for (classAxiom in tempAx) {
            if (classAxiom.isOfType(AxiomType.EQUIVALENT_CLASSES)) {
                continue
            }
            val ce = classAxiom as OWLSubClassOfAxiomImpl
            val superClass = ce.superClass

            // If type is OWL_CLASS it is the 'OWL parent'
            if (superClass.classExpressionType == ClassExpressionType.OWL_CLASS) {
                superClassEntity = superClass as OWLClass
            }
        }

        return superClassEntity
    }

    // Returns the parent class name, e.g., the parent of VirtualMachine is Compute
    private fun getParentClassName(clazz: OWLClass): String {
        val parent = getParentClass(clazz)
        if (parent != null) {
            // Format class name
            return formatString(getClassName(parent, ontology))
        }

        return ""
    }

    // Deletes not needed characters from string, e.g. space, '/', '-'
    private fun formatString(unformattedString: String): String {
        var formattedString = unformattedString
        if (formattedString.contains(" ")) formattedString = formattedString.replace(" ", "")
        if (formattedString.contains("/")) formattedString = formattedString.replace("/", "")
        if (formattedString.contains("-")) formattedString = formattedString.replace("-", "")
        if (formattedString.contains(")")) formattedString = formattedString.replace(")", "")
        if (formattedString.contains(">")) formattedString = formattedString.replace(">", "")
        if (formattedString.contains("<")) formattedString = formattedString.replace("<", "")

        return formattedString
    }

    private fun getFormatedClassName(clazz: OWLClass): String {
        var objectName = getClassName(clazz, ontology)

        // Format class name
        if (objectName.contains("#")) objectName = objectName.split("#").toTypedArray()[1]
        objectName = formatString(objectName)
        return objectName
    }

    private fun getSliceClassName(nce: OWLClassExpression, ontology: OWLOntology?): String {
        return "[]" + getClassName(nce, ontology)
    }

    private fun getArrayClassName(nce: OWLClassExpression, ontology: OWLOntology?): String {
        return "java.util.List<" + getClassName(nce, ontology) + ">"
    }

    // Get class name from OWLClassExpression
    private fun getClassName(nce: OWLClassExpression, ontology: OWLOntology?): String {
        for (elem in nce.classesInSignature) {
            for (item in EntitySearcher.getAnnotationObjects(elem, ontology!!)) {
                if (item != null) {
                    if (item.property.iri.remainder.get() == "label") {
                        return if (item.value.toString().contains("\""))
                            item.value.toString().split("\"").toTypedArray()[1]
                        else (item.value as OWLLiteralImplString).literal
                    }
                }
            }
        }
        return formatString(nce.toString().split("/").toTypedArray().last())
    }

    // TODO(all): Refactor methods getClassDescription() and getDataPropertyDescription()
    // Get class description from OWLClassExpression
    private fun getClassDescription(nce: OWLClassExpression, ontology: OWLOntology?): String {
        val description: String
        for (elem in nce.classesInSignature) {
            for (item in EntitySearcher.getAnnotationObjects(elem, ontology!!)) {
                if (item != null) {
                    if (
                        item.property.iri.remainder.get() == "comment" ||
                            item.property.iri.remainder.get() == "description"
                    ) {
                        description = item.value.toString()
                        return description.substring(1, description.length - 1)
                    }
                }
            }
        }
        return ""
    }

    // Get description from a data property, e.g., interval
    private fun getDataPropertyDescription(
        ontology: OWLOntology?,
        props: MutableSet<OWLDataProperty>,
    ): String {
        val description: String

        for (elem in ontology!!.dataPropertiesInSignature) {
            for (item in EntitySearcher.getAnnotationObjects(props.elementAt(0), ontology)) {
                if (item != null) {
                    if (
                        item.property.iri.remainder.get() == "comment" ||
                            item.property.iri.remainder.get() == "description"
                    ) {
                        description = item.value.toString()
                        return description.substring(1, description.length - 1)
                    }
                }
            }
        }
        return ""
    }

    // Get class data property value (relationship in OWL)
    private fun getClassDataPropertyValue(nce: OWLDataHasValue): String {
        return nce.filler.toString().split(":").toTypedArray()[1].replace("\"", "")
    }

    private fun getClassDataPropertyValue(nce: OWLDataSomeValuesFrom): String {
        return nce.filler.toString().split(":").toTypedArray()[1]
    }

    // Get class data property name (realtionship in OWL)
    private fun getClassDataPropertyName(nce: OWLClassExpression): String {
        for (elem in nce.dataPropertiesInSignature) {
            return elem.iri.fragment
        }
        return ""
    }

    // Get class object property name (realtionship in owl)
    private fun getClassObjectPropertyName(nce: OWLClassExpression): String {
        for (elem in nce.objectPropertiesInSignature) {
            for (item in EntitySearcher.getAnnotationObjects(elem, ontology!!)) {
                if (item != null && item.property.iri.remainder.get() != "comment") {
                    return item.value.toString().split("\"").toTypedArray()[1]
                }
            }
        }
        return ""
    }

    // Add superclass imports and add superclass call
    private fun addSuperclassProperties(
        jcsList: MutableList<JavaClassSource>
    ): MutableList<JavaClassSource> {
        for (jcs in jcsList) {

            // If superclass is 'Node', do nothing
            if (StringUtils.substringAfterLast(jcs.superType, ".") == "Node") continue
            // HCKY HACK HACK
            if (jcs.name == "HttpRequest") {
                continue
            }

            // Set superclass call
            addSuperclassCall(jcs, jcsList)

            // Add imports from superclasses to javaclass
            addImportsFromSuperclass(jcs, jcsList)
        }
        return jcsList
    }
}
