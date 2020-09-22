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
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import de.fraunhofer.aisec.cpg.TranslationConfiguration;
import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend;
import de.fraunhofer.aisec.cpg.frontends.TranslationException;
import de.fraunhofer.aisec.cpg.graph.IncludeDeclaration;
import de.fraunhofer.aisec.cpg.graph.NamespaceDeclaration;
import de.fraunhofer.aisec.cpg.graph.NodeBuilder;
import de.fraunhofer.aisec.cpg.graph.TranslationUnitDeclaration;
import de.fraunhofer.aisec.cpg.graph.TypeManager;
import de.fraunhofer.aisec.cpg.graph.type.TypeParser;
import de.fraunhofer.aisec.cpg.helpers.Benchmark;
import de.fraunhofer.aisec.cpg.helpers.CommonPath;
import de.fraunhofer.aisec.cpg.passes.scopes.Scope;
import de.fraunhofer.aisec.cpg.passes.scopes.ScopeManager;
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation;
import de.fraunhofer.aisec.cpg.sarif.Region;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/** Main parser for ONE Java files. */
public class JavaLanguageFrontend extends LanguageFrontend {

  private CompilationUnit context;

  private ExpressionHandler expressionHandler = new ExpressionHandler(this);
  private StatementAnalyzer statementHandler = new StatementAnalyzer(this);
  private DeclarationHandler declarationHandler = new DeclarationHandler(this);

  private JavaSymbolSolver javaSymbolResolver;
  private CombinedTypeSolver internalTypeSolver = new CombinedTypeSolver();

  public JavaLanguageFrontend(@NonNull TranslationConfiguration config, ScopeManager scopeManager) {
    super(config, scopeManager, ".");

    ReflectionTypeSolver reflectionTypeSolver = new ReflectionTypeSolver();
    internalTypeSolver.add(reflectionTypeSolver);

    File root = config.getTopLevel();
    if (root == null) {
      root = CommonPath.commonPath(config.getSourceLocations());
    }

    if (root == null) {
      log.warn("Could not determine source root for {}", config.getSourceLocations());
    } else {
      log.info("Source file root used for type solver: {}", root);
      JavaParserTypeSolver javaParserTypeSolver = new JavaParserTypeSolver(root);
      internalTypeSolver.add(javaParserTypeSolver);
    }
    this.javaSymbolResolver = new JavaSymbolSolver(internalTypeSolver);
  }

  @Override
  public TranslationUnitDeclaration parse(File file) throws TranslationException {
    TypeManager.getInstance().setLanguageFrontend(this);

    // load in the file
    try {
      ParserConfiguration parserConfiguration = new ParserConfiguration();
      parserConfiguration.setSymbolResolver(this.javaSymbolResolver);
      JavaParser parser = new JavaParser(parserConfiguration);

      // parse the file
      Benchmark bench = new Benchmark(this.getClass(), "Parsing source file");
      context = parse(file, parser);
      bench.stop();

      bench = new Benchmark(this.getClass(), "Transform to CPG");
      context.setData(com.github.javaparser.ast.Node.SYMBOL_RESOLVER_KEY, this.javaSymbolResolver);

      // starting point is always a translation declaration
      TranslationUnitDeclaration fileDeclaration =
          NodeBuilder.newTranslationUnitDeclaration(file.toString(), context.toString());
      setCurrentTU(fileDeclaration);

      scopeManager.resetToGlobal(fileDeclaration);

      PackageDeclaration packDecl = context.getPackageDeclaration().orElse(null);
      NamespaceDeclaration namespaceDeclaration = null;
      if (packDecl != null) {
        namespaceDeclaration = NodeBuilder.newNamespaceDeclaration(packDecl.getName().asString());
        // Todo set region and code and push/pop scope

        scopeManager.addDeclaration(namespaceDeclaration);

        scopeManager.enterScope(namespaceDeclaration);
      }

      for (TypeDeclaration<?> type : context.getTypes()) {
        // handle each type. all declaration in this type will be added by the scope manager along
        // the way
        getDeclarationHandler().handle(type);
      }

      for (ImportDeclaration anImport : context.getImports()) {
        IncludeDeclaration incl = NodeBuilder.newIncludeDeclaration(anImport.getNameAsString());
        scopeManager.addDeclaration(incl);
      }

      if (packDecl != null) {
        scopeManager.leaveScope(namespaceDeclaration);
      }

      bench.stop();

      return fileDeclaration;
    } catch (IOException ex) {
      throw new TranslationException(ex);
    }
  }

  protected CompilationUnit parse(File file, JavaParser parser)
      throws TranslationException, FileNotFoundException {

    ParseResult<CompilationUnit> result = parser.parse(file);

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
      log.error("Could not parse the file {} correctly! AST may be empty", file);
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
    return astNode.toString();
  }

  @Override
  @Nullable
  public <T> PhysicalLocation getLocationFromRawNode(T astNode) {
    if (astNode instanceof Node) {
      Node node = (Node) astNode;

      // find compilation unit of node
      CompilationUnit cu = node.findCompilationUnit().orElse(null);
      if (cu == null) {
        return null;
      }

      // retrieve storage
      CompilationUnit.Storage storage = cu.getStorage().orElse(null);
      if (storage == null) {
        return null;
      }

      Optional<Range> optional = node.getRange();
      if (optional.isPresent()) {
        Range r = optional.get();

        Region region =
            new Region(
                r.begin.line,
                r.begin.column,
                r.end.line,
                r.end.column + 1); // +1 for SARIF compliance

        return new PhysicalLocation(storage.getPath().toUri(), region);
      }
    }

    return null;
  }

