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

package de.fraunhofer.aisec.cpg.frontends.java;

import static com.github.javaparser.ParseStart.COMPILATION_UNIT;
import static com.github.javaparser.Providers.UTF8;
import static com.github.javaparser.Providers.provider;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.Range;
import com.github.javaparser.TokenRange;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.Node.Parsedness;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithType;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedValueDeclaration;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import de.fraunhofer.aisec.cpg.TranslationConfiguration;
import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend;
import de.fraunhofer.aisec.cpg.frontends.TranslationException;
import de.fraunhofer.aisec.cpg.graph.IncludeDeclaration;
import de.fraunhofer.aisec.cpg.graph.NamespaceDeclaration;
import de.fraunhofer.aisec.cpg.graph.NodeBuilder;
import de.fraunhofer.aisec.cpg.graph.Region;
import de.fraunhofer.aisec.cpg.graph.TranslationUnitDeclaration;
import de.fraunhofer.aisec.cpg.graph.Type.Origin;
import de.fraunhofer.aisec.cpg.graph.TypeManager;
import de.fraunhofer.aisec.cpg.helpers.Benchmark;
import de.fraunhofer.aisec.cpg.helpers.CommonPath;
import de.fraunhofer.aisec.cpg.helpers.Util;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.NonNull;

/** Main parser for ONE Java files. */
public class JavaLanguageFrontend extends LanguageFrontend {

  private CompilationUnit context;

  private ExpressionHandler expressionHandler = new ExpressionHandler(this);
  private StatementAnalyzer statementHandler = new StatementAnalyzer(this);
  private DeclarationHandler declarationHandler = new DeclarationHandler(this);

  private JavaSymbolSolver javaSymbolResolver;
  private HashSet<TypeSolver> internalTypeSolvers =
      new HashSet<>(); // we store a reference here to clean them up later

  public JavaLanguageFrontend(TranslationConfiguration config) {
    super(config, ".");

    CombinedTypeSolver typeResolver = new CombinedTypeSolver();
    internalTypeSolvers.add(typeResolver);
    ReflectionTypeSolver reflectionTypeSolver = new ReflectionTypeSolver();
    internalTypeSolvers.add(reflectionTypeSolver);
    typeResolver.add(reflectionTypeSolver);
    if (config != null) {
      File root = config.getTopLevel();
      if (root == null) {
        root = CommonPath.commonPath(config.getSourceFiles());
      }

      if (root == null) {
        log.warn("Could not determine source root for {}", config.getSourceFiles());
      } else {
        log.info("Source file root used for type solver: {}", root);
        JavaParserTypeSolver javaParserTypeSolver = new JavaParserTypeSolver(root);
        internalTypeSolvers.add(javaParserTypeSolver);
        typeResolver.add(javaParserTypeSolver);
      }
    }
    this.javaSymbolResolver = new JavaSymbolSolver(typeResolver);
  }

  @Override
  public TranslationUnitDeclaration parse(File file) throws TranslationException {
    TypeManager.getInstance().setLanguageFrontend(this);
    TranslationConfiguration c = this.config;

    // load in the file
    try (FileInputStream in = new FileInputStream(file)) {
      ParserConfiguration parserConfiguration = new ParserConfiguration();
      parserConfiguration.setSymbolResolver(this.javaSymbolResolver);
      JavaParser parser = new JavaParser(parserConfiguration);

      // parse the file
      Benchmark bench = new Benchmark(this.getClass(), "Parsing sourcefile");
      context = parse(Util.inputStreamToString(in), parser);
      bench.stop();

      bench = new Benchmark(this.getClass(), "Transform to CPG");
      context.setData(com.github.javaparser.ast.Node.SYMBOL_RESOLVER_KEY, this.javaSymbolResolver);

      // starting point is always a translation declaration
      TranslationUnitDeclaration fileDeclaration =
          NodeBuilder.newTranslationUnitDeclaration(file.toString(), context.toString());
      TranslationUnitDeclaration declaration = fileDeclaration;

      PackageDeclaration packDecl = context.getPackageDeclaration().orElse(null);
      NamespaceDeclaration namespaceDeclaration = null;
      if (packDecl != null) {
        namespaceDeclaration = NodeBuilder.newNamespaceDeclaration(packDecl.getName().asString());
        // Todo set region and code and push/pop scope
        scopeManager.enterScope(namespaceDeclaration);
        declaration.add(namespaceDeclaration);
        declaration = namespaceDeclaration;
      }

      for (TypeDeclaration<?> type : context.getTypes()) {
        declaration.add(getDeclarationHandler().handle(type));
      }

      for (ImportDeclaration anImport : context.getImports()) {
        IncludeDeclaration incl = NodeBuilder.newIncludeDeclaration(anImport.getNameAsString());
        declaration.add(incl);
      }

      if (packDecl != null) scopeManager.leaveScope(namespaceDeclaration);
      bench.stop();

      return fileDeclaration;
    } catch (IOException ex) {
      throw new TranslationException(ex);
    }
  }

