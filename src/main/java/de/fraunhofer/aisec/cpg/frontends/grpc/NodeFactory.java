/*
 * Copyright (c) 2019, Fraunhofer AISEC. All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
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
import de.fraunhofer.aisec.cpg.graph.*;
import de.fraunhofer.aisec.cpg.graph.declarations.*;
import de.fraunhofer.aisec.cpg.graph.declarations.Declaration;
import de.fraunhofer.aisec.cpg.graph.statements.*;
import de.fraunhofer.aisec.cpg.graph.types.*;
import java.util.List;

public class NodeFactory {
  private NodeFactory() {}

  // Converts a de.fraunhofer.aisec.cpg.frontends.grpc.messages.Node (grpcNode)
  // to a de.fraunhofer.aisec.cpg.graph.Node (cpgNode). Also resolves and
  // creates all connected nodes.
  public static Node createNode(
      int index,
      List<de.fraunhofer.aisec.cpg.frontends.grpc.messages.Node> grpcNodes,
      List<Node> cpgNodes)
      throws TranslationException {
    // Sanity check
    if (index >= grpcNodes.size() || grpcNodes.size() != cpgNodes.size()) {
      throw new TranslationException("Wrong size");
    }

    // Check if node already has been initialized
    if (cpgNodes.get(index) != null) {
      return cpgNodes.get(index);
    }

    // Apparently we have to create a new node
    de.fraunhofer.aisec.cpg.frontends.grpc.messages.Node grpcNode = grpcNodes.get(index);
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

    // TODO: Iterate over the declarations, namespaces etc. to create the new nodes

    return cpgNode;
  }

  private static Annotation createAnnotation(
      de.fraunhofer.aisec.cpg.frontends.grpc.messages.Annotation a,
      int index,
      List<de.fraunhofer.aisec.cpg.frontends.grpc.messages.Node> grpcNodes,
      List<Node> cpgNodes)
      throws TranslationException {
    Annotation annotation = new Annotation();
    return annotation;
  }

  private static Declaration createDeclaration(
      de.fraunhofer.aisec.cpg.frontends.grpc.messages.Declaration d,
      int index,
      List<de.fraunhofer.aisec.cpg.frontends.grpc.messages.Node> grpcNodes,
      List<Node> cpgNodes)
      throws TranslationException {
    Declaration declaration;

    if (d.hasTranslationUnitDeclaration()) {
      declaration =
          createTranslationUnitDeclaration(
              d.getTranslationUnitDeclaration(), index, grpcNodes, cpgNodes);
    } else if (d.hasValueDeclaration()) {
      declaration = createValueDeclaration(d.getValueDeclaration(), index, grpcNodes, cpgNodes);
    } else if (d.hasNamespaceDeclaration()) {
      declaration =
          createNamespaceDeclaration(d.getNamespaceDeclaration(), index, grpcNodes, cpgNodes);
    } else {
      throw new TranslationException("Error in NodeFactory!");
    }

    return declaration;
  }

  private static de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
      createTranslationUnitDeclaration(
          de.fraunhofer.aisec.cpg.frontends.grpc.messages.TranslationUnitDeclaration d,
          int index,
          List<de.fraunhofer.aisec.cpg.frontends.grpc.messages.Node> grpcNodes,
          List<Node> cpgNodes)
          throws TranslationException {

    de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration tud =
        new de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration();
    cpgNodes.set(index, tud);

    // TODO: All 3 parts do exactly the same thing. Maybe think if we want to unify that

    // Declarations
    for (de.fraunhofer.aisec.cpg.frontends.grpc.messages.NodeIndex idx : d.getDeclarationsList()) {
      System.out.println("decl");
      Node n = createNode(idx.getIndex(), grpcNodes, cpgNodes);
      if (!(n instanceof Declaration)) {
        throw new TranslationException("Node at " + idx.getIndex() + " is not a declaration.");
      }
      tud.addDeclaration((Declaration) createNode(idx.getIndex(), grpcNodes, cpgNodes));
    }
    // Includes
    for (de.fraunhofer.aisec.cpg.frontends.grpc.messages.NodeIndex idx : d.getIncludesList()) {
      System.out.println("include");
      Node n = createNode(idx.getIndex(), grpcNodes, cpgNodes);
      if (!(n instanceof Declaration)) {
        throw new TranslationException("Node at " + idx.getIndex() + " is not a declaration.");
      }
      tud.addDeclaration((Declaration) createNode(idx.getIndex(), grpcNodes, cpgNodes));
    }
    // Namespaces
    for (de.fraunhofer.aisec.cpg.frontends.grpc.messages.NodeIndex idx : d.getNamespacesList()) {
      System.out.println("namespace");
      Node n = createNode(idx.getIndex(), grpcNodes, cpgNodes);
      if (!(n instanceof Declaration)) {
        throw new TranslationException("Node at " + idx.getIndex() + " is not a declaration.");
      }
      tud.addDeclaration((Declaration) createNode(idx.getIndex(), grpcNodes, cpgNodes));
    }

    return tud;
  }

  private static de.fraunhofer.aisec.cpg.graph.declarations.ValueDeclaration createValueDeclaration(
      de.fraunhofer.aisec.cpg.frontends.grpc.messages.ValueDeclaration v,
      int index,
      List<de.fraunhofer.aisec.cpg.frontends.grpc.messages.Node> grpcNodes,
      List<Node> cpgNodes)
      throws TranslationException {
    de.fraunhofer.aisec.cpg.graph.declarations.ValueDeclaration valueDeclaration;

    if (v.hasVariableDeclaration()) {
      valueDeclaration =
          createVariableDeclaration(v.getVariableDeclaration(), index, grpcNodes, cpgNodes);
    } else if (v.hasFunctionDeclaration()) {
      valueDeclaration =
          createFunctionDeclaration(v.getFunctionDeclaration(), index, grpcNodes, cpgNodes);
    } else {
      throw new TranslationException("Error in NodeFactory!");
    }

    return valueDeclaration;
  }

  private static de.fraunhofer.aisec.cpg.graph.declarations.NamespaceDeclaration
      createNamespaceDeclaration(
          de.fraunhofer.aisec.cpg.frontends.grpc.messages.NamespaceDeclaration d,
          int index,
          List<de.fraunhofer.aisec.cpg.frontends.grpc.messages.Node> grpcNodes,
          List<Node> cpgNodes)
          throws TranslationException {
    de.fraunhofer.aisec.cpg.graph.declarations.NamespaceDeclaration namespaceDeclaration =
        new de.fraunhofer.aisec.cpg.graph.declarations.NamespaceDeclaration();
    cpgNodes.set(index, namespaceDeclaration);

    for (de.fraunhofer.aisec.cpg.frontends.grpc.messages.NodeIndex idx : d.getDeclarationsList()) {
      Node n = createNode(idx.getIndex(), grpcNodes, cpgNodes);
      System.out.println("Found a decl in namespaces :)");
      if (!(n instanceof Declaration)) {
        throw new TranslationException("Node at " + idx.getIndex() + " is not a declaration.");
      }
      namespaceDeclaration.getDeclarations().add((Declaration) n);
    }

    return namespaceDeclaration;
  }

  private static de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration
      createVariableDeclaration(
          de.fraunhofer.aisec.cpg.frontends.grpc.messages.VariableDeclaration v,
          int index,
          List<de.fraunhofer.aisec.cpg.frontends.grpc.messages.Node> grpcNodes,
          List<Node> cpgNodes)
          throws TranslationException {
    de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration variableDeclaration =
        new de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration();
    variableDeclaration.setIsArray(v.getIsArray());
    variableDeclaration.setImplicitInitializerAllowed(v.getImplicitInitializerAllowed());
    return variableDeclaration;
  }

  private static de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
      createFunctionDeclaration(
          de.fraunhofer.aisec.cpg.frontends.grpc.messages.FunctionDeclaration v,
          int index,
          List<de.fraunhofer.aisec.cpg.frontends.grpc.messages.Node> grpcNodes,
          List<Node> cpgNodes)
          throws TranslationException {
    de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration functionDeclaration =
        new de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration();
    functionDeclaration.setBody(
        (de.fraunhofer.aisec.cpg.graph.statements.Statement)
            createNode(v.getBody().getIndex(), grpcNodes, cpgNodes));
    System.out.println("Here in CreateFunctionDecl");
    return functionDeclaration;
  }

  private static de.fraunhofer.aisec.cpg.graph.statements.Statement createStatement(
      de.fraunhofer.aisec.cpg.frontends.grpc.messages.Statement s,
      int index,
      List<de.fraunhofer.aisec.cpg.frontends.grpc.messages.Node> grpcNodes,
      List<Node> cpgNodes)
      throws TranslationException {
    return new de.fraunhofer.aisec.cpg.graph.statements.Statement();
  }

  private static Type createType(
      de.fraunhofer.aisec.cpg.frontends.grpc.messages.Type t,
      int index,
      List<de.fraunhofer.aisec.cpg.frontends.grpc.messages.Node> grpcNodes,
      List<Node> cpgNodes)
      throws TranslationException {
    Type type;

    if (t.hasObjectType()) {
      type = createObjectType(t.getObjectType(), index, grpcNodes, cpgNodes);
    } else {
      throw new TranslationException("Error in NodeFactory!");
    }

    return type;
  }

  private static ObjectType createObjectType(
      de.fraunhofer.aisec.cpg.frontends.grpc.messages.ObjectType t,
      int index,
      List<de.fraunhofer.aisec.cpg.frontends.grpc.messages.Node> grpcNodes,
      List<Node> cpgNodes) {
    ObjectType objectType = new ObjectType();
    return objectType;
  }
}
