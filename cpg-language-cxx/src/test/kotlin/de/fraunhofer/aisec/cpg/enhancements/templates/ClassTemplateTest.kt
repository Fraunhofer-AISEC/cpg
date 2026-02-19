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
package de.fraunhofer.aisec.cpg.enhancements.templates

import de.fraunhofer.aisec.cpg.frontends.cxx.CPPLanguage
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.graph.types.FunctionType
import de.fraunhofer.aisec.cpg.graph.types.ObjectType
import de.fraunhofer.aisec.cpg.graph.types.ParameterizedType
import de.fraunhofer.aisec.cpg.graph.types.PointerType
import de.fraunhofer.aisec.cpg.graph.types.PointerType.PointerOrigin
import de.fraunhofer.aisec.cpg.test.*
import java.nio.file.Path
import kotlin.test.*

internal class ClassTemplateTest : BaseTest() {
    private val topLevel = Path.of("src", "test", "resources", "templates", "classtemplates")

    private fun testTemplateStructure(
        template: RecordTemplate,
        pair: Record?,
        type1: TypeParameter?,
        type2: TypeParameter?,
    ) {
        assertEquals(2, template.parameters.size)
        assertEquals(type1, template.parameters[0] as TypeParameter?)
        assertEquals(type2, template.parameters[1] as TypeParameter?)
        assertEquals(1, template.realizations.size)
        assertNotNull(pair)
        assertEquals(pair, template.realizations[0])
    }

    private fun testClassTemplateFields(
        pair: Record,
        first: Field?,
        second: Field?,
    ) {
        assertTrue(pair.fields.contains(first))
        assertTrue(pair.fields.contains(second))
    }

    private fun testClassTemplatesTypes(
        pair: Record?,
        receiver: Variable,
        type1: TypeParameter,
        type2: TypeParameter,
    ): ObjectType {
        assertLocalName("Pair*", receiver.type)
        assertTrue(receiver.type is PointerType)

        val pairType = (receiver.type as PointerType).elementType as? ObjectType
        assertNotNull(pairType)

        assertLocalName("Type1", type1.type)
        assertLocalName("Type2", type2.type)
        assertEquals(type1.type, pairType.generics[0])
        assertEquals(type2.type, pairType.generics[1])
        assertEquals(pair, pairType.recordDeclaration)

        return pairType
    }

    private fun testClassTemplateConstructor(
        pair: Record,
        pairType: ObjectType?,
        pairConstructorDeclaration: Constructor,
    ) {
        assertEquals(pair, pairConstructorDeclaration.recordDeclaration)
        assertTrue(pair.constructors.contains(pairConstructorDeclaration))

        val type = pairConstructorDeclaration.type as? FunctionType
        assertNotNull(type)
        assertEquals(pairType, type.returnTypes.firstOrNull())
    }

    private fun testClassTemplateInvocation(
        pairConstructorDeclaration: Constructor?,
        constructExpression: ConstructExpression,
        pair: Record?,
        pairType: ObjectType,
        template: RecordTemplate?,
        point1: Variable,
    ) {
        assertEquals(pairConstructorDeclaration, constructExpression.constructor)
        assertNotNull(pairConstructorDeclaration)
        assertTrue(constructExpression.invokes.contains(pairConstructorDeclaration))
        assertEquals(pair, constructExpression.instantiates)
        assertEquals(template, constructExpression.templateInstantiation)
        assertLocalName("Pair", constructExpression.type)
        assertEquals(constructExpression.type, point1.type)
        assertNotEquals(pairType, constructExpression.type)

        val instantiatedType = constructExpression.type as? ObjectType
        assertNotNull(instantiatedType)
        assertEquals(2, instantiatedType.generics.size)
        assertLocalName("int", instantiatedType.generics[0])
        assertLocalName("int", instantiatedType.generics[1])

        val templateParameters = constructExpression.templateArguments
        assertNotNull(templateParameters)
        assertEquals(2, templateParameters.size)
        assertLocalName("int", (templateParameters[0] as TypeExpression).type)
        assertLocalName("int", (templateParameters[1] as TypeExpression).type)
        assertTrue(templateParameters[0].isImplicit)
        assertTrue(templateParameters[1].isImplicit)
        assertEquals(2, point1.templateParameters.size)
        assertLocalName("int", (point1.templateParameters[0] as TypeExpression).type)
        assertLocalName("int", (point1.templateParameters[1] as TypeExpression).type)
        assertFalse(point1.templateParameters[0].isImplicit)
        assertFalse(point1.templateParameters[1].isImplicit)
    }