  protected CompilationUnit parse(String fileContent, JavaParser parser)
      throws TranslationException {

    ByteArrayInputStream stream = new ByteArrayInputStream(fileContent.getBytes());
    ParseResult<CompilationUnit> result = parser.parse(COMPILATION_UNIT, provider(stream, UTF8));

    Optional<CompilationUnit> optional = result.getResult();
    if (optional.isEmpty()) {
      throw new TranslationException("JavaParser could not parse file");
    }

    if (optional.get().getParsed() == Parsedness.PARSED) {
      log.debug("Successfully parsed java file");
    } else {
      result
          .getProblems()
          .forEach(
              p -> {
                StringBuilder sb = new StringBuilder();
                sb.append(p.getMessage());
                if (p.getLocation().isPresent()
                    && p.getLocation().get().getBegin().getRange().isPresent()) {
                  sb.append(" ");
                  sb.append(p.getLocation().get().getBegin().getRange().get().begin.toString());
                }
                log.error(sb.toString());
              });
      log.error("Could not parse the file correctly! AST may be empty");
    }
    return optional.get();
  }

  @Override
  public <T> String getCodeFromRawNode(T astNode) {
    if (astNode instanceof Node) {
      Node node = (Node) astNode;
      Optional<TokenRange> optional = node.getTokenRange();
      if (optional.isPresent()) {
        return optional.get().toString();
      }
    }
    return null;
  }

  @Override
  @NonNull
  public <T> Region getRegionFromRawNode(T astNode) {
    if (astNode instanceof Node) {
      Node node = (Node) astNode;
      Optional<Range> optional = node.getRange();
      if (optional.isPresent()) {
        Range r = optional.get();
        return new Region(
            r.begin.line, r.begin.column, r.end.line, r.end.column + 1); // +1 for SARIF compliance
      }
    }
    return new Region();
  }

  public de.fraunhofer.aisec.cpg.graph.Type getTypeAsGoodAsPossible(
      NodeWithType nodeWithType, ResolvedValueDeclaration resolved) {
    try {
      return new de.fraunhofer.aisec.cpg.graph.Type(resolved.getType().describe());
    } catch (RuntimeException | NoClassDefFoundError ex) {
      return getTypeFromImportIfPossible(nodeWithType.getType());
    }
  }

  public String getQualifiedMethodNameAsGoodAsPossible(MethodCallExpr callExpr) {
    try {
      return callExpr.resolve().getQualifiedName();
    } catch (RuntimeException | NoClassDefFoundError ex) {
      Optional<Expression> scope = callExpr.getScope();
      if (scope.isPresent()) {
        Expression expression = scope.get();
        if (expression instanceof NameExpr) {
          // try to look for imports matching the name
          // i.e. a static call
          String fromImport = getQualifiedNameFromImports(callExpr.getNameAsString());
          if (fromImport != null) {
            return fromImport;
          }
        }
        if (scope.get().toString().equals("this")) {
          // this is not strictly true. This could also be a function of a superclass,
          // but is the best we can do for now.
          // if the superclass would be known, this would already be resolved by the Javaresolver
          return this.getScopeManager().getCurrentNamePrefix() + "." + callExpr.getNameAsString();
        } else {
          return scope.get().toString() + "." + callExpr.getNameAsString();
        }
      } else {
        // if the method is a static method of a resolveable class, the .resolve() would have
        // worked.
        // but, the following can still be false, if the superclass implements callExpr, but is not
        // available for analysis

        // check if this is a "specific" static import (not of the type 'import static x.y.Z.*')
        String fromImport = getQualifiedNameFromImports(callExpr.getNameAsString());
        if (fromImport != null) {
          return fromImport;
        }
        // this is not strictly true. This could also be a function of a superclass or from a
        // static asterisk import
        return this.getScopeManager().getCurrentNamePrefix() + "." + callExpr.getNameAsString();
      }
    }
  }

