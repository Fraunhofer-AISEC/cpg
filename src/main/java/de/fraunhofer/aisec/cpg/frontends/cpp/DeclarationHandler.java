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

package de.fraunhofer.aisec.cpg.frontends.cpp;

import de.fraunhofer.aisec.cpg.frontends.Handler;
import de.fraunhofer.aisec.cpg.graph.CompoundStatement;
import de.fraunhofer.aisec.cpg.graph.Declaration;
import de.fraunhofer.aisec.cpg.graph.FunctionDeclaration;
import de.fraunhofer.aisec.cpg.graph.IncludeDeclaration;
import de.fraunhofer.aisec.cpg.graph.NamespaceDeclaration;
import de.fraunhofer.aisec.cpg.graph.NodeBuilder;
import de.fraunhofer.aisec.cpg.graph.ProblemDeclaration;
import de.fraunhofer.aisec.cpg.graph.ReturnStatement;
import de.fraunhofer.aisec.cpg.graph.Statement;
import de.fraunhofer.aisec.cpg.graph.TranslationUnitDeclaration;
import de.fraunhofer.aisec.cpg.graph.Type;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTCompositeTypeSpecifier;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTFunctionDefinition;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTLinkageSpecification;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTName;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTNamespaceDefinition;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTProblemDeclaration;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTSimpleDeclaration;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTTranslationUnit;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTUsingDirective;

public class DeclarationHandler extends Handler<Declaration, IASTDeclaration, CXXLanguageFrontend> {

  public DeclarationHandler(CXXLanguageFrontend lang) {
    super(Declaration::new, lang);

    map.put(
        CPPASTSimpleDeclaration.class,
        ctx -> handleSimpleDeclaration((CPPASTSimpleDeclaration) ctx));
    map.put(
        CPPASTFunctionDefinition.class,
        ctx -> handleFunctionDefinition((CPPASTFunctionDefinition) ctx));
    //    map.put(
    //        CPPASTLinkageSpecification.class,
    //        ctx -> handleInclude((CPPASTLinkageSpecification) ctx));
    map.put(CPPASTProblemDeclaration.class, ctx -> handleProblem((CPPASTProblemDeclaration) ctx));
    map.put(
        CPPASTNamespaceDefinition.class, ctx -> handleNamespace((CPPASTNamespaceDefinition) ctx));
    map.put(CPPASTUsingDirective.class, ctx -> handleUsingDirective((CPPASTUsingDirective) ctx));
  }

  private Declaration handleUsingDirective(CPPASTUsingDirective using) {
    return NodeBuilder.newUsingDirective(
        using.getRawSignature(), using.getQualifiedName().toString());
  }

  private Declaration handleNamespace(CPPASTNamespaceDefinition ctx) {
    NamespaceDeclaration declaration =
        NodeBuilder.newNamespaceDeclaration(ctx.getName().toString());
    lang.getScopeManager().enterScope(declaration);
    for (IASTNode child : ctx.getChildren()) {
      if (child instanceof IASTDeclaration) {
        declaration.add(this.lang.getDeclarationHandler().handle((IASTDeclaration) child));
      } else if (child instanceof CPPASTName) {
        // this is the name of the namespace. Already parsed outside, skipping.
      } else {
        log.error("Unknown child in namespace: {}", child.getClass());
      }
    }
    lang.getScopeManager().enterScope(declaration);

    return declaration;
  }

  private Declaration handleProblem(CPPASTProblemDeclaration ctx) {
    return NodeBuilder.newProblemDeclaration(
        ctx.getContainingFilename(),
        ctx.getProblem().getMessage(),
        ctx.getProblem().getFileLocation().toString());
  }

  private FunctionDeclaration handleFunctionDefinition(CPPASTFunctionDefinition ctx) {
    // Todo: A problem with cpp functions is that we cannot know if they may throw an exception as
    // throw(...) is not compiler enforced (Problem for TryStatement)
    FunctionDeclaration functionDeclaration =
        (FunctionDeclaration) this.lang.getDeclaratorHandler().handle(ctx.getDeclarator());
    lang.getScopeManager().enterScope(functionDeclaration);

    // Pointer type is added in DeklaratorHandler.handleFunctionDeclarator
    String typeAdjustment = functionDeclaration.getType().getTypeAdjustment();
    functionDeclaration.setType(Type.createFrom(ctx.getDeclSpecifier().toString()));
    functionDeclaration.getType().setTypeAdjustment(typeAdjustment);

    Statement bodyStatement = this.lang.getStatementHandler().handle(ctx.getBody());

    if (bodyStatement instanceof CompoundStatement) {
      CompoundStatement body = (CompoundStatement) bodyStatement;
      List<Statement> statements = body.getStatements();

      // get the last statement
      Statement lastStatement = null;
      if (statements.size() > 1) {
        lastStatement = statements.get(statements.size() - 1);
      }
      // make sure, method contains a return statement
      // todo to-be-discussed: do we need a dummy return statement?
      if (!(lastStatement instanceof ReturnStatement)) {
        // statements.add(new StatementHandler(this.lang).handle(new CPPASTReturnStatement()));
        ReturnStatement returnStatement = NodeBuilder.newReturnStatement("return;");
        returnStatement.setDummy(true);
        statements.add(returnStatement);
      }
      functionDeclaration.setBody(body);
    }

    lang.getScopeManager().leaveScope(functionDeclaration);
    return functionDeclaration;
  }