    @Test
    @Throws(Exception::class)
    fun testClassTemplateStructure() {
        val result =
            analyze(listOf(Path.of(topLevel.toString(), "pair.cpp").toFile()), topLevel, true) {
                it.registerLanguage<CPPLanguage>()
            }
        val recordTemplateDeclarations = result.allChildren<RecordTemplate>()
        val template =
            findByUniqueName(
                recordTemplateDeclarations,
                "template<class Type1, class Type2> class Pair",
            )
        val pair = findByUniqueName(result.records, "Pair")
        val type1 = findByUniqueName(result.allChildren<TypeParameter>(), "class Type1")
        val type2 = findByUniqueName(result.allChildren<TypeParameter>(), "class Type2")
        val first = findByUniqueName(result.fields, "first")
        val second = findByUniqueName(result.fields, "second")
        val constructor = pair.constructors["Pair"]
        assertNotNull(constructor)

        val receiver = constructor.receiver
        assertNotNull(receiver)

        val pairConstructorDecl =
            findByUniqueName(result.allChildren<Constructor>(), "Pair")
        val constructExpression =
            findByUniquePredicate(result.allChildren()) { c: ConstructExpression ->
                c.code == "Pair()"
            }
        val point1 = findByUniqueName(result.variables, "point1")

        // Test Template Structure
        testTemplateStructure(template, pair, type1, type2)

        // Test Fields
        testClassTemplateFields(pair, first, second)

        // Test Types
        val pairType = testClassTemplatesTypes(pair, receiver, type1, type2)

        // Test Constructor
        testClassTemplateConstructor(pair, pairType, pairConstructorDecl)

        // Test Invocation
        testClassTemplateInvocation(
            pairConstructorDecl,
            constructExpression,
            pair,
            pairType,
            template,
            point1,
        )
    }

    @Test
    @Throws(Exception::class)
    fun testClassTemplateWithValueParameter() {
        // Test pair2.cpp: Add Value Parameter to Template Instantiation
        val result =
            analyze(listOf(Path.of(topLevel.toString(), "pair2.cpp").toFile()), topLevel, true) {
                it.registerLanguage<CPPLanguage>()
            }
        val recordTemplateDeclarations = result.allChildren<RecordTemplate>()
        val template =
            findByUniqueName(
                recordTemplateDeclarations,
                "template<class Type1, class Type2, int N> class Pair",
            )
        val pair = findByUniqueName(result.records, "Pair")
        val paramN = findByUniqueName(result.parameters, "N")
        val n = findByUniqueName(result.fields, "n")
        val receiver = pair.constructors["Pair"]?.receiver
        assertNotNull(receiver)

        val pairConstructorDecl =
            findByUniqueName(result.allChildren<Constructor>(), "Pair")
        val constructExpr =
            findByUniquePredicate(result.allChildren<ConstructExpression>()) { it.code == "Pair()" }
        val literal3 = findByUniquePredicate(result.literals) { it.value == 3 && !it.isImplicit }
        val literal3Implicit =
            findByUniquePredicate(result.literals) { it.value == 3 && it.isImplicit }
        val point1 = findByUniqueName(result.variables, "point1")
        assertEquals(3, template.parameters.size)
        assertEquals(paramN, template.parameters[2])
        assertTrue(pair.fields.contains(n))
        assertEquals(paramN, (n.initializer as? Reference)?.refersTo)

        // Test Type
        val type = ((receiver.type as? PointerType)?.elementType as? ObjectType)
        assertNotNull(type)
        assertEquals((pairConstructorDecl.type as? FunctionType)?.returnTypes?.firstOrNull(), type)
        assertEquals(pair, type.recordDeclaration)
        assertEquals(2, type.generics.size)
        assertLocalName("Type1", type.generics[0])
        assertLocalName("Type2", type.generics[1])
        val instantiatedType = constructExpr.type as ObjectType
        assertEquals(instantiatedType, point1.type)
        assertEquals(2, instantiatedType.generics.size)
        assertLocalName("int", instantiatedType.generics[0])
        assertLocalName("int", instantiatedType.generics[1])

        // Test TemplateParameter of Variable
        assertEquals(3, point1.templateParameters.size)
        assertEquals(literal3, point1.templateParameters[2])

        // Test Invocation
        val templateParameters = constructExpr.templateArguments
        assertNotNull(templateParameters)
        assertEquals(3, templateParameters.size)
        assertEquals(literal3Implicit, templateParameters[2])
        assertEquals(
            Template.TemplateInitialization.EXPLICIT,
            constructExpr.templateArgumentEdges?.get(2)?.instantiation,
        )
        assertEquals(pair, constructExpr.instantiates)
        assertEquals(template, constructExpr.templateInstantiation)
    }

