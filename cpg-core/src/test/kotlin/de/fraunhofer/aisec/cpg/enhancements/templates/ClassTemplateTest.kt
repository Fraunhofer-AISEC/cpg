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

import de.fraunhofer.aisec.cpg.BaseTest
import de.fraunhofer.aisec.cpg.TestUtils.analyze
import de.fraunhofer.aisec.cpg.TestUtils.findByUniqueName
import de.fraunhofer.aisec.cpg.TestUtils.findByUniquePredicate
import de.fraunhofer.aisec.cpg.TestUtils.flattenListIsInstance
import de.fraunhofer.aisec.cpg.graph.byNameOrNull
import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.graph.edge.Properties
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.graph.types.ObjectType
import de.fraunhofer.aisec.cpg.graph.types.ParameterizedType
import de.fraunhofer.aisec.cpg.graph.types.PointerType
import de.fraunhofer.aisec.cpg.graph.types.PointerType.PointerOrigin
import java.nio.file.Path
import kotlin.test.*

internal class ClassTemplateTest : BaseTest() {
    private val topLevel = Path.of("src", "test", "resources", "templates", "classtemplates")

    private fun testTemplateStructure(
        template: ClassTemplateDeclaration,
        pair: RecordDeclaration?,
        type1: TypeParamDeclaration?,
        type2: TypeParamDeclaration?
    ) {
        assertEquals(2, template.parameters.size)
        assertEquals(type1, template.parameters[0])
        assertEquals(type2, template.parameters[1])
        assertEquals(1, template.realization.size)
        assertEquals(pair, template.realization[0])
    }

    private fun testClassTemplateFields(
        pair: RecordDeclaration,
        first: FieldDeclaration?,
        second: FieldDeclaration?
    ) {
        assertTrue(pair.fields.contains(first))
        assertTrue(pair.fields.contains(second))
    }

    private fun testClassTemplatesTypes(
        pair: RecordDeclaration?,
        receiver: VariableDeclaration,
        type1: TypeParamDeclaration,
        type2: TypeParamDeclaration
    ): ObjectType {
        assertEquals("Pair*", receiver.type.name)
        assertTrue(receiver.type is PointerType)

        val pairType = (receiver.type as PointerType).elementType as? ObjectType
        assertNotNull(pairType)

        assertEquals("Type1", type1.type.name)
        assertEquals("Type2", type2.type.name)
        assertEquals(type1.type, pairType.generics[0])
        assertEquals(type2.type, pairType.generics[1])
        assertEquals(pair, pairType.recordDeclaration)

        return pairType
    }

    private fun testClassTemplateConstructor(
        pair: RecordDeclaration,
        pairType: ObjectType?,
        pairConstructorDeclaration: ConstructorDeclaration
    ) {
        assertEquals(pair, pairConstructorDeclaration.recordDeclaration)
        assertTrue(pair.constructors.contains(pairConstructorDeclaration))
        assertEquals(pairType, pairConstructorDeclaration.type)
    }

    private fun testClassTemplateInvocation(
        pairConstructorDeclaration: ConstructorDeclaration?,
        constructExpression: ConstructExpression,
        pair: RecordDeclaration?,
        pairType: ObjectType?,
        template: ClassTemplateDeclaration?,
        point1: VariableDeclaration
    ) {
        assertEquals(pairConstructorDeclaration, constructExpression.constructor)
        assertNotNull(pairConstructorDeclaration)
        assertTrue(constructExpression.invokes.contains(pairConstructorDeclaration))
        assertEquals(pair, constructExpression.instantiates)
        assertEquals(template, constructExpression.templateInstantiation)
        assertEquals("Pair", constructExpression.type.name)
        assertEquals(constructExpression.type, point1.type)
        assertNotEquals(pairType, constructExpression.type)

        val instantiatedType = constructExpression.type as? ObjectType
        assertNotNull(instantiatedType)
        assertEquals(2, instantiatedType.generics.size)
        assertEquals("int", instantiatedType.generics[0].name)
        assertEquals("int", instantiatedType.generics[1].name)

        val templateParameters = constructExpression.templateParameters
        assertNotNull(templateParameters)
        assertEquals(2, templateParameters.size)
        assertEquals("int", (templateParameters[0] as TypeExpression).type.name)
        assertEquals("int", (templateParameters[1] as TypeExpression).type.name)
        assertTrue(templateParameters[0].isImplicit)
        assertTrue(templateParameters[1].isImplicit)
        assertEquals(2, point1.templateParameters.size)
        assertEquals("int", (point1.templateParameters[0] as TypeExpression).type.name)
        assertEquals("int", (point1.templateParameters[1] as TypeExpression).type.name)
        assertFalse(point1.templateParameters[0].isImplicit)
        assertFalse(point1.templateParameters[1].isImplicit)
    }