  public String recoverTypeFromUnsolvedException(Throwable ex) {
    if (ex.getCause() instanceof UnsolvedSymbolException
        || (ex.getCause() == null && ex instanceof UnsolvedSymbolException)) {
      String qualifier;
      if (ex.getCause() == null) {
        qualifier = ((UnsolvedSymbolException) ex).getName();
      } else {
        qualifier = ((UnsolvedSymbolException) ex.getCause()).getName();
      }
      // this comes from the Javaparser!
      if (qualifier.startsWith("We are unable to find the value declaration corresponding to")
          || qualifier.startsWith("Solving ")) {
        return null;
      }
      String fromImport = getQualifiedNameFromImports(qualifier);
      if (fromImport != null) {
        return fromImport;
      }
      return qualifier;
    }
    log.debug("Unable to resolve qualified name from exception");
    return null;
  }

  public String getQualifiedNameFromImports(String className) {
    if (context != null) {
      // see if we can make the qualifier more precise using the imports
      for (ImportDeclaration importDeclaration : context.getImports()) {
        if (importDeclaration.getName().asString().endsWith("." + className)) {
          return importDeclaration.getName().asString();
        }
      }
    }
    return null;
  }

  public de.fraunhofer.aisec.cpg.graph.Type getTypeAsGoodAsPossible(Type type) {
    try {
      return new de.fraunhofer.aisec.cpg.graph.Type(type.resolve().describe());
    } catch (RuntimeException | NoClassDefFoundError ex) {
      return getTypeFromImportIfPossible(type);
    }
  }

  public de.fraunhofer.aisec.cpg.graph.Type getReturnTypeAsGoodAsPossible(
      NodeWithType nodeWithType, ResolvedMethodDeclaration resolved) {
    try {
      return new de.fraunhofer.aisec.cpg.graph.Type(resolved.getReturnType().describe());
    } catch (RuntimeException | NoClassDefFoundError ex) {
      return getTypeFromImportIfPossible(nodeWithType.getType());
    }
  }

  private de.fraunhofer.aisec.cpg.graph.Type getTypeFromImportIfPossible(Type type) {
    Type searchType = type;
    while (searchType.isArrayType()) {
      searchType = searchType.getElementType();
    }
    // if this is not a ClassOrInterfaceType, just return
    if (!searchType.isClassOrInterfaceType() || context == null) {
      log.warn("Unable to resolve type for {}", type.asString());
      return new de.fraunhofer.aisec.cpg.graph.Type(type.asString(), Origin.GUESSED);
    }

    ClassOrInterfaceType clazz = searchType.asClassOrInterfaceType();

    if (clazz != null) {
      // try to look for imports matching the name
      for (ImportDeclaration importDeclaration : context.getImports()) {
        if (importDeclaration.getName().getIdentifier().endsWith(clazz.getName().getIdentifier())) {
          // TODO: handle type parameters
          return new de.fraunhofer.aisec.cpg.graph.Type(importDeclaration.getNameAsString());
        }
      }
      return new de.fraunhofer.aisec.cpg.graph.Type(clazz.getNameAsString(), Origin.GUESSED);
    }

    log.warn("Unable to resolve type for {}", type.asString());
    return new de.fraunhofer.aisec.cpg.graph.Type(type.asString(), Origin.GUESSED);
  }

  @Override
  public void cleanup() {
    JavaParserFacade.clearInstances();

    for (TypeSolver tr : internalTypeSolvers) {
      if (tr != null && tr.getParent() != null) {
        tr.setParent(null); // trying to help the garbagecollector a bit
      }
    }

    super.cleanup();
    context = null;
    expressionHandler = null;
    statementHandler = null;
    declarationHandler = null;
    javaSymbolResolver = null;
  }

  @Override
  public <S, T> void setComment(S s, T ctx) {
    if (ctx instanceof Node && s instanceof de.fraunhofer.aisec.cpg.graph.Node) {
      Node node = (Node) ctx;
      de.fraunhofer.aisec.cpg.graph.Node cpgNode = (de.fraunhofer.aisec.cpg.graph.Node) s;
      node.getComment().ifPresent(comment -> cpgNode.setComment(comment.getContent()));
      // TODO: handle orphanComments?
    }
  }

  public ExpressionHandler getExpressionHandler() {
    return expressionHandler;
  }

  public StatementAnalyzer getStatementHandler() {
    return statementHandler;
  }

  public DeclarationHandler getDeclarationHandler() {
    return declarationHandler;
  }

  public CompilationUnit getContext() {
    return context;
  }
}