    private fun testStructTemplateWithSameDefaultTypeInvocation(
        template: RecordTemplate?,
        pair: Record?,
        pairConstructorDeclaration: Constructor?,
        constructExpression: ConstructExpression,
        point1: Variable,
    ) {
        assertEquals(pair, constructExpression.instantiates)
        assertEquals(template, constructExpression.templateInstantiation)
        assertEquals(pairConstructorDeclaration, constructExpression.constructor)
        assertEquals(2, constructExpression.templateArguments.size)
        assertLocalName("int", constructExpression.templateArguments[0])
        assertEquals(
            Template.TemplateInitialization.EXPLICIT,
            constructExpression.templateArgumentEdges?.get(0)?.instantiation,
        )
        assertLocalName("int", constructExpression.templateArguments[1])
        assertEquals(
            Template.TemplateInitialization.EXPLICIT,
            constructExpression.templateArgumentEdges?.get(1)?.instantiation,
        )

        val pairTypeInstantiated = constructExpression.type as ObjectType
        assertEquals(pair, pairTypeInstantiated.recordDeclaration)
        assertEquals(2, pairTypeInstantiated.generics.size)
        assertLocalName("int", pairTypeInstantiated.generics[0])
        assertLocalName("int", pairTypeInstantiated.generics[1])
        assertEquals(pairTypeInstantiated, point1.type)
    }

    @Test
    @Throws(Exception::class)
    fun testStructTemplateWithSameDefaultType() {
        // Test pair3.cpp: Template a struct instead of a class and use a Type1 as default of Type2
        val result =
            analyze(listOf(Path.of(topLevel.toString(), "pair3.cpp").toFile()), topLevel, true) {
                it.registerLanguage<CPPLanguage>()
            }
        val template =
            findByUniqueName(
                result.allChildren<RecordTemplate>(),
                "template<class Type1, class Type2 = Type1> struct Pair",
            )
        val pair = findByUniqueName(result.records, "Pair")
        val pairConstructorDecl =
            findByUniqueName(result.allChildren<Constructor>(), "Pair")
        val type1 = findByUniqueName(result.allChildren<TypeParameter>(), "class Type1")
        val type2 =
            findByUniqueName(result.allChildren<TypeParameter>(), "class Type2 = Type1")
        val first = findByUniqueName(result.fields, "first")
        val second = findByUniqueName(result.fields, "second")
        val point1 = findByUniqueName(result.variables, "point1")
        val constructExpr =
            findByUniquePredicate(result.allChildren<ConstructExpression>()) { it.code == "Pair()" }
        assertEquals(1, template.realizations.size)
        assertEquals(pair, template.realizations[0])
        assertEquals(2, template.parameters.size)
        assertEquals(type1, template.parameters[0])
        assertEquals(type2, template.parameters[1])
        assertLocalName("Type1", type1.type)

        val type1ParameterizedType = type1.type as? ParameterizedType
        assertNotNull(type1ParameterizedType)
        assertLocalName("Type2", type2.type)

        val type2ParameterizedType = type2.type as? ParameterizedType
        assertNotNull(type2ParameterizedType)
        assertEquals(type1ParameterizedType, type2.default?.type)

        val pairType =
            (pairConstructorDecl.type as FunctionType).returnTypes.firstOrNull() as? ObjectType
        assertNotNull(pairType)
        assertEquals(2, pairType.generics.size)
        assertEquals(type1ParameterizedType, pairType.generics[0])
        assertEquals(type2ParameterizedType, pairType.generics[1])
        assertEquals(2, pair.fields.size)
        assertEquals(first, pair.fields[0])
        assertEquals(second, pair.fields[1])
        assertEquals(type1ParameterizedType, first.type)
        assertEquals(type2ParameterizedType, second.type)
        testStructTemplateWithSameDefaultTypeInvocation(
            template,
            pair,
            pairConstructorDecl,
            constructExpr,
            point1,
        )
    }