    @Test
    @Throws(Exception::class)
    fun testClassTemplateStructure() {
        val result =
            analyze(listOf(Path.of(topLevel.toString(), "pair.cpp").toFile()), topLevel, true)
        val classTemplateDeclarations = flattenListIsInstance<ClassTemplateDeclaration>(result)
        val template =
            findByUniqueName(
                classTemplateDeclarations,
                "template<class Type1, class Type2> class Pair"
            )
        val pair = findByUniqueName(flattenListIsInstance<RecordDeclaration>(result), "Pair")
        val type1 =
            findByUniqueName(flattenListIsInstance<TypeParamDeclaration>(result), "class Type1")
        val type2 =
            findByUniqueName(flattenListIsInstance<TypeParamDeclaration>(result), "class Type2")
        val first = findByUniqueName(flattenListIsInstance<FieldDeclaration>(result), "first")
        val second = findByUniqueName(flattenListIsInstance<FieldDeclaration>(result), "second")
        val receiver = pair.byNameOrNull<MethodDeclaration>("Pair")?.receiver
        assertNotNull(receiver)

        val pairConstructorDeclaration =
            findByUniqueName(flattenListIsInstance<ConstructorDeclaration>(result), "Pair")
        val constructExpression =
            findByUniquePredicate(flattenListIsInstance(result)) { c: ConstructExpression ->
                c.code == "()"
            }
        val point1 = findByUniqueName(flattenListIsInstance<VariableDeclaration>(result), "point1")

        // Test Template Structure
        testTemplateStructure(template, pair, type1, type2)

        // Test Fields
        testClassTemplateFields(pair, first, second)

        // Test Types
        val pairType = testClassTemplatesTypes(pair, receiver, type1, type2)

        // Test Constructor
        testClassTemplateConstructor(pair, pairType, pairConstructorDeclaration)

        // Test Invocation
        testClassTemplateInvocation(
            pairConstructorDeclaration,
            constructExpression,
            pair,
            pairType,
            template,
            point1
        )
    }