  public de.fraunhofer.aisec.cpg.graph.type.Type getTypeAsGoodAsPossible(
      NodeWithType nodeWithType, ResolvedValueDeclaration resolved) {
    try {
      return TypeParser.createFrom(resolved.getType().describe(), true);
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

  @Nullable
  public String recoverTypeFromUnsolvedException(Throwable ex) {
    if (ex instanceof UnsolvedSymbolException
        || (ex.getCause() != null && ex.getCause() instanceof UnsolvedSymbolException)) {
      String qualifier;
      if (ex instanceof UnsolvedSymbolException) {
        qualifier = ((UnsolvedSymbolException) ex).getName();
      } else {
        qualifier = ((UnsolvedSymbolException) ex.getCause()).getName();
      }
      // this comes from the Javaparser!
      if (qualifier.startsWith("We are unable to find") || qualifier.startsWith("Solving ")) {
        return null;
      }
      String fromImport = getQualifiedNameFromImports(qualifier);
      if (fromImport != null) {
        return fromImport;
      }
      return getFQNInCurrentPackage(qualifier);
    }
    log.debug("Unable to resolve qualified name from exception");
    return null;
  }

  @Nullable
  public String getQualifiedNameFromImports(String className) {
    if (context != null && className != null) {
      List<String> potentialClassNames = new ArrayList<>();
      String prefix = "";
      for (String s : className.split("\\.")) {
        potentialClassNames.add(prefix + s);
        prefix = prefix + s + ".";
      }
      // see if we can make the qualifier more precise using the imports
      for (ImportDeclaration importDeclaration : context.getImports()) {
        for (String cn : potentialClassNames) {
          if (importDeclaration.getName().asString().endsWith("." + cn)) {
            prefix = importDeclaration.getName().asString();
            return prefix.substring(0, prefix.lastIndexOf(cn)) + className;
          }
        }
      }
    }
    return null;
  }

  public de.fraunhofer.aisec.cpg.graph.type.Type getTypeAsGoodAsPossible(Type type) {
    try {
      return TypeParser.createFrom(type.resolve().describe(), true);
    } catch (RuntimeException | NoClassDefFoundError ex) {
      return getTypeFromImportIfPossible(type);
    }
  }

  public de.fraunhofer.aisec.cpg.graph.type.Type getReturnTypeAsGoodAsPossible(
      NodeWithType nodeWithType, ResolvedMethodDeclaration resolved) {
    try {
      return TypeParser.createFrom(resolved.getReturnType().describe(), true);
    } catch (RuntimeException | NoClassDefFoundError ex) {
      return getTypeFromImportIfPossible(nodeWithType.getType());
    }
  }

  /**
   * Returns the FQN of the given parameter assuming that is declared somewhere in the same package.
   * Names declared in a package are automatically imported.
   *
   * @param simpleName
   * @return
   */
  private String getFQNInCurrentPackage(String simpleName) {
    Scope theScope =
        getScopeManager()
            .getFirstScopeThat(scope -> scope.getAstNode() instanceof NamespaceDeclaration);
    // If scope is null we are in a default package
    if (theScope == null) {
      return simpleName;
    }
    return theScope.getScopedName() + getNamespaceDelimiter() + simpleName;
  }

  private de.fraunhofer.aisec.cpg.graph.type.Type getTypeFromImportIfPossible(Type type) {
    Type searchType = type;
    while (searchType.isArrayType()) {
      searchType = searchType.getElementType();
    }
    // if this is not a ClassOrInterfaceType, just return
    if (!searchType.isClassOrInterfaceType() || context == null) {
      log.warn("Unable to resolve type for {}", type.asString());
      de.fraunhofer.aisec.cpg.graph.type.Type returnType =
          TypeParser.createFrom(type.asString(), true);
      returnType.setTypeOrigin(de.fraunhofer.aisec.cpg.graph.type.Type.Origin.GUESSED);
      return returnType;
    }

    ClassOrInterfaceType clazz = searchType.asClassOrInterfaceType();

    if (clazz != null) {

      // try to look for imports matching the name
      for (ImportDeclaration importDeclaration : context.getImports()) {
        if (importDeclaration.getName().getIdentifier().endsWith(clazz.getName().getIdentifier())) {
          // TODO: handle type parameters
          return TypeParser.createFrom(importDeclaration.getNameAsString(), true);
        }
      }
      de.fraunhofer.aisec.cpg.graph.type.Type returnType =
          TypeParser.createFrom(clazz.asString(), true);
      returnType.setTypeOrigin(de.fraunhofer.aisec.cpg.graph.type.Type.Origin.GUESSED);
      return returnType;
    }

    log.warn("Unable to resolve type for {}", type.asString());
    de.fraunhofer.aisec.cpg.graph.type.Type returnType =
        TypeParser.createFrom(type.asString(), true);
    returnType.setTypeOrigin(de.fraunhofer.aisec.cpg.graph.type.Type.Origin.GUESSED);
    return returnType;
  }

  @Override
  public void cleanup() {
    JavaParserFacade.clearInstances();

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

  public CombinedTypeSolver getNativeTypeResolver() {
    return this.internalTypeSolver;
  }
}