    @Test
    @Throws(Exception::class)
    fun testTemplateOverridingDefaults() {
        // Test pair3-1.cpp: Override defaults of template
        val result =
            analyze(listOf(Path.of(topLevel.toString(), "pair3-1.cpp").toFile()), topLevel, true) {
                it.registerLanguage<CPPLanguage>()
            }
        val template =
            findByUniqueName(
                result.allChildren<RecordTemplate>(),
                "template<class Type1, class Type2 = Type1, int A=1, int B=A> struct Pair",
            )
        val pair = findByUniqueName(result.records, "Pair")
        val constructExpr =
            findByUniquePredicate(result.allChildren<ConstructExpression>()) { it.code == "Pair()" }
        val literal2 = findByUniquePredicate(result.literals) { it.value == 2 && !it.isImplicit }
        assertNotNull(literal2)
        val literal2Implicit =
            findByUniquePredicate(result.literals) { it.value == 2 && it.isImplicit }
        assertEquals(pair, constructExpr.instantiates)
        assertEquals(template, constructExpr.templateInstantiation)
        assertEquals(4, constructExpr.templateArguments.size)
        assertLocalName("int", constructExpr.templateArguments[0])
        assertEquals(
            Template.TemplateInitialization.EXPLICIT,
            constructExpr.templateArgumentEdges?.get(0)?.instantiation,
        )
        assertLocalName("int", constructExpr.templateArguments[1])
        assertEquals(
            Template.TemplateInitialization.EXPLICIT,
            constructExpr.templateArgumentEdges?.get(1)?.instantiation,
        )
        assertEquals(literal2Implicit, constructExpr.templateArguments[2])
        assertEquals(
            Template.TemplateInitialization.EXPLICIT,
            constructExpr.templateArgumentEdges?.get(2)?.instantiation,
        )
        assertEquals(literal2Implicit, constructExpr.templateArguments[3])
        assertEquals(
            Template.TemplateInitialization.DEFAULT,
            constructExpr.templateArgumentEdges?.get(3)?.instantiation,
        )

        val type = constructExpr.type as ObjectType
        assertEquals(pair, type.recordDeclaration)
        assertEquals(2, type.generics.size)
        assertLocalName("int", type.generics[0])
        assertLocalName("int", type.generics[1])
    }

    @Test
    @Throws(Exception::class)
    fun testTemplateRecursiveDefaults() {
        // Test pair3-2.cpp: Use recursive template parameters using defaults
        val result =
            analyze(listOf(Path.of(topLevel.toString(), "pair3-2.cpp").toFile()), topLevel, true) {
                it.registerLanguage<CPPLanguage>()
            }
        val template =
            findByUniqueName(
                result.allChildren<RecordTemplate>(),
                "template<class Type1, class Type2 = Type1, int A=1, int B=A> struct Pair",
            )
        val pair = findByUniqueName(result.records, "Pair")
        val paramA = findByUniqueName(result.parameters, "A")
        val paramB = findByUniqueName(result.parameters, "B")
        val constructExpression =
            findByUniquePredicate(result.allChildren()) { c: ConstructExpression ->
                c.code == "Pair()"
            }
        val literal1 = findByUniquePredicate(result.literals) { it.value == 1 }
        assertEquals(4, template.parameters.size)
        assertEquals(paramA, template.parameters[2])
        assertEquals(literal1, paramA.default)
        assertEquals(paramB, template.parameters[3])
        assertEquals(paramA, (paramB.default as Reference).refersTo)
        assertEquals(pair, constructExpression.instantiates)
        assertEquals(template, constructExpression.templateInstantiation)
        assertEquals(4, constructExpression.templateArguments.size)
        assertLocalName("int", (constructExpression.templateArguments[0] as TypeExpression).type)
        assertEquals(
            Template.TemplateInitialization.EXPLICIT,
            constructExpression.templateArgumentEdges?.get(0)?.instantiation,
        )
        assertEquals(0, constructExpression.templateArgumentEdges?.get(0)?.index)
        assertLocalName("int", (constructExpression.templateArguments[1] as TypeExpression).type)
        assertEquals(
            Template.TemplateInitialization.DEFAULT,
            constructExpression.templateArgumentEdges?.get(1)?.instantiation,
        )
        assertEquals(1, constructExpression.templateArgumentEdges?.get(1)?.index)
        assertEquals(literal1, constructExpression.templateArguments[2])
        assertEquals(
            Template.TemplateInitialization.DEFAULT,
            constructExpression.templateArgumentEdges?.get(2)?.instantiation,
        )
        assertEquals(2, constructExpression.templateArgumentEdges?.get(2)?.index)
        assertEquals(literal1, constructExpression.templateArguments[3])
        assertEquals(
            Template.TemplateInitialization.DEFAULT,
            constructExpression.templateArgumentEdges?.get(3)?.instantiation,
        )
        assertEquals(3, constructExpression.templateArgumentEdges?.get(3)?.index)

        // Test Type
        val type = constructExpression.type as ObjectType
        assertEquals(2, type.generics.size)
        assertLocalName("int", type.generics[0])
        assertLocalName("int", type.generics[1])
    }