    @Test
    @Throws(Exception::class)
    fun testClassTemplateWithValueParameter() {
        // Test pair2.cpp: Add Value Parameter to Template Instantiation
        val result =
            analyze(listOf(Path.of(topLevel.toString(), "pair2.cpp").toFile()), topLevel, true)
        val classTemplateDeclarations = flattenListIsInstance<ClassTemplateDeclaration>(result)
        val template =
            findByUniqueName(
                classTemplateDeclarations,
                "template<class Type1, class Type2, int N> class Pair"
            )
        val pair = findByUniqueName(flattenListIsInstance<RecordDeclaration>(result), "Pair")
        val paramN = findByUniqueName(flattenListIsInstance<ParamVariableDeclaration>(result), "N")
        val n = findByUniqueName(flattenListIsInstance<FieldDeclaration>(result), "n")
        val receiver = pair.byNameOrNull<ConstructorDeclaration>("Pair")?.receiver
        assertNotNull(receiver)

        val pairConstructorDeclaration =
            findByUniqueName(flattenListIsInstance<ConstructorDeclaration>(result), "Pair")
        val constructExpression =
            findByUniquePredicate(flattenListIsInstance<ConstructExpression>(result)) {
                it.code == "()"
            }
        val literal3 =
            findByUniquePredicate(flattenListIsInstance<Literal<*>>(result)) {
                it.value == 3 && !it.isImplicit
            }
        val literal3Implicit =
            findByUniquePredicate(flattenListIsInstance<Literal<*>>(result)) {
                it.value == 3 && it.isImplicit
            }
        val point1 = findByUniqueName(flattenListIsInstance<VariableDeclaration>(result), "point1")
        assertEquals(3, template.parameters.size)
        assertEquals(paramN, template.parameters[2])
        assertTrue(pair.fields.contains(n))
        assertEquals(paramN, (n.initializer as? DeclaredReferenceExpression)?.refersTo)

        // Test Type
        val type = ((receiver.type as? PointerType)?.elementType as? ObjectType)
        assertNotNull(type)
        assertEquals(pairConstructorDeclaration.type, type)
        assertEquals(pair, type.recordDeclaration)
        assertEquals(2, type.generics.size)
        assertEquals("Type1", type.generics[0].name)
        assertEquals("Type2", type.generics[1].name)
        val instantiatedType = constructExpression.type as ObjectType
        assertEquals(instantiatedType, point1.type)
        assertEquals(2, instantiatedType.generics.size)
        assertEquals("int", instantiatedType.generics[0].name)
        assertEquals("int", instantiatedType.generics[1].name)

        // Test TemplateParameter of VariableDeclaration
        assertEquals(3, point1.templateParameters.size)
        assertEquals(literal3, point1.templateParameters[2])

        // Test Invocation
        val templateParameters = constructExpression.templateParameters
        assertNotNull(templateParameters)
        assertEquals(3, templateParameters.size)
        assertEquals(literal3Implicit, templateParameters[2])
        assertEquals(
            TemplateDeclaration.TemplateInitialization.EXPLICIT,
            constructExpression.templateParametersEdges
                ?.get(2)
                ?.getProperty(Properties.INSTANTIATION)
        )
        assertEquals(pair, constructExpression.instantiates)
        assertEquals(template, constructExpression.templateInstantiation)
    }

    private fun testStructTemplateWithSameDefaultTypeInvocation(
        template: ClassTemplateDeclaration?,
        pair: RecordDeclaration?,
        pairConstructorDeclaration: ConstructorDeclaration?,
        constructExpression: ConstructExpression,
        point1: VariableDeclaration
    ) {
        assertEquals(pair, constructExpression.instantiates)
        assertEquals(template, constructExpression.templateInstantiation)
        assertEquals(pairConstructorDeclaration, constructExpression.constructor)
        assertEquals(2, constructExpression.templateParameters.size)
        assertEquals("int", constructExpression.templateParameters[0].name)
        assertEquals(
            TemplateDeclaration.TemplateInitialization.EXPLICIT,
            constructExpression.templateParametersEdges
                ?.get(0)
                ?.getProperty(Properties.INSTANTIATION)
        )
        assertEquals("int", constructExpression.templateParameters[1].name)
        assertEquals(
            TemplateDeclaration.TemplateInitialization.EXPLICIT,
            constructExpression.templateParametersEdges
                ?.get(1)
                ?.getProperty(Properties.INSTANTIATION)
        )

        val pairTypeInstantiated = constructExpression.type as ObjectType
        assertEquals(pair, pairTypeInstantiated.recordDeclaration)
        assertEquals(2, pairTypeInstantiated.generics.size)
        assertEquals("int", pairTypeInstantiated.generics[0].name)
        assertEquals("int", pairTypeInstantiated.generics[1].name)
        assertEquals(pairTypeInstantiated, point1.type)
    }

