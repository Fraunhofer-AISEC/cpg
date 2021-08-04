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
package de.fraunhofer.aisec.cpg.frontends.cpp;

import static de.fraunhofer.aisec.cpg.helpers.Util.errorWithFileLocation;
import static de.fraunhofer.aisec.cpg.helpers.Util.warnWithFileLocation;

import de.fraunhofer.aisec.cpg.frontends.Handler;
import de.fraunhofer.aisec.cpg.graph.Node;
import de.fraunhofer.aisec.cpg.graph.NodeBuilder;
import de.fraunhofer.aisec.cpg.graph.TypeManager;
import de.fraunhofer.aisec.cpg.graph.declarations.*;
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge;
import de.fraunhofer.aisec.cpg.graph.statements.CompoundStatement;
import de.fraunhofer.aisec.cpg.graph.statements.ReturnStatement;
import de.fraunhofer.aisec.cpg.graph.statements.Statement;
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression;
import de.fraunhofer.aisec.cpg.graph.types.ObjectType;
import de.fraunhofer.aisec.cpg.graph.types.ParameterizedType;
import de.fraunhofer.aisec.cpg.graph.types.Type;
import de.fraunhofer.aisec.cpg.graph.types.TypeParser;
import de.fraunhofer.aisec.cpg.helpers.Util;
import de.fraunhofer.aisec.cpg.passes.scopes.RecordScope;
import de.fraunhofer.aisec.cpg.passes.scopes.TemplateScope;
import java.util.*;
import java.util.stream.Collectors;
import org.eclipse.cdt.core.dom.ast.*;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTemplateParameter;
import org.eclipse.cdt.internal.core.dom.parser.cpp.*;

public class DeclarationHandler extends Handler<Declaration, IASTDeclaration, CXXLanguageFrontend> {