  private Declaration handleSimpleDeclaration(CPPASTSimpleDeclaration ctx) {
    if (ctx.getDeclarators().length == 0) {
      if (ctx.getDeclSpecifier() != null) {
        if (ctx.getDeclSpecifier() instanceof CPPASTCompositeTypeSpecifier) {
          // probably a class or struct declaration
          Declaration declaration =
              this.lang
                  .getDeclaratorHandler()
                  .handle((CPPASTCompositeTypeSpecifier) ctx.getDeclSpecifier());

          return declaration;
        } else {
          log.error(
              "Unknown Declspecifier in SimpleDeclaration: {}", ctx.getDeclSpecifier().getClass());
        }
      } else {
        log.error("Declspecifier is null");
      }
    } else if (ctx.getDeclarators().length == 1) {

      List<Declaration> handle = (this.lang).getDeclarationListHandler().handle(ctx);
      if (handle.size() != 1) {
        log.error("Invalid declaration generation");
        return NodeBuilder.newDeclaration("");
      }

      return handle.get(0);
    } else {
      log.error("More than one declaration, this should not happen here.");
    }

    return null;
  }

  private void parseInclusions(
      IASTTranslationUnit.IDependencyTree.IASTInclusionNode[] includes,
      HashMap<String, HashSet<String>> allIncludes) {
    if (includes != null) {
      for (IASTTranslationUnit.IDependencyTree.IASTInclusionNode n : includes) {
        HashSet<String> strings =
            allIncludes.computeIfAbsent(
                n.getIncludeDirective().getContainingFilename(), k -> new HashSet<>());
        strings.add(n.getIncludeDirective().getPath());
        parseInclusions(n.getNestedInclusions(), allIncludes);
      }
    }
  }

  TranslationUnitDeclaration handleTranslationUnit(CPPASTTranslationUnit translationUnit) {
    TranslationUnitDeclaration node =
        NodeBuilder.newTranslationUnitDeclaration(
            translationUnit.getFilePath(), translationUnit.getRawSignature());

    HashMap<String, HashSet<ProblemDeclaration>> problematicIncludes = new HashMap<>();
    for (IASTDeclaration declaration : translationUnit.getDeclarations()) {
      if (declaration instanceof CPPASTLinkageSpecification) {
        continue; // do not care about these for now
      }

      Declaration decl = handle(declaration);
      if (decl instanceof ProblemDeclaration) {
        HashSet<ProblemDeclaration> problems =
            problematicIncludes.computeIfAbsent(
                ((ProblemDeclaration) decl).getFilename(), k -> new HashSet<>());
        problems.add((ProblemDeclaration) decl);
      } else if (decl instanceof NamespaceDeclaration) {
        node.add(decl);
      } else {
        node.add(decl);
      }
    }

    boolean addIncludesToGraph = true; // todo move to config
    if (addIncludesToGraph) {

      // this tree is a bit problematic: If a file was already included before, it will not be shown
      // connecting to other leaves.
      // I.e. if FileA includes FileB and FileC, and FileC also includes FileB, _no_ connection
      // between FileC and FileB will be shown.
      IASTTranslationUnit.IDependencyTree dependencyTree = translationUnit.getDependencyTree();
      HashMap<String, HashSet<String>> allIncludes = new HashMap<>();
      parseInclusions(dependencyTree.getInclusions(), allIncludes);

      //      for (Map.Entry<String, HashSet<String>> entry : allIncludes.entrySet()) {
      //        System.out.println(entry.getKey() + ":");
      //        for (String s : entry.getValue()) {
      //          System.out.println("\t" + s);
      //        }
      //      }

      if (allIncludes.size() > 0) {
        // create all include nodes, potentially attach problemdecl
        HashSet<String> includesStrings = new HashSet<>();
        HashMap<String, IncludeDeclaration> includeMap = new HashMap<>();
        allIncludes.values().forEach(includesStrings::addAll);
        for (String includeString : includesStrings) {
          HashSet<ProblemDeclaration> problems = problematicIncludes.get(includeString);

          IncludeDeclaration includeDeclaration = NodeBuilder.newIncludeDeclaration(includeString);
          if (problems != null) {
            includeDeclaration.getProblems().addAll(problems);
          }
          includeMap.put(includeString, includeDeclaration);
        }

        // attach to root note
        for (String incl : allIncludes.get(translationUnit.getFilePath())) {
          node.add(includeMap.get(incl));
        }
        allIncludes.remove(translationUnit.getFilePath());
        // attach to remaining nodes
        for (Map.Entry<String, HashSet<String>> entry : allIncludes.entrySet()) {
          IncludeDeclaration includeDeclaration = includeMap.get(entry.getKey());
          for (String s : entry.getValue()) {
            includeDeclaration.getIncludes().add(includeMap.get(s));
          }
        }
      }
    }

    return node;
  }
}