    @Test
    @Throws(Exception::class)
    fun testStructTemplateWithSameDefaultType() {
        // Test pair3.cpp: Template a struct instead of a class and use a Type1 as default of Type2
        val result =
            analyze(listOf(Path.of(topLevel.toString(), "pair3.cpp").toFile()), topLevel, true)
        val template =
            findByUniqueName(
                flattenListIsInstance<ClassTemplateDeclaration>(result),
                "template<class Type1, class Type2 = Type1> struct Pair"
            )
        val pair = findByUniqueName(flattenListIsInstance<RecordDeclaration>(result), "Pair")
        val pairConstructorDeclaration =
            findByUniqueName(flattenListIsInstance<ConstructorDeclaration>(result), "Pair")
        val type1 =
            findByUniqueName(flattenListIsInstance<TypeParamDeclaration>(result), "class Type1")
        val type2 =
            findByUniqueName(
                flattenListIsInstance<TypeParamDeclaration>(result),
                "class Type2 = Type1"
            )
        val first = findByUniqueName(flattenListIsInstance<FieldDeclaration>(result), "first")
        val second = findByUniqueName(flattenListIsInstance<FieldDeclaration>(result), "second")
        val point1 = findByUniqueName(flattenListIsInstance<VariableDeclaration>(result), "point1")
        val constructExpression =
            findByUniquePredicate(flattenListIsInstance<ConstructExpression>(result)) {
                it.code == "()"
            }
        assertEquals(1, template.realization.size)
        assertEquals(pair, template.realization[0])
        assertEquals(2, template.parameters.size)
        assertEquals(type1, template.parameters[0])
        assertEquals(type2, template.parameters[1])
        assertEquals("Type1", type1.type.name)

        val type1ParameterizedType = type1.type as ParameterizedType
        assertEquals("Type2", type2.type.name)

        val type2ParameterizedType = type2.type as ParameterizedType
        assertEquals(type1ParameterizedType, type2.default)

        val pairType = pairConstructorDeclaration.type as ObjectType
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
            pairConstructorDeclaration,
            constructExpression,
            point1
        )
    }

    @Test
    @Throws(Exception::class)
    fun testTemplateOverrindingDefaults() {
        // Test pair3-1.cpp: Override defaults of template
        val result =
            analyze(listOf(Path.of(topLevel.toString(), "pair3-1.cpp").toFile()), topLevel, true)
        val template =
            findByUniqueName(
                flattenListIsInstance<ClassTemplateDeclaration>(result),
                "template<class Type1, class Type2 = Type1, int A=1, int B=A> struct Pair"
            )
        val pair = findByUniqueName(flattenListIsInstance<RecordDeclaration>(result), "Pair")
        val constructExpression =
            findByUniquePredicate(flattenListIsInstance<ConstructExpression>(result)) {
                it.code == "()"
            }
        val literal2 =
            findByUniquePredicate(flattenListIsInstance<Literal<*>>(result)) {
                it.value == 2 && !it.isImplicit
            }
        assertNotNull(literal2)
        val literal2Implicit =
            findByUniquePredicate(flattenListIsInstance<Literal<*>>(result)) {
                it.value == 2 && it.isImplicit
            }
        assertEquals(pair, constructExpression.instantiates)
        assertEquals(template, constructExpression.templateInstantiation)
        assertEquals(4, constructExpression.templateParameters.size)
        assertEquals("int", constructExpression.templateParameters[0].name)
        assertEquals(
            TemplateDeclaration.TemplateInitialization.EXPLICIT,
            constructExpression.templateParametersEdges
                ?.get(0)
                ?.getProperty(Properties.INSTANTIATION)
        )
        assertEquals("int", constructExpression.templateParameters[1].name)
        assertEquals(
            TemplateDeclaration.TemplateInitialization.EXPLICIT,
            constructExpression.templateParametersEdges
                ?.get(1)
                ?.getProperty(Properties.INSTANTIATION)
        )
        assertEquals(literal2Implicit, constructExpression.templateParameters[2])
        assertEquals(
            TemplateDeclaration.TemplateInitialization.EXPLICIT,
            constructExpression.templateParametersEdges
                ?.get(2)
                ?.getProperty(Properties.INSTANTIATION)
        )
        assertEquals(literal2Implicit, constructExpression.templateParameters[3])
        assertEquals(
            TemplateDeclaration.TemplateInitialization.DEFAULT,
            constructExpression.templateParametersEdges
                ?.get(3)
                ?.getProperty(Properties.INSTANTIATION)
        )

        val type = constructExpression.type as ObjectType
        assertEquals(pair, type.recordDeclaration)
        assertEquals(2, type.generics.size)
        assertEquals("int", type.generics[0].name)
        assertEquals("int", type.generics[1].name)
    }

    @Test
    @Throws(Exception::class)
    fun testTemplateRecursiveDefaults() {
        // Test pair3-2.cpp: Use recursive template parameters using defaults
        val result =
            analyze(listOf(Path.of(topLevel.toString(), "pair3-2.cpp").toFile()), topLevel, true)
        val template =
            findByUniqueName(
                flattenListIsInstance<ClassTemplateDeclaration>(result),
                "template<class Type1, class Type2 = Type1, int A=1, int B=A> struct Pair"
            )
        val pair = findByUniqueName(flattenListIsInstance<RecordDeclaration>(result), "Pair")
        val paramA = findByUniqueName(flattenListIsInstance<ParamVariableDeclaration>(result), "A")
        val paramB = findByUniqueName(flattenListIsInstance<ParamVariableDeclaration>(result), "B")
        val constructExpression =
            findByUniquePredicate(flattenListIsInstance(result)) { c: ConstructExpression ->
                c.code == "()"
            }
        val literal1 =
            findByUniquePredicate(flattenListIsInstance(result)) { l: Literal<*> -> l.value == 1 }
        assertEquals(4, template.parameters.size)
        assertEquals(paramA, template.parameters[2])
        assertEquals(literal1, paramA.default)
        assertEquals(paramB, template.parameters[3])
        assertEquals(paramA, (paramB.default as DeclaredReferenceExpression).refersTo)
        assertEquals(pair, constructExpression.instantiates)
        assertEquals(template, constructExpression.templateInstantiation)
        assertEquals(4, constructExpression.templateParameters.size)
        assertEquals("int", (constructExpression.templateParameters[0] as TypeExpression).type.name)
        assertEquals(
            TemplateDeclaration.TemplateInitialization.EXPLICIT,
            constructExpression.templateParametersEdges
                ?.get(0)
                ?.getProperty(Properties.INSTANTIATION)
        )
        assertEquals(
            0,
            constructExpression.templateParametersEdges?.get(0)?.getProperty(Properties.INDEX)
        )
        assertEquals("int", (constructExpression.templateParameters[1] as TypeExpression).type.name)
        assertEquals(
            TemplateDeclaration.TemplateInitialization.DEFAULT,
            constructExpression.templateParametersEdges
                ?.get(1)
                ?.getProperty(Properties.INSTANTIATION)
        )
        assertEquals(
            1,
            constructExpression.templateParametersEdges?.get(1)?.getProperty(Properties.INDEX)
        )
        assertEquals(literal1, constructExpression.templateParameters[2])
        assertEquals(
            TemplateDeclaration.TemplateInitialization.DEFAULT,
            constructExpression.templateParametersEdges
                ?.get(2)
                ?.getProperty(Properties.INSTANTIATION)
        )
        assertEquals(
            2,
            constructExpression.templateParametersEdges?.get(2)?.getProperty(Properties.INDEX)
        )
        assertEquals(literal1, constructExpression.templateParameters[3])
        assertEquals(
            TemplateDeclaration.TemplateInitialization.DEFAULT,
            constructExpression.templateParametersEdges
                ?.get(3)
                ?.getProperty(Properties.INSTANTIATION)
        )
        assertEquals(
            3,
            constructExpression.templateParametersEdges?.get(3)?.getProperty(Properties.INDEX)
        )

        // Test Type
        val type = constructExpression.type as ObjectType
        assertEquals(2, type.generics.size)
        assertEquals("int", type.generics[0].name)
        assertEquals("int", type.generics[1].name)
    }

    @Test
    @Throws(Exception::class)
    fun testReferenceInTemplates() {
        // Test array.cpp: checks usage of referencetype of parameterized type (T[])
        val result =
            analyze(listOf(Path.of(topLevel.toString(), "array.cpp").toFile()), topLevel, true)
        val template =
            findByUniqueName(
                flattenListIsInstance<ClassTemplateDeclaration>(result),
                "template<typename T, int N=10> class Array"
            )
        val array = findByUniqueName(flattenListIsInstance<RecordDeclaration>(result), "Array")
        val paramN = findByUniqueName(flattenListIsInstance<ParamVariableDeclaration>(result), "N")
        val paramT =
            findByUniqueName(flattenListIsInstance<TypeParamDeclaration>(result), "typename T")
        val literal10 =
            findByUniquePredicate(flattenListIsInstance<Literal<*>>(result)) { it.value == 10 }
        val mArray = findByUniqueName(flattenListIsInstance<FieldDeclaration>(result), "m_Array")
        assertEquals(2, template.parameters.size)
        assertEquals(paramT, template.parameters[0])
        assertEquals(paramN, template.parameters[1])
        assertEquals(literal10, paramN.default)
        assertEquals(1, array.fields.size)
        assertEquals(mArray, array.fields[0])

        val receiver = array.byNameOrNull<MethodDeclaration>("GetSize")?.receiver
        assertNotNull(receiver)

        val arrayType = ((receiver.type as? PointerType)?.elementType) as? ObjectType
        assertNotNull(arrayType)
        assertEquals(1, arrayType.generics.size)
        assertEquals("T", arrayType.generics[0].name)

        val typeT = arrayType.generics[0] as ParameterizedType
        assertEquals(typeT, paramT.type)
        assertTrue(mArray.type is PointerType)

        val tArray = mArray.type as PointerType
        assertEquals(typeT, tArray.elementType)

        val constructExpression =
            findByUniquePredicate(flattenListIsInstance<ConstructExpression>(result)) {
                it.code == "()"
            }
        assertEquals(template, constructExpression.templateInstantiation)
        assertEquals(array, constructExpression.instantiates)
        assertEquals("int", constructExpression.templateParameters[0].name)
        assertEquals(literal10, constructExpression.templateParameters[1])
        assertEquals("Array", constructExpression.type.name)

        val instantiatedType = constructExpression.type as ObjectType
        assertEquals(1, instantiatedType.generics.size)
        assertEquals("int", instantiatedType.generics[0].name)
    }

    @Test
    @Throws(Exception::class)
    fun testTemplateInstantiationWithNew() {
        // Test array2.cpp: Test template usage with new keyword
        val result =
            analyze(listOf(Path.of(topLevel.toString(), "array2.cpp").toFile()), topLevel, true)
        val template =
            findByUniqueName(
                flattenListIsInstance<ClassTemplateDeclaration>(result),
                "template<typename T, int N=10> class Array"
            )
        val array = findByUniqueName(flattenListIsInstance<RecordDeclaration>(result), "Array")
        val constructExpression =
            findByUniquePredicate(flattenListIsInstance(result)) { c: ConstructExpression ->
                c.code == "()"
            }
        val literal5 =
            findByUniquePredicate(flattenListIsInstance<Literal<*>>(result)) {
                it.value == 5 && it.location!!.region.endColumn == 41 && !it.isImplicit
            }
        assertNotNull(literal5)
        val literal5Declaration =
            findByUniquePredicate(flattenListIsInstance<Literal<*>>(result)) {
                it.value == 5 && it.location!!.region.endColumn == 14 && !it.isImplicit
            }
        val literal5Implicit =
            findByUniquePredicate(flattenListIsInstance<Literal<*>>(result)) {
                it.value == 5 && it.location!!.region.endColumn == 41 && it.isImplicit
            }
        val arrayVariable =
            findByUniqueName(flattenListIsInstance<VariableDeclaration>(result), "array")
        val newExpression = findByUniqueName(flattenListIsInstance<NewExpression>(result), "")
        assertEquals(array, constructExpression.instantiates)
        assertEquals(template, constructExpression.templateInstantiation)
        assertEquals(2, constructExpression.templateParameters.size)
        assertEquals("int", (constructExpression.templateParameters[0] as TypeExpression).type.name)
        assertTrue(constructExpression.templateParameters[0].isImplicit)
        assertEquals(literal5Implicit, constructExpression.templateParameters[1])
        assertEquals(2, arrayVariable.templateParameters.size)
        assertEquals("int", (arrayVariable.templateParameters[0] as TypeExpression).type.name)
        assertFalse(arrayVariable.templateParameters[0].isImplicit)
        assertEquals(literal5Declaration, arrayVariable.templateParameters[1])
        assertEquals("Array", constructExpression.type.name)

        val arrayType = constructExpression.type as ObjectType
        assertEquals(1, arrayType.generics.size)
        assertEquals("int", arrayType.generics[0].name)
        assertEquals(array, arrayType.recordDeclaration)
        assertEquals(arrayType.reference(PointerOrigin.POINTER), arrayVariable.type)
        assertEquals(arrayType.reference(PointerOrigin.POINTER), newExpression.type)
    }
}