  public DeclarationHandler(CXXLanguageFrontend lang) {
    super(Declaration::new, lang);

    map.put(
        CPPASTTemplateDeclaration.class,
        ctx -> handleTemplateDeclaration((CPPASTTemplateDeclaration) ctx));
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
        NodeBuilder.newNamespaceDeclaration(ctx.getName().toString(), lang.getCodeFromRawNode(ctx));

    lang.getScopeManager().addDeclaration(declaration);

    lang.getScopeManager().enterScope(declaration);
    for (IASTNode child : ctx.getChildren()) {
      if (child instanceof IASTDeclaration) {
        handle((IASTDeclaration) child);
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
    var problem =
        NodeBuilder.newProblemDeclaration(
            ctx.getContainingFilename(),
            ctx.getProblem().getMessage(),
            ctx.getProblem().getFileLocation().toString());

    this.lang.getScopeManager().addDeclaration(problem);

    return problem;
  }

  private FunctionDeclaration handleFunctionDefinition(CPPASTFunctionDefinition ctx) {
    // Todo: A problem with cpp functions is that we cannot know if they may throw an exception as
    // throw(...) is not compiler enforced (Problem for TryStatement)

    FunctionDeclaration functionDeclaration =
        (FunctionDeclaration) this.lang.getDeclaratorHandler().handle(ctx.getDeclarator());

    String typeString = getTypeStringFromDeclarator(ctx.getDeclarator(), ctx.getDeclSpecifier());

    functionDeclaration.setIsDefinition(true);
    functionDeclaration.setType(TypeParser.createFrom(typeString, true, lang));

    // associated record declaration if this is a method or constructor
    RecordDeclaration recordDeclaration =
        functionDeclaration instanceof MethodDeclaration
            ? ((MethodDeclaration) functionDeclaration).getRecordDeclaration()
            : null;
    var outsideOfRecord =
        !(lang.getScopeManager().getCurrentScope() instanceof RecordScope
            || lang.getScopeManager().getCurrentScope() instanceof TemplateScope);

    if (recordDeclaration != null) {
      if (outsideOfRecord) {
        // everything inside the method is within the scope of its record
        this.lang.getScopeManager().enterScope(recordDeclaration);
      }

      // update the definition
      List<? extends MethodDeclaration> candidates;

      if (functionDeclaration instanceof ConstructorDeclaration) {
        candidates = recordDeclaration.getConstructors();
      } else {
        candidates = recordDeclaration.getMethods();
      }

      // look for the method or constructor
      FunctionDeclaration finalFunctionDeclaration = functionDeclaration;
      candidates =
          candidates.stream()
              .filter(m -> m.getSignature().equals(finalFunctionDeclaration.getSignature()))
              .collect(Collectors.toList());

      if (candidates.isEmpty()
          && !(lang.getScopeManager().getCurrentScope() instanceof TemplateScope)) {
        log.warn(
            "Could not find declaration of method {} in record {}",
            functionDeclaration.getName(),
            recordDeclaration.getName());
      } else if (candidates.size() > 1) {
        log.warn(
            "Found more than one candidate to connect definition of method {} in record {} to its declaration. We will comply, but this is suspicious.",
            functionDeclaration.getName(),
            recordDeclaration.getName());
      }

      for (MethodDeclaration candidate : candidates) {
        candidate.setDefinition(functionDeclaration);
      }
    }

    lang.getScopeManager().enterScope(functionDeclaration);

    functionDeclaration.setType(TypeParser.createFrom(typeString, true, lang));

    if (ctx.getBody() != null) {
      Statement bodyStatement = this.lang.getStatementHandler().handle(ctx.getBody());

      if (bodyStatement instanceof CompoundStatement) {
        CompoundStatement body = (CompoundStatement) bodyStatement;
        List<PropertyEdge<Statement>> statements = body.getStatementEdges();

        // get the last statement
        Statement lastStatement = null;
        if (!statements.isEmpty()) {
          lastStatement = statements.get(statements.size() - 1).getEnd();
        }

        // add an implicit return statement, if there is none
        if (!(lastStatement instanceof ReturnStatement)) {
          ReturnStatement returnStatement = NodeBuilder.newReturnStatement("return;");
          returnStatement.setImplicit(true);
          body.addStatement(returnStatement);
        }

        functionDeclaration.setBody(body);
      }
    }

    this.lang.processAttributes(functionDeclaration, ctx);

    lang.getScopeManager().leaveScope(functionDeclaration);

    if (recordDeclaration != null && outsideOfRecord) {
      this.lang.getScopeManager().leaveScope(recordDeclaration);
    }

    // Check for declarations of the same function
    List<FunctionDeclaration> declarationCandidates =
        lang.getCurrentTU().getDeclarations().stream()
            .filter(FunctionDeclaration.class::isInstance)
            .map(FunctionDeclaration.class::cast)
            .filter(f -> !f.isDefinition() && f.hasSameSignature(functionDeclaration))
            .collect(Collectors.toList());
    for (FunctionDeclaration declaration : declarationCandidates) {
      declaration.setDefinition(functionDeclaration);
      for (int i = 0; i < functionDeclaration.getParameters().size(); i++) {
        if (declaration.getParameters().get(i).getDefault() != null) {
          functionDeclaration
              .getParameters()
              .get(i)
              .setDefault(declaration.getParameters().get(i).getDefault());
        }
      }
    }

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

  private Declaration handleTemplateDeclaration(CPPASTTemplateDeclaration ctx) {

    TemplateDeclaration templateDeclaration;
    if (ctx.getDeclaration() instanceof CPPASTFunctionDefinition) {
      templateDeclaration =
          NodeBuilder.newFunctionTemplateDeclaration(
              ctx.getRawSignature().split("\\{")[0].replace('\n', ' ').trim(),
              this.lang.getCodeFromRawNode(ctx));
    } else {
      templateDeclaration =
          NodeBuilder.newClassTemplateDeclaration(
              ctx.getRawSignature().split("\\{")[0].replace('\n', ' ').trim(),
              this.lang.getCodeFromRawNode(ctx));
    }
    templateDeclaration.setLocation(this.lang.getLocationFromRawNode(ctx));
    lang.getScopeManager().addDeclaration(templateDeclaration);
    lang.getScopeManager().enterScope(templateDeclaration);

    addTemplateParameters(ctx, templateDeclaration);

    // Handle Template
    Declaration innerDeclaration = lang.getDeclarationHandler().handle(ctx.getDeclaration());
    lang.getScopeManager().leaveScope(templateDeclaration);

    if (templateDeclaration instanceof FunctionTemplateDeclaration) {
      // Fix typeName
      templateDeclaration.setName(
          templateDeclaration.getRealizationDeclarations().get(0).getName());
    } else if (innerDeclaration instanceof RecordDeclaration) {
      fixTypeOfInnerDeclaration(templateDeclaration, innerDeclaration);
    }
    addRealizationToScope(templateDeclaration);
    return templateDeclaration;
  }

  /**
   * Add Template Parameters to the TemplateDeclaration
   *
   * @param ctx
   * @param templateDeclaration
   */
  private void addTemplateParameters(
      CPPASTTemplateDeclaration ctx, TemplateDeclaration templateDeclaration) {
    for (ICPPASTTemplateParameter templateParameter : ctx.getTemplateParameters()) {
      if (templateParameter instanceof CPPASTSimpleTypeTemplateParameter) {
        // Handle Type Parameters
        TypeParamDeclaration typeParamDeclaration =
            (TypeParamDeclaration)
                this.lang
                    .getDeclaratorHandler()
                    .handle((CPPASTSimpleTypeTemplateParameter) templateParameter);
        ParameterizedType parameterizedType =
            TypeManager.getInstance()
                .createOrGetTypeParameter(
                    templateDeclaration,
                    ((CPPASTSimpleTypeTemplateParameter) templateParameter).getName().toString());

        typeParamDeclaration.setType(parameterizedType);

        if (((CPPASTSimpleTypeTemplateParameter) templateParameter).getDefaultType() != null) {
          Type defaultType =
              TypeParser.createFrom(
                  ((CPPASTSimpleTypeTemplateParameter) templateParameter)
                      .getDefaultType()
                      .getDeclSpecifier()
                      .getRawSignature(),
                  false,
                  lang);
          typeParamDeclaration.setDefault(defaultType);
        }

        templateDeclaration.addParameter(typeParamDeclaration);
      } else if (templateParameter instanceof CPPASTParameterDeclaration) {
        // Handle Value Parameters
        ParamVariableDeclaration nonTypeTemplateParamDeclaration =
            this.lang
                .getParameterDeclarationHandler()
                .handle((IASTParameterDeclaration) templateParameter);

        if (((CPPASTParameterDeclaration) templateParameter).getDeclarator().getInitializer()
            != null) {
          Expression defaultExpression =
              this.lang
                  .getInitializerHandler()
                  .handle(
                      ((CPPASTParameterDeclaration) templateParameter)
                          .getDeclarator()
                          .getInitializer());
          nonTypeTemplateParamDeclaration.setDefault(defaultExpression);
          nonTypeTemplateParamDeclaration.addPrevDFG(defaultExpression);
          defaultExpression.addNextDFG(nonTypeTemplateParamDeclaration);
        }
        templateDeclaration.addParameter(nonTypeTemplateParamDeclaration);
        lang.getScopeManager().addDeclaration(nonTypeTemplateParamDeclaration);
      }
    }
  }

  /**
   * Adds the generic realization of the template to the scope
   *
   * @param templateDeclaration
   */
  private void addRealizationToScope(TemplateDeclaration templateDeclaration) {
    for (Declaration declaration : templateDeclaration.getRealizationDeclarations()) {
      lang.getScopeManager().addDeclaration(declaration);
    }
  }

  /**
   * Fixed the Types created by the innerDeclaration with the ParameterizedTypes of the
   * TemplateDeclaration
   *
   * @param templateDeclaration
   * @param innerDeclaration If RecordDeclaration
   */
  private void fixTypeOfInnerDeclaration(
      TemplateDeclaration templateDeclaration, Declaration innerDeclaration) {
    Type type;
    if (((RecordDeclaration) innerDeclaration).getThis() == null) {
      type = TypeParser.createFrom(innerDeclaration.getName(), true);
    } else {
      type = ((RecordDeclaration) innerDeclaration).getThis().getType();
    }
    List<ParameterizedType> parameterizedTypes =
        TypeManager.getInstance().getAllParameterizedType(templateDeclaration);
    // Add ParameterizedTypes to type
    addParameterizedTypesToType(type, parameterizedTypes);

    // Add ParameterizedTypes to ConstructorDeclaration Type
    for (ConstructorDeclaration constructorDeclaration :
        ((RecordDeclaration) innerDeclaration).getConstructors()) {
      type = constructorDeclaration.getType();
      addParameterizedTypesToType(type, parameterizedTypes);
    }
  }

  /**
   * Connects the ObjectType node to the ParameterizedType nodes with the generics edge
   *
   * @param type
   * @param parameterizedTypes
   */
  private void addParameterizedTypesToType(Type type, List<ParameterizedType> parameterizedTypes) {
    if (type instanceof ObjectType) {
      for (ParameterizedType parameterizedType : parameterizedTypes) {
        ((ObjectType) type).addGeneric(parameterizedType);
      }
    }
  }

  private Declaration handleSimpleDeclaration(CPPASTSimpleDeclaration ctx) {
    if (isTypedef(ctx)) {
      TypeManager.getInstance().handleTypedef(ctx.getRawSignature());
    }

    DeclarationSequence sequence = new DeclarationSequence();

    // check, whether the declaration specifier also contains declarations, i.e. class definitions
    IASTDeclSpecifier declSpecifier = ctx.getDeclSpecifier();

    if (declSpecifier instanceof CPPASTCompositeTypeSpecifier) {
      Declaration declaration =
          this.lang
              .getDeclaratorHandler()
              .handle((CPPASTCompositeTypeSpecifier) ctx.getDeclSpecifier());

      // handle typedef
      if ((declaration.getName().isEmpty()
          && ctx.getRawSignature().strip().startsWith("typedef"))) {
        // CDT didn't find out the name due to this thing being a typedef. We need to fix this
        int endOfDeclaration = ctx.getRawSignature().lastIndexOf('}');
        if (endOfDeclaration + 1 < ctx.getRawSignature().length()) {
          List<String> parts =
              Util.splitLeavingParenthesisContents(
                  ctx.getRawSignature().substring(endOfDeclaration + 1), ",");
          Optional<String> name =
              parts.stream().filter(p -> !p.contains("*") && !p.contains("[")).findFirst();
          name.ifPresent(s -> declaration.setName(s.replace(";", "")));
        }
      }

      this.lang.processAttributes(declaration, ctx);

      sequence.addDeclaration(declaration);
    } else if (declSpecifier instanceof CPPASTElaboratedTypeSpecifier) {
      warnWithFileLocation(
          lang,
          ctx,
          log,
          "Parsing elaborated type specifiers is not supported (yet)",
          declSpecifier.getClass());
    }

    if (declSpecifier instanceof CPPASTNamedTypeSpecifier
        && ((CPPASTNamedTypeSpecifier) declSpecifier).getName() instanceof CPPASTTemplateId) {
      handleTemplateUsage((CPPASTNamedTypeSpecifier) declSpecifier, ctx, sequence);
    } else {
      for (IASTDeclarator declarator : ctx.getDeclarators()) {
        ValueDeclaration declaration =
            (ValueDeclaration) this.lang.getDeclaratorHandler().handle(declarator);

        String typeString = getTypeStringFromDeclarator(declarator, ctx.getDeclSpecifier());

        Type result = TypeParser.createFrom(typeString, true, lang);
        declaration.setType(result);

        // cache binding
        this.lang.cacheDeclaration(declarator.getName().resolveBinding(), declaration);

        // process attributes
        this.lang.processAttributes(declaration, ctx);

        sequence.addDeclaration(declaration);
      }
    }

    return simplifySequence(sequence);
  }

  /**
   * @param sequence
   * @return First Element of DeclarationSequence if the Sequence consist of only one element, full
   *     sequence if it contains more than one element
   */
  private Declaration simplifySequence(DeclarationSequence sequence) {
    if (sequence.isSingle()) {
      return sequence.first();
    } else {
      return sequence;
    }
  }

  /**
   * Handles usage of Templates in SimpleDeclarations
   *
   * @param declSpecifier
   * @param ctx
   * @param sequence
   */
  private void handleTemplateUsage(
      CPPASTNamedTypeSpecifier declSpecifier,
      CPPASTSimpleDeclaration ctx,
      DeclarationSequence sequence) {
    CPPASTTemplateId templateId = (CPPASTTemplateId) (declSpecifier).getName();
    Type type = TypeParser.createFrom(ctx.getRawSignature(), true);
    List<Node> templateParams = new ArrayList<>();
    assert type.getRoot() instanceof ObjectType;
    ObjectType objectType = (ObjectType) type.getRoot();
    objectType.setGenerics(Collections.emptyList());

    for (IASTNode templateArgument : templateId.getTemplateArguments()) {
      if (templateArgument instanceof CPPASTTypeId) {
        Type genericInstantiation = TypeParser.createFrom(templateArgument.getRawSignature(), true);
        objectType.addGeneric(genericInstantiation);
        templateParams.add(
            NodeBuilder.newTypeExpression(genericInstantiation.getName(), genericInstantiation));
      } else if (templateArgument instanceof IASTExpression) {
        Expression expression =
            this.lang.getExpressionHandler().handle((IASTExpression) templateArgument);
        templateParams.add(expression);
      }
    }

    for (IASTDeclarator declarator : ctx.getDeclarators()) {
      ValueDeclaration declaration =
          (ValueDeclaration) this.lang.getDeclaratorHandler().handle(declarator);

      // Update Type
      declaration.setType(type);

      // Set TemplateParameters into VariableDeclaration
      if (declaration instanceof VariableDeclaration) {
        ((VariableDeclaration) declaration).setTemplateParameters(templateParams);
      }

      // cache binding
      this.lang.cacheDeclaration(declarator.getName().resolveBinding(), declaration);

      // process attributes
      this.lang.processAttributes(declaration, ctx);

      sequence.addDeclaration(declaration);
    }
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
    lang.getScopeManager().resetToGlobal(node);

    lang.setCurrentTU(node);

    HashMap<String, HashSet<ProblemDeclaration>> problematicIncludes = new HashMap<>();
    for (IASTDeclaration declaration : translationUnit.getDeclarations()) {
      if (declaration instanceof CPPASTLinkageSpecification) {
        continue; // do not care about these for now
      }

      var decl = handle(declaration);
      if (decl == null) {
        continue;
      }

      if (decl instanceof ProblemDeclaration) {
        HashSet<ProblemDeclaration> problems =
            problematicIncludes.computeIfAbsent(
                ((ProblemDeclaration) decl).getFilename(), k -> new HashSet<>());
        problems.add((ProblemDeclaration) decl);
      }
    }

    // TODO: Remark CB: I am not quite sure, what the point of the code beyord this line is.
    // Probably needs to be refactored
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
            includeDeclaration.addProblems(problems);
          }
          includeMap.put(includeString, includeDeclaration);
        }

        // attach to root note
        for (String incl : allIncludes.get(translationUnit.getFilePath())) {
          node.addDeclaration(includeMap.get(incl));
        }

        allIncludes.remove(translationUnit.getFilePath());
        // attach to remaining nodes
        for (Map.Entry<String, HashSet<String>> entry : allIncludes.entrySet()) {
          IncludeDeclaration includeDeclaration = includeMap.get(entry.getKey());
          for (String s : entry.getValue()) {
            includeDeclaration.addInclude(includeMap.get(s));
          }
        }
      }
    }

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