    @Test
    @Throws(Exception::class)
    fun testReferenceInTemplates() {
        // Test array.cpp: checks usage of referencetype of parameterized type (T[])
        val result =
            analyze(listOf(Path.of(topLevel.toString(), "array.cpp").toFile()), topLevel, true) {
                it.registerLanguage<CPPLanguage>()
            }
        val template =
            findByUniqueName(
                result.allChildren<RecordTemplate>(),
                "template<typename T, int N=10> class Array",
            )
        val array = findByUniqueName(result.records, "Array")
        val paramN = findByUniqueName(result.parameters, "N")
        val paramT = findByUniqueName(result.allChildren<TypeParameter>(), "typename T")
        val literal10 = findByUniquePredicate(result.literals) { it.value == 10 }
        val mArray = findByUniqueName(result.fields, "m_Array")
        assertEquals(2, template.parameters.size)
        assertEquals(paramT, template.parameters[0])
        assertEquals(paramN, template.parameters[1])
        assertEquals(literal10, paramN.default)
        assertEquals(1, array.fields.size)
        assertEquals(mArray, array.fields[0])

        val receiver = array.methods["GetSize"]?.receiver
        assertNotNull(receiver)

        val arrayType = ((receiver.type as? PointerType)?.elementType) as? ObjectType
        assertNotNull(arrayType)
        assertEquals(1, arrayType.generics.size)
        assertLocalName("T", arrayType.generics[0])

        val typeT = arrayType.generics[0] as ParameterizedType
        assertEquals(typeT, paramT.type)
        assertTrue(mArray.type is PointerType)

        val tArray = mArray.type as PointerType
        assertEquals(typeT, tArray.elementType)

        val constructExpr =
            findByUniquePredicate(result.allChildren<ConstructExpression>()) {
                it.code == "Array()"
            }
        assertEquals(template, constructExpr.templateInstantiation)
        assertEquals(array, constructExpr.instantiates)
        assertLocalName("int", constructExpr.templateArguments[0])
        assertEquals(literal10, constructExpr.templateArguments[1])
        assertLocalName("Array", constructExpr.type)

        val instantiatedType = constructExpr.type as ObjectType
        assertEquals(1, instantiatedType.generics.size)
        assertLocalName("int", instantiatedType.generics[0])
    }

    @Test
    @Throws(Exception::class)
    fun testTemplateInstantiationWithNew() {
        // Test array2.cpp: Test template usage with new keyword
        val result =
            analyze(listOf(Path.of(topLevel.toString(), "array2.cpp").toFile()), topLevel, true) {
                it.registerLanguage<CPPLanguage>()
            }
        val template =
            findByUniqueName(
                result.allChildren<RecordTemplate>(),
                "template<typename T, int N=10> class Array",
            )
        val array = findByUniqueName(result.records, "Array")
        val constructExpression =
            findByUniquePredicate(result.allChildren()) { c: ConstructExpression ->
                c.code == "Array()"
            }
        val literal5 =
            findByUniquePredicate(result.literals) {
                it.value == 5 && it.location!!.region.endColumn == 41 && !it.isImplicit
            }
        assertNotNull(literal5)
        val literal5Declaration =
            findByUniquePredicate(result.literals) {
                it.value == 5 && it.location!!.region.endColumn == 14 && !it.isImplicit
            }
        val literal5Implicit =
            findByUniquePredicate(result.literals) {
                it.value == 5 && it.location!!.region.endColumn == 41 && it.isImplicit
            }
        val arrayVariable = findByUniqueName(result.variables, "array")
        val newExpression = findByUniqueName(result.allChildren<NewExpression>(), "")
        assertEquals(array, constructExpression.instantiates)
        assertEquals(template, constructExpression.templateInstantiation)
        assertEquals(2, constructExpression.templateArguments.size)
        assertLocalName("int", (constructExpression.templateArguments[0] as TypeExpression).type)
        assertTrue(constructExpression.templateArguments[0].isImplicit)
        assertEquals(literal5Implicit, constructExpression.templateArguments[1])
        assertEquals(2, arrayVariable.templateParameters.size)
        assertLocalName("int", (arrayVariable.templateParameters[0] as TypeExpression).type)
        assertFalse(arrayVariable.templateParameters[0].isImplicit)
        assertEquals(literal5Declaration, arrayVariable.templateParameters[1])
        assertLocalName("Array", constructExpression.type)

        val arrayType = constructExpression.type as ObjectType
        assertEquals(1, arrayType.generics.size)
        assertLocalName("int", arrayType.generics[0])
        assertEquals(array, arrayType.recordDeclaration)
        assertEquals(arrayType.reference(PointerOrigin.POINTER), arrayVariable.type)
        assertEquals(arrayType.reference(PointerOrigin.POINTER), newExpression.type)
    }
}
