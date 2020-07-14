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

import static de.fraunhofer.aisec.cpg.helpers.Util.errorWithFileLocation;

import de.fraunhofer.aisec.cpg.frontends.Handler;
import de.fraunhofer.aisec.cpg.graph.*;
import de.fraunhofer.aisec.cpg.graph.TypeManager;
import de.fraunhofer.aisec.cpg.graph.type.TypeParser;
import de.fraunhofer.aisec.cpg.helpers.Util;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.eclipse.cdt.core.dom.ast.IASTArrayModifier;
import org.eclipse.cdt.core.dom.ast.IASTDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTPointerOperator;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.internal.core.dom.parser.cpp.*;

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
        errorWithFileLocation(
            this.lang, ctx, log, "Unknown child in namespace: {}", child.getClass());
      }
    }
    lang.getScopeManager().leaveScope(declaration);

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

    String typeString = getTypeStringFromDeclarator(ctx.getDeclarator(), ctx.getDeclSpecifier());

    // It is a constructor
    if (functionDeclaration instanceof MethodDeclaration && typeString.isEmpty()) {
      functionDeclaration = ConstructorDeclaration.from((MethodDeclaration) functionDeclaration);
    }

    // Add it to the record declaration if its a method or constructor
    if (functionDeclaration instanceof MethodDeclaration
        && ((MethodDeclaration) functionDeclaration).getRecordDeclaration() != null) {
      RecordDeclaration recordDeclaration =
          ((MethodDeclaration) functionDeclaration).getRecordDeclaration();
      if (recordDeclaration != null) {
        recordDeclaration.getMethods().add((MethodDeclaration) functionDeclaration);
      }
    }

    if (functionDeclaration instanceof ConstructorDeclaration
        && ((MethodDeclaration) functionDeclaration).getRecordDeclaration() != null) {
      RecordDeclaration recordDeclaration =
          ((MethodDeclaration) functionDeclaration).getRecordDeclaration();
      if (recordDeclaration != null) {
        recordDeclaration.getConstructors().add((ConstructorDeclaration) functionDeclaration);
      }
    }

    lang.getScopeManager().enterScope(functionDeclaration);

    functionDeclaration.setType(TypeParser.createFrom(typeString, true));

    if (ctx.getBody() != null) {
      Statement bodyStatement = this.lang.getStatementHandler().handle(ctx.getBody());

      if (bodyStatement instanceof CompoundStatement) {
        CompoundStatement body = (CompoundStatement) bodyStatement;
        List<Statement> statements = body.getStatements();

        // get the last statement
        Statement lastStatement = null;
        if (!statements.isEmpty()) {
          lastStatement = statements.get(statements.size() - 1);
        }

        // add an implicit return statement, if there is none
        if (!(lastStatement instanceof ReturnStatement)) {
          ReturnStatement returnStatement = NodeBuilder.newReturnStatement("return;");
          returnStatement.setImplicit(true);
          statements.add(returnStatement);
        }

        functionDeclaration.setBody(body);
      }
    }

    lang.getScopeManager().leaveScope(functionDeclaration);
    return functionDeclaration;
  }

  private boolean isTypedef(CPPASTSimpleDeclaration ctx) {
    if (ctx.getRawSignature().contains("typedef")) {
      if (ctx.getDeclSpecifier() instanceof CPPASTCompositeTypeSpecifier) {
        // we need to make a difference between structs that have typedefs and structs that are
        // typedefs themselves
        return ctx.getDeclSpecifier().toString().equals("struct")
            && ctx.getRawSignature().strip().startsWith("typedef");
      } else {
        return true;
      }
    } else {
      return false;
    }
  }

  private Declaration handleSimpleDeclaration(CPPASTSimpleDeclaration ctx) {
    if (isTypedef(ctx)) {
      TypeManager.getInstance().handleTypedef(ctx.getRawSignature());
      // if this was a struct typedef, we still need to handle this struct!
      if (!(ctx.getDeclSpecifier() instanceof CPPASTCompositeTypeSpecifier)) {
        return null;
      }
    }

    if (ctx.getDeclarators().length == 0) {
      return handleNoDeclarator(ctx);
    } else if (ctx.getDeclarators().length == 1) {
      return handleSingleDeclarator(ctx);
    } else {
      return handleMultipleDeclarators(ctx);
    }
  }

  private Declaration handleNoDeclarator(CPPASTSimpleDeclaration ctx) {
    if (ctx.getDeclSpecifier() != null) {
      if (ctx.getDeclSpecifier() instanceof CPPASTCompositeTypeSpecifier) {
        // probably a class or struct declaration
        return this.lang
            .getDeclaratorHandler()
            .handle((CPPASTCompositeTypeSpecifier) ctx.getDeclSpecifier());
      } else {
        errorWithFileLocation(
            this.lang,
            ctx,
            log,
            "Unknown DeclSpecifier in SimpleDeclaration: {}",
            ctx.getDeclSpecifier().getClass());
      }
    } else {
      errorWithFileLocation(this.lang, ctx, log, ("DeclSpecifier is null"));
    }
    return null;
  }

  private Declaration handleSingleDeclarator(CPPASTSimpleDeclaration ctx) {
    List<Declaration> handle = (this.lang).getDeclarationListHandler().handle(ctx);
    if (handle.size() != 1) {
      errorWithFileLocation(this.lang, ctx, log, "Invalid declaration generation");
      return NodeBuilder.newDeclaration("");
    }

    return handle.get(0);
  }

  private Declaration handleMultipleDeclarators(CPPASTSimpleDeclaration ctx) {
    // A legitimate case where this will happen is when multiply typedefing a struct
    // e.g.: typedef struct {...} S, *pS, s_arr[10], ...;
    if (ctx.getDeclSpecifier() instanceof CPPASTCompositeTypeSpecifier) {
      Declaration result =
          this.lang
              .getDeclaratorHandler()
              .handle((CPPASTCompositeTypeSpecifier) ctx.getDeclSpecifier());
      if (result.getName().isEmpty() && ctx.getRawSignature().strip().startsWith("typedef")) {
        // CDT didn't find out the name due to this thing being a typedef. We need to fix this
        int endOfDeclaration = ctx.getRawSignature().lastIndexOf('}');
        if (endOfDeclaration + 1 < ctx.getRawSignature().length()) {
          List<String> parts =
              Util.splitLeavingParenthesisContents(
                  ctx.getRawSignature().substring(endOfDeclaration + 1), ",");
          Optional<String> name =
              parts.stream().filter(p -> !p.contains("*") && !p.contains("[")).findFirst();
          name.ifPresent(s -> result.setName(s.replace(";", "")));
        }
      }
      return result;
    }
    errorWithFileLocation(
        this.lang, ctx, log, "More than one declaration, this should not happen here.");
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

    // There might have been errors in the previous translation unit and in any case
    // we need to reset the scope manager scope to global, to avoid spilling scope errors into other
    // translation units
    lang.getScopeManager().enterScope(node);

    lang.setCurrentTU(node);

    HashMap<String, HashSet<ProblemDeclaration>> problematicIncludes = new HashMap<>();
    for (IASTDeclaration declaration : translationUnit.getDeclarations()) {
      if (declaration instanceof CPPASTLinkageSpecification) {
        continue; // do not care about these for now
      }

      Declaration decl = handle(declaration);
      if (decl == null) {
        continue;
      }

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

    // wrap the translation unit into its own global scope
    lang.getScopeManager().leaveScope(node);

    return node;
  }

  /**
   * Returns a raw type string (that can be parsed by the {@link TypeParser} out of a cpp declarator
   * and associated declaration specifiers.
   *
   * @param declarator the declarator
   * @param declSpecifier the declaration specifier
   * @return the type string
   */
  static String getTypeStringFromDeclarator(
      IASTDeclarator declarator, IASTDeclSpecifier declSpecifier) {
    // use the declaration specifier as basis
    StringBuilder typeString = new StringBuilder(declSpecifier.toString());

    // append names, pointer operators and array modifiers and such
    for (IASTNode node : declarator.getChildren()) {
      if (node instanceof IASTPointerOperator || node instanceof IASTArrayModifier) {
        typeString.append(node.getRawSignature());
      }
    }

    return typeString.toString();
  }
}
