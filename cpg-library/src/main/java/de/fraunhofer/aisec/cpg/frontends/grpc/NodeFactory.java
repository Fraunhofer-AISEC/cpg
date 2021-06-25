/*
 * Copyright (c) 2019, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.frontends.grpc;

import de.fraunhofer.aisec.cpg.frontends.TranslationException;
import de.fraunhofer.aisec.cpg.graph.Annotation;
import de.fraunhofer.aisec.cpg.graph.AnnotationMember;
import de.fraunhofer.aisec.cpg.graph.Node;
import de.fraunhofer.aisec.cpg.graph.declarations.*;
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge;
import de.fraunhofer.aisec.cpg.graph.statements.Statement;
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression;
import de.fraunhofer.aisec.cpg.graph.types.IncompleteType;
import de.fraunhofer.aisec.cpg.graph.types.ObjectType;
import de.fraunhofer.aisec.cpg.graph.types.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class NodeFactory {
  private NodeFactory() {}

  // Converts a de.fraunhofer.aisec.cpg.frontends.grpc.messages.Node (grpcNode)
  // to a de.fraunhofer.aisec.cpg.graph.Node (cpgNode). Also resolves and
  // creates all connected nodes.
  public static Node createNode(
      long index,
      List<de.fraunhofer.aisec.cpg.frontends.grpc.messages.Node> grpcNodes,
      List<Node> cpgNodes)
      throws TranslationException {
    // Sanity check
    if (index >= grpcNodes.size() || grpcNodes.size() != cpgNodes.size()) {
      throw new TranslationException("Wrong size of node list.");
    }

    // Index 0 is the default index for "no node", so we return null
    if (index == 0) {
      return null;
    }

    // Check if node already has been initialized
    if (cpgNodes.get((int) index) != null) {
      return cpgNodes.get((int) index);
    }

    // Apparently we have to create a new node
    de.fraunhofer.aisec.cpg.frontends.grpc.messages.Node grpcNode = grpcNodes.get((int) index);
    Node cpgNode;

    if (grpcNode.hasAnnotation()) {
      cpgNode = createAnnotation(grpcNode.getAnnotation(), index, grpcNodes, cpgNodes);
    } else if (grpcNode.hasDeclaration()) {
      cpgNode = createDeclaration(grpcNode.getDeclaration(), index, grpcNodes, cpgNodes);
    } else if (grpcNode.hasStatement()) {
      cpgNode = createStatement(grpcNode.getStatement(), index, grpcNodes, cpgNodes);
    } else if (grpcNode.hasType()) {
      cpgNode = createType(grpcNode.getType(), index, grpcNodes, cpgNodes);
    } else {
      throw new TranslationException("Error in NodeFactory. node: " + grpcNode);
    }

    cpgNode.setName(grpcNode.getName());
    cpgNode.setCode(grpcNode.getCode());
    cpgNode.setComment(grpcNode.getComment());
    cpgNode.setFile(grpcNode.getFile());

    cpgNode.setImplicit(grpcNode.getImplicit());
    cpgNode.setArgumentIndex(grpcNode.getArgumentIndex());

    // id (GeneratedValue) and dummy (deprecated) are not set

    // PrevDFG
    for (var prevDFGIndex : grpcNode.getPrevDFGList()) {
      var prevDFG = createNode(prevDFGIndex.getNodeIndex(), grpcNodes, cpgNodes);
      if (prevDFG != null) {
        cpgNode.addPrevDFG(prevDFG);
      }
    }

    // NextDFG
    for (var nextDFGIndex : grpcNode.getNextDFGList()) {
      var nextDFG = createNode(nextDFGIndex.getNodeIndex(), grpcNodes, cpgNodes);
      if (nextDFG != null) {
        cpgNode.addNextDFG(nextDFG);
      }
    }

    // Typedefs
    for (var typedefIndex : grpcNode.getTypedefsList()) {
      var typedef = createNode(typedefIndex.getNodeIndex(), grpcNodes, cpgNodes);
      if (typedef == null) {
        if (!(typedef instanceof TypedefDeclaration)) {
          throw new TranslationException(
              "node " + typedefIndex.getNodeIndex() + " not of type typedef (in createNode");
        }
        cpgNode.addTypedef((TypedefDeclaration) typedef);
      }
    }

    // Annotations
    var annotations = new HashSet<Annotation>();
    for (var annotationIndex : grpcNode.getAnnotationsList()) {
      var annotation = createNode(annotationIndex.getNodeIndex(), grpcNodes, cpgNodes);
      if (annotation != null) {
        if (!(annotation instanceof Annotation)) {
          throw new TranslationException(
              "node " + annotationIndex.getNodeIndex() + " not of type annotation (in createNode");
        }
        annotations.add((Annotation) annotation);
      }
    }
    cpgNode.addAnnotations(annotations);

    for (var a : grpcNode.getPrevEOGList()) {
      var pe = NodeFactory.<Node>createPropertyEdgeNode(a, grpcNodes, cpgNodes);
      cpgNode.getPrevEOGEdges().add(pe);
    }

    for (var a : grpcNode.getNextEOGList()) {
      var pe = NodeFactory.<Node>createPropertyEdgeNode(a, grpcNodes, cpgNodes);
      cpgNode.getNextEOGEdges().add(pe);
    }

    // Deprecated
    // for (var a : grpcNode.getNextCFGList()) {
    //  var pe = NodeFactory.<Node>createPropertyEdgeNode(a, grpcNodes, cpgNodes);
    //  cpgNode.getNextCFG().add(pe);
    // }

    /*
    There are 2 different Functions: leaf and no-leaf
    A leaf will create the Node itself and will add it to the list.
    A No-leaf will skip that part

    Leaves:
    1. Create Node itself
    2. Add node to list (only in leaf functions)

    No-Leaves:
    1. Call lower level function to create node

    3. Set parameters of that level
    4. Set all connected nodes of that level with createNode()
    5. return created node
    */

    return cpgNode;
  }

  private static Annotation createAnnotation(
      de.fraunhofer.aisec.cpg.frontends.grpc.messages.Annotation a,
      long index,
      List<de.fraunhofer.aisec.cpg.frontends.grpc.messages.Node> grpcNodes,
      List<Node> cpgNodes)
      throws TranslationException {
    Annotation annotation = new Annotation();
    List<AnnotationMember> members = new ArrayList<>();
    for (var m : a.getMembersList()) {
      Node n = createNode(m.getNodeIndex(), grpcNodes, cpgNodes);
      if (n != null) {
        if (!(n instanceof AnnotationMember)) {
          throw new TranslationException("No annotationMember");
        }
        members.add((AnnotationMember) n);
      }
    }
    annotation.setMembers(members);
    return annotation;
  }

  private static de.fraunhofer.aisec.cpg.graph.declarations.Declaration createDeclaration(
      de.fraunhofer.aisec.cpg.frontends.grpc.messages.Declaration d,
      long index,
      List<de.fraunhofer.aisec.cpg.frontends.grpc.messages.Node> grpcNodes,
      List<Node> cpgNodes)
      throws TranslationException {
    de.fraunhofer.aisec.cpg.graph.declarations.Declaration declaration;

    if (d.hasDeclarationSequence()) {
      declaration =
          createDeclarationSequence(d.getDeclarationSequence(), index, grpcNodes, cpgNodes);
    } else if (d.hasProblemDeclaration()) {
      declaration = createProblemDeclaration(d.getProblemDeclaration(), index, grpcNodes, cpgNodes);
    } else if (d.hasRecordDeclaration()) {
      declaration = createRecordDeclaration(d.getRecordDeclaration(), index, grpcNodes, cpgNodes);
    } else if (d.hasTranslationUnitDeclaration()) {
      declaration =
          createTranslationUnitDeclaration(
              d.getTranslationUnitDeclaration(), index, grpcNodes, cpgNodes);
    } else if (d.hasUsingDirective()) {
      declaration = createUsingDirective(d.getUsingDirective(), index, grpcNodes, cpgNodes);
    } else if (d.hasTypedefDeclaration()) {
      declaration = createTypedefDeclaration(d.getTypedefDeclaration(), index, grpcNodes, cpgNodes);
    } else if (d.hasEnumDeclaration()) {
      declaration = createEnumDeclaration(d.getEnumDeclaration(), index, grpcNodes, cpgNodes);
    } else if (d.hasIncludeDeclaration()) {
      declaration = createIncludeDeclaration(d.getIncludeDeclaration(), index, grpcNodes, cpgNodes);
    } else if (d.hasValueDeclaration()) {
      declaration = createValueDeclaration(d.getValueDeclaration(), index, grpcNodes, cpgNodes);
    } else if (d.hasNamespaceDeclaration()) {
      declaration =
          createNamespaceDeclaration(d.getNamespaceDeclaration(), index, grpcNodes, cpgNodes);
    } else {
      throw new TranslationException("Error in NodeFactory");
    }

    return declaration;
  }

  private static de.fraunhofer.aisec.cpg.graph.declarations.DeclarationSequence
      createDeclarationSequence(
          de.fraunhofer.aisec.cpg.frontends.grpc.messages.DeclarationSequence d,
          long index,
          List<de.fraunhofer.aisec.cpg.frontends.grpc.messages.Node> grpcNodes,
          List<Node> cpgNodes)
          throws TranslationException {
    var declarationSequence = new de.fraunhofer.aisec.cpg.graph.declarations.DeclarationSequence();

    cpgNodes.set((int) index, declarationSequence);

    return declarationSequence;
  }

  private static de.fraunhofer.aisec.cpg.graph.declarations.ProblemDeclaration
      createProblemDeclaration(
          de.fraunhofer.aisec.cpg.frontends.grpc.messages.ProblemDeclaration d,
          long index,
          List<de.fraunhofer.aisec.cpg.frontends.grpc.messages.Node> grpcNodes,
          List<Node> cpgNodes)
          throws TranslationException {
    var problemDeclaration = new de.fraunhofer.aisec.cpg.graph.declarations.ProblemDeclaration();
    cpgNodes.set((int) index, problemDeclaration);

    return problemDeclaration;
  }

  private static de.fraunhofer.aisec.cpg.graph.declarations.RecordDeclaration
      createRecordDeclaration(
          de.fraunhofer.aisec.cpg.frontends.grpc.messages.RecordDeclaration d,
          long index,
          List<de.fraunhofer.aisec.cpg.frontends.grpc.messages.Node> grpcNodes,
          List<Node> cpgNodes)
          throws TranslationException {
    var recordDeclaration = new de.fraunhofer.aisec.cpg.graph.declarations.RecordDeclaration();
    cpgNodes.set((int) index, recordDeclaration);

    return recordDeclaration;
  }

  private static de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
      createTranslationUnitDeclaration(
          de.fraunhofer.aisec.cpg.frontends.grpc.messages.TranslationUnitDeclaration d,
          long index,
          List<de.fraunhofer.aisec.cpg.frontends.grpc.messages.Node> grpcNodes,
          List<Node> cpgNodes)
          throws TranslationException {
    var translationUnitDeclaration =
        new de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration();
    cpgNodes.set((int) index, translationUnitDeclaration);

    for (var a : d.getDeclarationsList()) {
      var pe = NodeFactory.<Declaration>createPropertyEdgeNode(a, grpcNodes, cpgNodes);
      translationUnitDeclaration.getDeclarationsPropertyEdge().add(pe);
    }

    for (var a : d.getIncludesList()) {
      var pe = NodeFactory.<IncludeDeclaration>createPropertyEdgeNode(a, grpcNodes, cpgNodes);
      translationUnitDeclaration.getIncludesPropertyEdge().add(pe);
    }

    for (var a : d.getNamespacesList()) {
      var pe = NodeFactory.<Declaration>createPropertyEdgeNode(a, grpcNodes, cpgNodes);
      translationUnitDeclaration.getNamespacesPropertyEdge().add(pe);
    }

    return translationUnitDeclaration;
  }

  private static de.fraunhofer.aisec.cpg.graph.declarations.UsingDirective createUsingDirective(
      de.fraunhofer.aisec.cpg.frontends.grpc.messages.UsingDirective d,
      long index,
      List<de.fraunhofer.aisec.cpg.frontends.grpc.messages.Node> grpcNodes,
      List<Node> cpgNodes)
      throws TranslationException {
    var usingDirective = new de.fraunhofer.aisec.cpg.graph.declarations.UsingDirective();
    cpgNodes.set((int) index, usingDirective);

    return usingDirective;
  }

  private static de.fraunhofer.aisec.cpg.graph.declarations.TypedefDeclaration
      createTypedefDeclaration(
          de.fraunhofer.aisec.cpg.frontends.grpc.messages.TypedefDeclaration d,
          long index,
          List<de.fraunhofer.aisec.cpg.frontends.grpc.messages.Node> grpcNodes,
          List<Node> cpgNodes)
          throws TranslationException {
    var typedefDeclaration = new de.fraunhofer.aisec.cpg.graph.declarations.TypedefDeclaration();
    cpgNodes.set((int) index, typedefDeclaration);

    return typedefDeclaration;
  }

  private static de.fraunhofer.aisec.cpg.graph.declarations.EnumDeclaration createEnumDeclaration(
      de.fraunhofer.aisec.cpg.frontends.grpc.messages.EnumDeclaration d,
      long index,
      List<de.fraunhofer.aisec.cpg.frontends.grpc.messages.Node> grpcNodes,
      List<Node> cpgNodes)
      throws TranslationException {
    var enumDeclaration = new de.fraunhofer.aisec.cpg.graph.declarations.EnumDeclaration();
    cpgNodes.set((int) index, enumDeclaration);

    return enumDeclaration;
  }

  private static de.fraunhofer.aisec.cpg.graph.declarations.IncludeDeclaration
      createIncludeDeclaration(
          de.fraunhofer.aisec.cpg.frontends.grpc.messages.IncludeDeclaration d,
          long index,
          List<de.fraunhofer.aisec.cpg.frontends.grpc.messages.Node> grpcNodes,
          List<Node> cpgNodes)
          throws TranslationException {
    var includeDeclaration = new de.fraunhofer.aisec.cpg.graph.declarations.IncludeDeclaration();
    cpgNodes.set((int) index, includeDeclaration);

    return includeDeclaration;
  }

  private static de.fraunhofer.aisec.cpg.graph.declarations.ValueDeclaration createValueDeclaration(
      de.fraunhofer.aisec.cpg.frontends.grpc.messages.ValueDeclaration d,
      long index,
      List<de.fraunhofer.aisec.cpg.frontends.grpc.messages.Node> grpcNodes,
      List<Node> cpgNodes)
      throws TranslationException {
    de.fraunhofer.aisec.cpg.graph.declarations.ValueDeclaration valueDeclaration;

    if (d.hasEnumConstantDeclaration()) {
      valueDeclaration =
          createEnumConstantDeclaration(d.getEnumConstantDeclaration(), index, grpcNodes, cpgNodes);
    } else if (d.hasVariableDeclaration()) {
      valueDeclaration =
          createVariableDeclaration(d.getVariableDeclaration(), index, grpcNodes, cpgNodes);
    } else if (d.hasParamVariableDeclaration()) {
      valueDeclaration =
          createParamVariableDeclaration(
              d.getParamVariableDeclaration(), index, grpcNodes, cpgNodes);
    } else if (d.hasFieldDeclaration()) {
      valueDeclaration =
          createFieldDeclaration(d.getFieldDeclaration(), index, grpcNodes, cpgNodes);
    } else if (d.hasFunctionDeclaration()) {
      valueDeclaration =
          createFunctionDeclaration(d.getFunctionDeclaration(), index, grpcNodes, cpgNodes);
    } else {
      throw new TranslationException("Error in createValueDeclaration");
    }

    // Type
    var type = createNode(d.getType().getNodeIndex(), grpcNodes, cpgNodes);
    if (type != null) {
      if (!(type instanceof Type)) {
        throw new TranslationException(
            "node " + d.getType().getNodeIndex() + " not of type type (in createValueDeclaration");
      }
      valueDeclaration.setType((Type) type);
    }

    // PossibleSubTypes
    var possibleSubTypes = new HashSet<Type>();
    for (var possibleSubTypeIndex : d.getPossibleSubTypesList()) {
      var possibleSubType = createNode(possibleSubTypeIndex.getNodeIndex(), grpcNodes, cpgNodes);
      if (possibleSubType != null) {
        if (!(possibleSubType instanceof Type)) {
          throw new TranslationException(
              "node "
                  + d.getType().getNodeIndex()
                  + " not of type type (in createValueDeclaration");
        }
        possibleSubTypes.add((Type) possibleSubType);
      }
    }
    valueDeclaration.setPossibleSubTypes(possibleSubTypes);

    // TODO typeListener

    return valueDeclaration;
  }

  private static de.fraunhofer.aisec.cpg.graph.declarations.NamespaceDeclaration
      createNamespaceDeclaration(
          de.fraunhofer.aisec.cpg.frontends.grpc.messages.NamespaceDeclaration d,
          long index,
          List<de.fraunhofer.aisec.cpg.frontends.grpc.messages.Node> grpcNodes,
          List<Node> cpgNodes)
          throws TranslationException {
    var namespaceDeclaration =
        new de.fraunhofer.aisec.cpg.graph.declarations.NamespaceDeclaration();
    cpgNodes.set((int) index, namespaceDeclaration);

    // Declarations
    for (var declarationIndex : d.getDeclarationsList()) {
      var declaration = createNode(declarationIndex.getNodeIndex(), grpcNodes, cpgNodes);
      if (declaration != null) {
        if (!(declaration instanceof Declaration)) {
          throw new TranslationException(
              "node "
                  + declarationIndex.getNodeIndex()
                  + " not of type type (in createValueDeclaration");
        }
        namespaceDeclaration.addDeclaration((Declaration) declaration);
      }
    }

    return namespaceDeclaration;
  }

  private static de.fraunhofer.aisec.cpg.graph.declarations.EnumConstantDeclaration
      createEnumConstantDeclaration(
          de.fraunhofer.aisec.cpg.frontends.grpc.messages.EnumConstantDeclaration v,
          long index,
          List<de.fraunhofer.aisec.cpg.frontends.grpc.messages.Node> grpcNodes,
          List<Node> cpgNodes)
          throws TranslationException {
    var enumConstantDeclaration =
        new de.fraunhofer.aisec.cpg.graph.declarations.EnumConstantDeclaration();

    cpgNodes.set((int) index, enumConstantDeclaration);

    return enumConstantDeclaration;
  }

  private static de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration
      createVariableDeclaration(
          de.fraunhofer.aisec.cpg.frontends.grpc.messages.VariableDeclaration v,
          long index,
          List<de.fraunhofer.aisec.cpg.frontends.grpc.messages.Node> grpcNodes,
          List<Node> cpgNodes)
          throws TranslationException {
    var variableDeclaration = new de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration();

    cpgNodes.set((int) index, variableDeclaration);

    // initializer
    var initializer = createNode(v.getInitializer().getNodeIndex(), grpcNodes, cpgNodes);
    if (initializer != null) {
      if (!(initializer instanceof Expression)) {
        throw new TranslationException(
            "node "
                + v.getInitializer().getNodeIndex()
                + " not of type Expression (in createVariableDeclaration");
      }
      variableDeclaration.setInitializer((Expression) initializer);
    }

    variableDeclaration.setIsArray(v.getIsArray());
    variableDeclaration.setImplicitInitializerAllowed(v.getImplicitInitializerAllowed());

    return variableDeclaration;
  }

  private static de.fraunhofer.aisec.cpg.graph.declarations.ParamVariableDeclaration
      createParamVariableDeclaration(
          de.fraunhofer.aisec.cpg.frontends.grpc.messages.ParamVariableDeclaration v,
          long index,
          List<de.fraunhofer.aisec.cpg.frontends.grpc.messages.Node> grpcNodes,
          List<Node> cpgNodes)
          throws TranslationException {
    var paramVariableDeclaration =
        new de.fraunhofer.aisec.cpg.graph.declarations.ParamVariableDeclaration();

    cpgNodes.set((int) index, paramVariableDeclaration);

    return paramVariableDeclaration;
  }

  private static de.fraunhofer.aisec.cpg.graph.declarations.FieldDeclaration createFieldDeclaration(
      de.fraunhofer.aisec.cpg.frontends.grpc.messages.FieldDeclaration v,
      long index,
      List<de.fraunhofer.aisec.cpg.frontends.grpc.messages.Node> grpcNodes,
      List<Node> cpgNodes)
      throws TranslationException {
    var fieldDeclaration = new de.fraunhofer.aisec.cpg.graph.declarations.FieldDeclaration();

    cpgNodes.set((int) index, fieldDeclaration);

    return fieldDeclaration;
  }

  private static de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
      createFunctionDeclaration(
          de.fraunhofer.aisec.cpg.frontends.grpc.messages.FunctionDeclaration v,
          long index,
          List<de.fraunhofer.aisec.cpg.frontends.grpc.messages.Node> grpcNodes,
          List<Node> cpgNodes)
          throws TranslationException {
    var functionDeclaration = new de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration();
    cpgNodes.set((int) index, functionDeclaration);

    // PropertyEdge
    // TODO

    for (var a : v.getRecordsList()) {
      var pe = NodeFactory.<RecordDeclaration>createPropertyEdgeNode(a, grpcNodes, cpgNodes);
      functionDeclaration.getRecordsPropertyEdge().add(pe);
    }
    for (var a : v.getParametersList()) {
      var pe = NodeFactory.<ParamVariableDeclaration>createPropertyEdgeNode(a, grpcNodes, cpgNodes);
      functionDeclaration.getParametersPropertyEdge().add(pe);
    }
    for (var a : v.getThrowsTypesList()) {
      var pe = NodeFactory.<Type>createPropertyEdgeNode(a, grpcNodes, cpgNodes);
      functionDeclaration.getThrowsTypesPropertyEdge().add(pe);
    }
    for (var a : v.getOverriddenByList()) {
      var pe = NodeFactory.<FunctionDeclaration>createPropertyEdgeNode(a, grpcNodes, cpgNodes);
      functionDeclaration.getOverriddenByPropertyEdge().add(pe);
    }
    for (var a : v.getOverridesList()) {
      var pe = NodeFactory.<FunctionDeclaration>createPropertyEdgeNode(a, grpcNodes, cpgNodes);
      functionDeclaration.getOverridesPropertyEdge().add(pe);
    }

    // isDefinition
    functionDeclaration.setIsDefinition(v.getIsDefinition());

    // Definition
    var definition = createNode(v.getDefinition().getNodeIndex(), grpcNodes, cpgNodes);
    if (definition != null) {
      if (!(definition instanceof FunctionDeclaration)) {
        throw new TranslationException(
            "node "
                + v.getDefinition().getNodeIndex()
                + " not of type funcDecl (in createfunDecl1");
      }
      functionDeclaration.setDefinition((FunctionDeclaration) definition);
    }

    // Body
    var body = createNode(v.getBody().getNodeIndex(), grpcNodes, cpgNodes);
    if (body != null) {
      if (!(body instanceof Statement)) {
        throw new TranslationException(
            "node " + v.getBody().getNodeIndex() + " not of type statement (in createfunDecl2");
      }
      functionDeclaration.setBody((Statement) body);
    }

    return functionDeclaration;
  }

  private static de.fraunhofer.aisec.cpg.graph.statements.Statement createStatement(
      de.fraunhofer.aisec.cpg.frontends.grpc.messages.Statement s,
      long index,
      List<de.fraunhofer.aisec.cpg.frontends.grpc.messages.Node> grpcNodes,
      List<Node> cpgNodes)
      throws TranslationException {
    de.fraunhofer.aisec.cpg.graph.statements.Statement statement;

    if (s.hasIfStatement()) {
      statement = createIfStatement(s.getIfStatement(), index, grpcNodes, cpgNodes);
    } else if (s.hasCompoundStatement()) {
      statement = createCompoundStatement(s.getCompoundStatement(), index, grpcNodes, cpgNodes);
    } else if (s.hasForStatement()) {
      statement = createForStatement(s.getForStatement(), index, grpcNodes, cpgNodes);
    } else if (s.hasLabelStatement()) {
      statement = createLabelStatement(s.getLabelStatement(), index, grpcNodes, cpgNodes);
    } else if (s.hasCatchClause()) {
      statement = createCatchClause(s.getCatchClause(), index, grpcNodes, cpgNodes);
    } else if (s.hasDefaultStatement()) {
      statement = createDefaultStatement(s.getDefaultStatement(), index, grpcNodes, cpgNodes);
    } else if (s.hasExpression()) {
      statement = createExpression(s.getExpression(), index, grpcNodes, cpgNodes);
    } else if (s.hasBreakStatement()) {
      statement = createBreakStatement(s.getBreakStatement(), index, grpcNodes, cpgNodes);
    } else if (s.hasSynchronizedStatement()) {
      statement =
          createSynchronizedStatement(s.getSynchronizedStatement(), index, grpcNodes, cpgNodes);
    } else if (s.hasDeclarationStatement()) {
      statement =
          createDeclarationStatement(s.getDeclarationStatement(), index, grpcNodes, cpgNodes);
    } else if (s.hasForEachStatement()) {
      statement = createForEachStatement(s.getForEachStatement(), index, grpcNodes, cpgNodes);
    } else if (s.hasDoStatement()) {
      statement = createDoStatement(s.getDoStatement(), index, grpcNodes, cpgNodes);
    } else if (s.hasReturnStatement()) {
      statement = createReturnStatement(s.getReturnStatement(), index, grpcNodes, cpgNodes);
    } else if (s.hasCaseStatement()) {
      statement = createCaseStatement(s.getCaseStatement(), index, grpcNodes, cpgNodes);
    } else if (s.hasEmptyStatement()) {
      statement = createEmptyStatement(s.getEmptyStatement(), index, grpcNodes, cpgNodes);
    } else if (s.hasTryStatement()) {
      statement = createTryStatement(s.getTryStatement(), index, grpcNodes, cpgNodes);
    } else if (s.hasAssertStatement()) {
      statement = createAssertStatement(s.getAssertStatement(), index, grpcNodes, cpgNodes);
    } else if (s.hasContinueStatement()) {
      statement = createContinueStatement(s.getContinueStatement(), index, grpcNodes, cpgNodes);
    } else if (s.hasGotoStatement()) {
      statement = createGotoStatement(s.getGotoStatement(), index, grpcNodes, cpgNodes);
    } else if (s.hasWhileStatement()) {
      statement = createWhileStatement(s.getWhileStatement(), index, grpcNodes, cpgNodes);
    } else if (s.hasSwitchStatement()) {
      statement = createSwitchStatement(s.getSwitchStatement(), index, grpcNodes, cpgNodes);
    } else {
      throw new TranslationException("Error in createStatement");
    }

    for (var a : s.getLocalsList()) {
      var pe = NodeFactory.<VariableDeclaration>createPropertyEdgeNode(a, grpcNodes, cpgNodes);
      statement.getLocalsPropertyEdge().add(pe);
    }

    return statement;
  }

  private static de.fraunhofer.aisec.cpg.graph.statements.IfStatement createIfStatement(
      de.fraunhofer.aisec.cpg.frontends.grpc.messages.IfStatement s,
      long index,
      List<de.fraunhofer.aisec.cpg.frontends.grpc.messages.Node> grpcNodes,
      List<Node> cpgNodes)
      throws TranslationException {
    var ifStatement = new de.fraunhofer.aisec.cpg.graph.statements.IfStatement();

    cpgNodes.set((int) index, ifStatement);

    return ifStatement;
  }

  private static de.fraunhofer.aisec.cpg.graph.statements.CompoundStatement createCompoundStatement(
      de.fraunhofer.aisec.cpg.frontends.grpc.messages.CompoundStatement s,
      long index,
      List<de.fraunhofer.aisec.cpg.frontends.grpc.messages.Node> grpcNodes,
      List<Node> cpgNodes)
      throws TranslationException {
    var compoundStatement = new de.fraunhofer.aisec.cpg.graph.statements.CompoundStatement();

    cpgNodes.set((int) index, compoundStatement);

    for (var a : s.getStatementsList()) {
      var pe = NodeFactory.<Statement>createPropertyEdgeNode(a, grpcNodes, cpgNodes);
      compoundStatement.getStatementEdges().add(pe);
    }

    return compoundStatement;
  }

  private static de.fraunhofer.aisec.cpg.graph.statements.ForStatement createForStatement(
      de.fraunhofer.aisec.cpg.frontends.grpc.messages.ForStatement s,
      long index,
      List<de.fraunhofer.aisec.cpg.frontends.grpc.messages.Node> grpcNodes,
      List<Node> cpgNodes)
      throws TranslationException {
    var forStatement = new de.fraunhofer.aisec.cpg.graph.statements.ForStatement();

    cpgNodes.set((int) index, forStatement);

    return forStatement;
  }

  private static de.fraunhofer.aisec.cpg.graph.statements.LabelStatement createLabelStatement(
      de.fraunhofer.aisec.cpg.frontends.grpc.messages.LabelStatement s,
      long index,
      List<de.fraunhofer.aisec.cpg.frontends.grpc.messages.Node> grpcNodes,
      List<Node> cpgNodes)
      throws TranslationException {
    var labelStatement = new de.fraunhofer.aisec.cpg.graph.statements.LabelStatement();

    cpgNodes.set((int) index, labelStatement);

    return labelStatement;
  }

  private static de.fraunhofer.aisec.cpg.graph.statements.CatchClause createCatchClause(
      de.fraunhofer.aisec.cpg.frontends.grpc.messages.CatchClause s,
      long index,
      List<de.fraunhofer.aisec.cpg.frontends.grpc.messages.Node> grpcNodes,
      List<Node> cpgNodes)
      throws TranslationException {
    var catchClause = new de.fraunhofer.aisec.cpg.graph.statements.CatchClause();

    cpgNodes.set((int) index, catchClause);

    return catchClause;
  }

  private static de.fraunhofer.aisec.cpg.graph.statements.DefaultStatement createDefaultStatement(
      de.fraunhofer.aisec.cpg.frontends.grpc.messages.DefaultStatement s,
      long index,
      List<de.fraunhofer.aisec.cpg.frontends.grpc.messages.Node> grpcNodes,
      List<Node> cpgNodes)
      throws TranslationException {
    var defaultStatement = new de.fraunhofer.aisec.cpg.graph.statements.DefaultStatement();

    cpgNodes.set((int) index, defaultStatement);

    return defaultStatement;
  }

  private static de.fraunhofer.aisec.cpg.graph.statements.BreakStatement createBreakStatement(
      de.fraunhofer.aisec.cpg.frontends.grpc.messages.BreakStatement s,
      long index,
      List<de.fraunhofer.aisec.cpg.frontends.grpc.messages.Node> grpcNodes,
      List<Node> cpgNodes)
      throws TranslationException {
    var breakStatement = new de.fraunhofer.aisec.cpg.graph.statements.BreakStatement();

    cpgNodes.set((int) index, breakStatement);

    return breakStatement;
  }

  private static de.fraunhofer.aisec.cpg.graph.statements.SynchronizedStatement
      createSynchronizedStatement(
          de.fraunhofer.aisec.cpg.frontends.grpc.messages.SynchronizedStatement s,
          long index,
          List<de.fraunhofer.aisec.cpg.frontends.grpc.messages.Node> grpcNodes,
          List<Node> cpgNodes)
          throws TranslationException {
    var synchronizedStatement =
        new de.fraunhofer.aisec.cpg.graph.statements.SynchronizedStatement();

    cpgNodes.set((int) index, synchronizedStatement);

    return synchronizedStatement;
  }

  private static de.fraunhofer.aisec.cpg.graph.statements.DeclarationStatement
      createDeclarationStatement(
          de.fraunhofer.aisec.cpg.frontends.grpc.messages.DeclarationStatement s,
          long index,
          List<de.fraunhofer.aisec.cpg.frontends.grpc.messages.Node> grpcNodes,
          List<Node> cpgNodes)
          throws TranslationException {
    var declarationStatement = new de.fraunhofer.aisec.cpg.graph.statements.DeclarationStatement();

    cpgNodes.set((int) index, declarationStatement);

    // TODO add declarations

    for (var a : s.getDeclarationsList()) {
      var pe = NodeFactory.<Declaration>createPropertyEdgeNode(a, grpcNodes, cpgNodes);
      declarationStatement.getDeclarationsPropertyEdge().add(pe);
    }

    return declarationStatement;
  }

  private static de.fraunhofer.aisec.cpg.graph.statements.ForEachStatement createForEachStatement(
      de.fraunhofer.aisec.cpg.frontends.grpc.messages.ForEachStatement s,
      long index,
      List<de.fraunhofer.aisec.cpg.frontends.grpc.messages.Node> grpcNodes,
      List<Node> cpgNodes)
      throws TranslationException {
    var forEachStatement = new de.fraunhofer.aisec.cpg.graph.statements.ForEachStatement();

    cpgNodes.set((int) index, forEachStatement);

    return forEachStatement;
  }

  private static de.fraunhofer.aisec.cpg.graph.statements.DoStatement createDoStatement(
      de.fraunhofer.aisec.cpg.frontends.grpc.messages.DoStatement s,
      long index,
      List<de.fraunhofer.aisec.cpg.frontends.grpc.messages.Node> grpcNodes,
      List<Node> cpgNodes)
      throws TranslationException {
    var doStatement = new de.fraunhofer.aisec.cpg.graph.statements.DoStatement();

    cpgNodes.set((int) index, doStatement);

    return doStatement;
  }

  private static de.fraunhofer.aisec.cpg.graph.statements.ReturnStatement createReturnStatement(
      de.fraunhofer.aisec.cpg.frontends.grpc.messages.ReturnStatement s,
      long index,
      List<de.fraunhofer.aisec.cpg.frontends.grpc.messages.Node> grpcNodes,
      List<Node> cpgNodes)
      throws TranslationException {
    var returnStatement = new de.fraunhofer.aisec.cpg.graph.statements.ReturnStatement();

    cpgNodes.set((int) index, returnStatement);

    // Definition
    var returnValue = createNode(s.getReturnValue().getNodeIndex(), grpcNodes, cpgNodes);
    if (returnValue != null) {
      if (!(returnValue instanceof Expression)) {
        throw new TranslationException(
            "node "
                + s.getReturnValue().getNodeIndex()
                + " not of type funcDecl (in createReturnStmt");
      }
      returnStatement.setReturnValue((Expression) returnValue);
    }

    return returnStatement;
  }

  private static de.fraunhofer.aisec.cpg.graph.statements.CaseStatement createCaseStatement(
      de.fraunhofer.aisec.cpg.frontends.grpc.messages.CaseStatement s,
      long index,
      List<de.fraunhofer.aisec.cpg.frontends.grpc.messages.Node> grpcNodes,
      List<Node> cpgNodes)
      throws TranslationException {
    var caseStatement = new de.fraunhofer.aisec.cpg.graph.statements.CaseStatement();

    cpgNodes.set((int) index, caseStatement);

    return caseStatement;
  }

  private static de.fraunhofer.aisec.cpg.graph.statements.EmptyStatement createEmptyStatement(
      de.fraunhofer.aisec.cpg.frontends.grpc.messages.EmptyStatement s,
      long index,
      List<de.fraunhofer.aisec.cpg.frontends.grpc.messages.Node> grpcNodes,
      List<Node> cpgNodes)
      throws TranslationException {
    var emptyStatement = new de.fraunhofer.aisec.cpg.graph.statements.EmptyStatement();

    cpgNodes.set((int) index, emptyStatement);

    return emptyStatement;
  }

  private static de.fraunhofer.aisec.cpg.graph.statements.TryStatement createTryStatement(
      de.fraunhofer.aisec.cpg.frontends.grpc.messages.TryStatement s,
      long index,
      List<de.fraunhofer.aisec.cpg.frontends.grpc.messages.Node> grpcNodes,
      List<Node> cpgNodes)
      throws TranslationException {
    var tryStatement = new de.fraunhofer.aisec.cpg.graph.statements.TryStatement();

    cpgNodes.set((int) index, tryStatement);

    return tryStatement;
  }

  private static de.fraunhofer.aisec.cpg.graph.statements.AssertStatement createAssertStatement(
      de.fraunhofer.aisec.cpg.frontends.grpc.messages.AssertStatement s,
      long index,
      List<de.fraunhofer.aisec.cpg.frontends.grpc.messages.Node> grpcNodes,
      List<Node> cpgNodes)
      throws TranslationException {
    var assertStatement = new de.fraunhofer.aisec.cpg.graph.statements.AssertStatement();

    cpgNodes.set((int) index, assertStatement);

    return assertStatement;
  }

  private static de.fraunhofer.aisec.cpg.graph.statements.ContinueStatement createContinueStatement(
      de.fraunhofer.aisec.cpg.frontends.grpc.messages.ContinueStatement s,
      long index,
      List<de.fraunhofer.aisec.cpg.frontends.grpc.messages.Node> grpcNodes,
      List<Node> cpgNodes)
      throws TranslationException {
    var continueStatement = new de.fraunhofer.aisec.cpg.graph.statements.ContinueStatement();

    cpgNodes.set((int) index, continueStatement);

    return continueStatement;
  }

  private static de.fraunhofer.aisec.cpg.graph.statements.GotoStatement createGotoStatement(
      de.fraunhofer.aisec.cpg.frontends.grpc.messages.GotoStatement s,
      long index,
      List<de.fraunhofer.aisec.cpg.frontends.grpc.messages.Node> grpcNodes,
      List<Node> cpgNodes)
      throws TranslationException {
    var gotoStatement = new de.fraunhofer.aisec.cpg.graph.statements.GotoStatement();

    cpgNodes.set((int) index, gotoStatement);

    return gotoStatement;
  }

  private static de.fraunhofer.aisec.cpg.graph.statements.WhileStatement createWhileStatement(
      de.fraunhofer.aisec.cpg.frontends.grpc.messages.WhileStatement s,
      long index,
      List<de.fraunhofer.aisec.cpg.frontends.grpc.messages.Node> grpcNodes,
      List<Node> cpgNodes)
      throws TranslationException {
    var whileStatement = new de.fraunhofer.aisec.cpg.graph.statements.WhileStatement();

    cpgNodes.set((int) index, whileStatement);

    return whileStatement;
  }

  private static de.fraunhofer.aisec.cpg.graph.statements.SwitchStatement createSwitchStatement(
      de.fraunhofer.aisec.cpg.frontends.grpc.messages.SwitchStatement s,
      long index,
      List<de.fraunhofer.aisec.cpg.frontends.grpc.messages.Node> grpcNodes,
      List<Node> cpgNodes)
      throws TranslationException {
    var switchStatement = new de.fraunhofer.aisec.cpg.graph.statements.SwitchStatement();

    cpgNodes.set((int) index, switchStatement);

    return switchStatement;
  }

  private static de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression createExpression(
      de.fraunhofer.aisec.cpg.frontends.grpc.messages.Expression e,
      long index,
      List<de.fraunhofer.aisec.cpg.frontends.grpc.messages.Node> grpcNodes,
      List<Node> cpgNodes)
      throws TranslationException {
    de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression expression;

    if (e.hasExpressionList()) {
      expression = createExpressionList(e.getExpressionList(), index, grpcNodes, cpgNodes);
    } else if (e.hasNewExpression()) {
      expression = createNewExpression(e.getNewExpression(), index, grpcNodes, cpgNodes);
    } else if (e.hasConditionalExpression()) {
      expression =
          createConditionalExpression(e.getConditionalExpression(), index, grpcNodes, cpgNodes);
    } else if (e.hasCompoundStatementExpression()) {
      expression =
          createCompoundStatementExpression(
              e.getCompoundStatementExpression(), index, grpcNodes, cpgNodes);
    } else if (e.hasLiteral()) {
      expression = createLiteral(e.getLiteral(), index, grpcNodes, cpgNodes);
    } else if (e.hasConstructExpression()) {
      expression =
          createConstructExpression(e.getConstructExpression(), index, grpcNodes, cpgNodes);
    } else if (e.hasDesignatedInitializerExpression()) {
      expression =
          createDesignatedInitializerExpression(
              e.getDesignatedInitializerExpression(), index, grpcNodes, cpgNodes);
    } else if (e.hasUnaryOperator()) {
      expression = createUnaryOperator(e.getUnaryOperator(), index, grpcNodes, cpgNodes);
    } else if (e.hasCallExpression()) {
      expression = createCallExpression(e.getCallExpression(), index, grpcNodes, cpgNodes);
    } else if (e.hasDeclaredReferenceExpression()) {
      expression =
          createDeclaredReferenceExpression(
              e.getDeclaredReferenceExpression(), index, grpcNodes, cpgNodes);
    } else if (e.hasCastExpression()) {
      expression = createCastExpression(e.getCastExpression(), index, grpcNodes, cpgNodes);
    } else if (e.hasUninitializedValue()) {
      expression = createUninitializedValue(e.getUninitializedValue(), index, grpcNodes, cpgNodes);
    } else if (e.hasTypeIdExpression()) {
      expression = createTypeIdExpression(e.getTypeIdExpression(), index, grpcNodes, cpgNodes);
    } else if (e.hasArrayCreationExpression()) {
      expression =
          createArrayCreationExpression(e.getArrayCreationExpression(), index, grpcNodes, cpgNodes);
    } else if (e.hasInitializerListExpression()) {
      expression =
          createInitializerListExpression(
              e.getInitializerListExpression(), index, grpcNodes, cpgNodes);
    } else if (e.hasDeleteExpression()) {
      expression = createDeleteExpression(e.getDeleteExpression(), index, grpcNodes, cpgNodes);
    } else if (e.hasArrayRangeExpression()) {
      expression =
          createArrayRangeExpression(e.getArrayRangeExpression(), index, grpcNodes, cpgNodes);
    } else if (e.hasBinaryOperator()) {
      expression = createBinaryOperator(e.getBinaryOperator(), index, grpcNodes, cpgNodes);
    } else if (e.hasArraySubscriptionExpression()) {
      expression =
          createArraySubscriptionExpression(
              e.getArraySubscriptionExpression(), index, grpcNodes, cpgNodes);
    } else {
      throw new TranslationException("Error in createExpression");
    }

    // Type
    var type = createNode(e.getType().getNodeIndex(), grpcNodes, cpgNodes);
    if (type != null) {
      if (!(type instanceof Type)) {
        throw new TranslationException(
            "node " + e.getType().getNodeIndex() + " not of type type (in createExpression");
      }
      expression.setType((Type) type);
    }

    // PossibleSubTypes
    var possibleSubTypes = new HashSet<Type>();
    for (var possibleSubTypeIndex : e.getPossibleSubTypesList()) {
      var possibleSubType = createNode(possibleSubTypeIndex.getNodeIndex(), grpcNodes, cpgNodes);
      if (possibleSubType != null) {
        if (!(possibleSubType instanceof Type)) {
          throw new TranslationException(
              "node "
                  + possibleSubTypeIndex.getNodeIndex()
                  + " not of type type (in createExpression");
        }
        possibleSubTypes.add((Type) possibleSubType);
      }
    }
    expression.setPossibleSubTypes(possibleSubTypes);

    // TODO typeListener

    return expression;
  }

  private static de.fraunhofer.aisec.cpg.graph.statements.expressions.ExpressionList
      createExpressionList(
          de.fraunhofer.aisec.cpg.frontends.grpc.messages.ExpressionList e,
          long index,
          List<de.fraunhofer.aisec.cpg.frontends.grpc.messages.Node> grpcNodes,
          List<Node> cpgNodes)
          throws TranslationException {
    var ExpressionList = new de.fraunhofer.aisec.cpg.graph.statements.expressions.ExpressionList();

    cpgNodes.set((int) index, ExpressionList);

    return ExpressionList;
  }

  private static de.fraunhofer.aisec.cpg.graph.statements.expressions.NewExpression
      createNewExpression(
          de.fraunhofer.aisec.cpg.frontends.grpc.messages.NewExpression e,
          long index,
          List<de.fraunhofer.aisec.cpg.frontends.grpc.messages.Node> grpcNodes,
          List<Node> cpgNodes)
          throws TranslationException {
    var NewExpression = new de.fraunhofer.aisec.cpg.graph.statements.expressions.NewExpression();

    cpgNodes.set((int) index, NewExpression);

    return NewExpression;
  }

  private static de.fraunhofer.aisec.cpg.graph.statements.expressions.ConditionalExpression
      createConditionalExpression(
          de.fraunhofer.aisec.cpg.frontends.grpc.messages.ConditionalExpression e,
          long index,
          List<de.fraunhofer.aisec.cpg.frontends.grpc.messages.Node> grpcNodes,
          List<Node> cpgNodes)
          throws TranslationException {
    var ConditionalExpression =
        new de.fraunhofer.aisec.cpg.graph.statements.expressions.ConditionalExpression();

    cpgNodes.set((int) index, ConditionalExpression);

    return ConditionalExpression;
  }

  private static de.fraunhofer.aisec.cpg.graph.statements.expressions.CompoundStatementExpression
      createCompoundStatementExpression(
          de.fraunhofer.aisec.cpg.frontends.grpc.messages.CompoundStatementExpression e,
          long index,
          List<de.fraunhofer.aisec.cpg.frontends.grpc.messages.Node> grpcNodes,
          List<Node> cpgNodes)
          throws TranslationException {
    var CompoundStatementExpression =
        new de.fraunhofer.aisec.cpg.graph.statements.expressions.CompoundStatementExpression();

    cpgNodes.set((int) index, CompoundStatementExpression);

    return CompoundStatementExpression;
  }

  private static de.fraunhofer.aisec.cpg.graph.statements.expressions.Literal createLiteral(
      de.fraunhofer.aisec.cpg.frontends.grpc.messages.Literal e,
      long index,
      List<de.fraunhofer.aisec.cpg.frontends.grpc.messages.Node> grpcNodes,
      List<Node> cpgNodes)
      throws TranslationException {
    var literal = new de.fraunhofer.aisec.cpg.graph.statements.expressions.Literal();

    cpgNodes.set((int) index, literal);

    // TODO generics currently not supported by protobuf

    return literal;
  }

  private static de.fraunhofer.aisec.cpg.graph.statements.expressions.ConstructExpression
      createConstructExpression(
          de.fraunhofer.aisec.cpg.frontends.grpc.messages.ConstructExpression e,
          long index,
          List<de.fraunhofer.aisec.cpg.frontends.grpc.messages.Node> grpcNodes,
          List<Node> cpgNodes)
          throws TranslationException {
    var ConstructExpression =
        new de.fraunhofer.aisec.cpg.graph.statements.expressions.ConstructExpression();

    cpgNodes.set((int) index, ConstructExpression);

    return ConstructExpression;
  }

  private static de.fraunhofer.aisec.cpg.graph.statements.expressions
          .DesignatedInitializerExpression
      createDesignatedInitializerExpression(
          de.fraunhofer.aisec.cpg.frontends.grpc.messages.DesignatedInitializerExpression e,
          long index,
          List<de.fraunhofer.aisec.cpg.frontends.grpc.messages.Node> grpcNodes,
          List<Node> cpgNodes)
          throws TranslationException {
    var DesignatedInitializerExpression =
        new de.fraunhofer.aisec.cpg.graph.statements.expressions.DesignatedInitializerExpression();

    cpgNodes.set((int) index, DesignatedInitializerExpression);

    return DesignatedInitializerExpression;
  }

  private static de.fraunhofer.aisec.cpg.graph.statements.expressions.UnaryOperator
      createUnaryOperator(
          de.fraunhofer.aisec.cpg.frontends.grpc.messages.UnaryOperator e,
          long index,
          List<de.fraunhofer.aisec.cpg.frontends.grpc.messages.Node> grpcNodes,
          List<Node> cpgNodes)
          throws TranslationException {
    var UnaryOperator = new de.fraunhofer.aisec.cpg.graph.statements.expressions.UnaryOperator();

    cpgNodes.set((int) index, UnaryOperator);

    return UnaryOperator;
  }

  private static de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
      createCallExpression(
          de.fraunhofer.aisec.cpg.frontends.grpc.messages.CallExpression e,
          long index,
          List<de.fraunhofer.aisec.cpg.frontends.grpc.messages.Node> grpcNodes,
          List<Node> cpgNodes)
          throws TranslationException {
    var CallExpression = new de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression();

    cpgNodes.set((int) index, CallExpression);

    return CallExpression;
  }

  private static de.fraunhofer.aisec.cpg.graph.statements.expressions.DeclaredReferenceExpression
      createDeclaredReferenceExpression(
          de.fraunhofer.aisec.cpg.frontends.grpc.messages.DeclaredReferenceExpression e,
          long index,
          List<de.fraunhofer.aisec.cpg.frontends.grpc.messages.Node> grpcNodes,
          List<Node> cpgNodes)
          throws TranslationException {
    var DeclaredReferenceExpression =
        new de.fraunhofer.aisec.cpg.graph.statements.expressions.DeclaredReferenceExpression();

    cpgNodes.set((int) index, DeclaredReferenceExpression);

    return DeclaredReferenceExpression;
  }

  private static de.fraunhofer.aisec.cpg.graph.statements.expressions.CastExpression
      createCastExpression(
          de.fraunhofer.aisec.cpg.frontends.grpc.messages.CastExpression e,
          long index,
          List<de.fraunhofer.aisec.cpg.frontends.grpc.messages.Node> grpcNodes,
          List<Node> cpgNodes)
          throws TranslationException {
    var CastExpression = new de.fraunhofer.aisec.cpg.graph.statements.expressions.CastExpression();

    cpgNodes.set((int) index, CastExpression);

    return CastExpression;
  }

  private static de.fraunhofer.aisec.cpg.graph.statements.expressions.UninitializedValue
      createUninitializedValue(
          de.fraunhofer.aisec.cpg.frontends.grpc.messages.UninitializedValue e,
          long index,
          List<de.fraunhofer.aisec.cpg.frontends.grpc.messages.Node> grpcNodes,
          List<Node> cpgNodes)
          throws TranslationException {
    var UninitializedValue =
        new de.fraunhofer.aisec.cpg.graph.statements.expressions.UninitializedValue();

    cpgNodes.set((int) index, UninitializedValue);

    return UninitializedValue;
  }

  private static de.fraunhofer.aisec.cpg.graph.statements.expressions.TypeIdExpression
      createTypeIdExpression(
          de.fraunhofer.aisec.cpg.frontends.grpc.messages.TypeIdExpression e,
          long index,
          List<de.fraunhofer.aisec.cpg.frontends.grpc.messages.Node> grpcNodes,
          List<Node> cpgNodes)
          throws TranslationException {
    var TypeIdExpression =
        new de.fraunhofer.aisec.cpg.graph.statements.expressions.TypeIdExpression();

    cpgNodes.set((int) index, TypeIdExpression);

    return TypeIdExpression;
  }

  private static de.fraunhofer.aisec.cpg.graph.statements.expressions.ArrayCreationExpression
      createArrayCreationExpression(
          de.fraunhofer.aisec.cpg.frontends.grpc.messages.ArrayCreationExpression e,
          long index,
          List<de.fraunhofer.aisec.cpg.frontends.grpc.messages.Node> grpcNodes,
          List<Node> cpgNodes)
          throws TranslationException {
    var ArrayCreationExpression =
        new de.fraunhofer.aisec.cpg.graph.statements.expressions.ArrayCreationExpression();

    cpgNodes.set((int) index, ArrayCreationExpression);

    return ArrayCreationExpression;
  }

  private static de.fraunhofer.aisec.cpg.graph.statements.expressions.InitializerListExpression
      createInitializerListExpression(
          de.fraunhofer.aisec.cpg.frontends.grpc.messages.InitializerListExpression e,
          long index,
          List<de.fraunhofer.aisec.cpg.frontends.grpc.messages.Node> grpcNodes,
          List<Node> cpgNodes)
          throws TranslationException {
    var InitializerListExpression =
        new de.fraunhofer.aisec.cpg.graph.statements.expressions.InitializerListExpression();

    cpgNodes.set((int) index, InitializerListExpression);

    return InitializerListExpression;
  }

  private static de.fraunhofer.aisec.cpg.graph.statements.expressions.DeleteExpression
      createDeleteExpression(
          de.fraunhofer.aisec.cpg.frontends.grpc.messages.DeleteExpression e,
          long index,
          List<de.fraunhofer.aisec.cpg.frontends.grpc.messages.Node> grpcNodes,
          List<Node> cpgNodes)
          throws TranslationException {
    var DeleteExpression =
        new de.fraunhofer.aisec.cpg.graph.statements.expressions.DeleteExpression();

    cpgNodes.set((int) index, DeleteExpression);

    return DeleteExpression;
  }

  private static de.fraunhofer.aisec.cpg.graph.statements.expressions.ArrayRangeExpression
      createArrayRangeExpression(
          de.fraunhofer.aisec.cpg.frontends.grpc.messages.ArrayRangeExpression e,
          long index,
          List<de.fraunhofer.aisec.cpg.frontends.grpc.messages.Node> grpcNodes,
          List<Node> cpgNodes)
          throws TranslationException {
    var ArrayRangeExpression =
        new de.fraunhofer.aisec.cpg.graph.statements.expressions.ArrayRangeExpression();

    cpgNodes.set((int) index, ArrayRangeExpression);

    return ArrayRangeExpression;
  }

  private static de.fraunhofer.aisec.cpg.graph.statements.expressions.BinaryOperator
      createBinaryOperator(
          de.fraunhofer.aisec.cpg.frontends.grpc.messages.BinaryOperator e,
          long index,
          List<de.fraunhofer.aisec.cpg.frontends.grpc.messages.Node> grpcNodes,
          List<Node> cpgNodes)
          throws TranslationException {
    var BinaryOperator = new de.fraunhofer.aisec.cpg.graph.statements.expressions.BinaryOperator();

    cpgNodes.set((int) index, BinaryOperator);

    return BinaryOperator;
  }

  private static de.fraunhofer.aisec.cpg.graph.statements.expressions.ArraySubscriptionExpression
      createArraySubscriptionExpression(
          de.fraunhofer.aisec.cpg.frontends.grpc.messages.ArraySubscriptionExpression e,
          long index,
          List<de.fraunhofer.aisec.cpg.frontends.grpc.messages.Node> grpcNodes,
          List<Node> cpgNodes)
          throws TranslationException {
    var ArraySubscriptionExpression =
        new de.fraunhofer.aisec.cpg.graph.statements.expressions.ArraySubscriptionExpression();

    cpgNodes.set((int) index, ArraySubscriptionExpression);

    return ArraySubscriptionExpression;
  }

  private static Type createType(
      de.fraunhofer.aisec.cpg.frontends.grpc.messages.Type t,
      long index,
      List<de.fraunhofer.aisec.cpg.frontends.grpc.messages.Node> grpcNodes,
      List<Node> cpgNodes)
      throws TranslationException {
    Type type;

    // TODO: isPrimitive must be handled

    if (t.hasObjectType()) {
      type = createObjectType(t.getObjectType(), index, grpcNodes, cpgNodes);
    } else if (t.hasIncompleteType()) {
      type = createIncompleteType(t.getIncompleteType(), index, grpcNodes, cpgNodes);
    } else {
      throw new TranslationException("Error in createType");
    }

    type.getQualifier().setConst(t.getQualifier().getIsConst());
    type.getQualifier().setVolatile(t.getQualifier().getIsVolatile());
    type.getQualifier().setRestrict(t.getQualifier().getIsRestrict());
    type.getQualifier().setAtomic(t.getQualifier().getIsAtomic());

    switch (t.getOrigin()) {
      case DATAFLOW:
        type.setTypeOrigin(Type.Origin.DATAFLOW);
        break;
      case GUESSED:
        type.setTypeOrigin(Type.Origin.GUESSED);
        break;
      case RESOLVED:
        type.setTypeOrigin(Type.Origin.RESOLVED);
        break;
      case UNRESOLVED:
        type.setTypeOrigin(Type.Origin.UNRESOLVED);
        break;
      default:
        System.out.println("Error in createType: Unknown origin.");
        break;
    }

    switch (t.getStorage()) {
      case AUTO:
        type.setStorage(Type.Storage.AUTO);
        break;
      case EXTERN:
        type.setStorage(Type.Storage.EXTERN);
        break;
      case STATIC:
        type.setStorage(Type.Storage.STATIC);
        break;
      case REGISTER:
        type.setStorage(Type.Storage.REGISTER);
        break;
      default:
        System.out.println("Error in createType: Unknown storage.");
        break;
    }

    for (var superTypeIndex : t.getSuperTypesList()) {
      var superType = createNode(superTypeIndex.getNodeIndex(), grpcNodes, cpgNodes);
      if (superType != null) {
        if (!(superType instanceof Type)) {
          throw new TranslationException(
              "node " + superTypeIndex.getNodeIndex() + " not of type type (in createType");
        }
        type.getSuperTypes().add((Type) superType);
      }
    }

    return type;
  }

  private static ObjectType createObjectType(
      de.fraunhofer.aisec.cpg.frontends.grpc.messages.ObjectType t,
      long index,
      List<de.fraunhofer.aisec.cpg.frontends.grpc.messages.Node> grpcNodes,
      List<Node> cpgNodes)
      throws TranslationException {
    var objectType = new ObjectType();

    for (var ignored : t.getGenericsList()) {
      var pe = NodeFactory.<Type>createPropertyEdgeNode(ignored, grpcNodes, cpgNodes);

      objectType.getGenericPropertyEdges().add(pe);
    }

    // recordDeclaration
    var recordDeclaration =
        createNode(t.getRecordDeclaration().getNodeIndex(), grpcNodes, cpgNodes);
    if (recordDeclaration != null) {
      if (!(recordDeclaration instanceof RecordDeclaration)) {
        throw new TranslationException(
            "node "
                + t.getRecordDeclaration().getNodeIndex()
                + " not of type funcDecl (in createObjectType");
      }
      objectType.setRecordDeclaration((RecordDeclaration) recordDeclaration);
    }

    // Modifier
    // TODO (can only be set in the constructor together with many other attributes)

    return objectType;
  }

  private static IncompleteType createIncompleteType(
      de.fraunhofer.aisec.cpg.frontends.grpc.messages.IncompleteType t,
      long index,
      List<de.fraunhofer.aisec.cpg.frontends.grpc.messages.Node> grpcNodes,
      List<Node> cpgNodes)
      throws TranslationException {

    return new IncompleteType();
  }

  private static <T extends Node> PropertyEdge<T> createPropertyEdgeNode(
      de.fraunhofer.aisec.cpg.frontends.grpc.messages.PropertyEdge p,
      List<de.fraunhofer.aisec.cpg.frontends.grpc.messages.Node> grpcNodes,
      List<Node> cpgNodes)
      throws TranslationException {
    Node start = createNode(p.getStart().getNodeIndex(), grpcNodes, cpgNodes);
    Node end = createNode(p.getEnd().getNodeIndex(), grpcNodes, cpgNodes);

    if (end == null || start == null) {
      throw new TranslationException("Error in createPropertyEdgeNode");
    }

    // TODO Add PropertyEdgeProperties
    // TODO How can the cast be checked? "end instanceof T" does not work

    try {
      return new PropertyEdge<T>(start, (T) end);
    } catch (ClassCastException e) {
      throw new TranslationException("No correct type.");
    }
  }
}
