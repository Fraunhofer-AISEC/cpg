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

import de.fraunhofer.aisec.cpg.TranslationConfiguration;
import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend;
import de.fraunhofer.aisec.cpg.frontends.TranslationException;
import de.fraunhofer.aisec.cpg.graph.Declaration;
import de.fraunhofer.aisec.cpg.graph.DeclaredReferenceExpression;
import de.fraunhofer.aisec.cpg.graph.Expression;
import de.fraunhofer.aisec.cpg.graph.Region;
import de.fraunhofer.aisec.cpg.graph.TranslationUnitDeclaration;
import de.fraunhofer.aisec.cpg.graph.Type;
import de.fraunhofer.aisec.cpg.graph.TypeManager;
import de.fraunhofer.aisec.cpg.graph.ValueDeclaration;
import de.fraunhofer.aisec.cpg.helpers.Benchmark;
import de.fraunhofer.aisec.cpg.passes.scopes.ScopeManager;
import java.io.File;
import java.lang.reflect.Field;
import java.util.HashMap;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.eclipse.cdt.core.dom.ast.IASTComment;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTFileLocation;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.IBinding;
import org.eclipse.cdt.core.dom.ast.gnu.cpp.GPPLanguage;
import org.eclipse.cdt.core.index.IIndexFileLocation;
import org.eclipse.cdt.core.model.ILanguage;
import org.eclipse.cdt.core.parser.DefaultLogService;
import org.eclipse.cdt.core.parser.FileContent;
import org.eclipse.cdt.core.parser.IncludeFileContentProvider;
import org.eclipse.cdt.core.parser.ScannerInfo;
import org.eclipse.cdt.internal.core.dom.parser.ASTNode;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTIdExpression;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTTranslationUnit;
import org.eclipse.cdt.internal.core.parser.IMacroDictionary;
import org.eclipse.cdt.internal.core.parser.scanner.AbstractCharArray;
import org.eclipse.cdt.internal.core.parser.scanner.CharArray;
import org.eclipse.cdt.internal.core.parser.scanner.InternalFileContent;
import org.eclipse.cdt.internal.core.parser.scanner.InternalFileContentProvider;
import org.eclipse.core.runtime.CoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The language frontend for translating CXX languages into the graph. It uses Eclipse CDT to parse
 * the actual source code into an AST.
 *
 * <p>Frontend for ONE CXX File
 */
public class CXXLanguageFrontend extends LanguageFrontend {

  public static final Type LONG_TYPE = Type.createFrom("long");
  public static final Type TYPE_UNSIGNED_LONG_LONG = Type.createFrom("unsigned long long");
  public static final Type INT_TYPE = Type.createFrom("int");
  public static final Type LONG_LONG_TYPE = Type.createFrom("long long");
  public static final Type TYPE_UNSIGNED_LONG = Type.createFrom("unsigned long");

  private static final Logger LOGGER = LoggerFactory.getLogger(CXXLanguageFrontend.class);
  private static final IncludeFileContentProvider INCLUDE_FILE_PROVIDER =
      new InternalFileContentProvider() {
        @Nullable
        private InternalFileContent getContentUncached(String path) {
          if (!getInclusionExists(path)) {
            LOGGER.debug("Include file not found: {}", path);
            return null;
          }
          LOGGER.debug("Loading include file {}", path);
          FileContent content = FileContent.createForExternalFileLocation(path);
          return (InternalFileContent) content;
        }

        @Nullable
        @Override
        public InternalFileContent getContentForInclusion(
            String path, IMacroDictionary macroDictionary) {
          return getContentUncached(path);
        }

        @Nullable
        @Override
        public InternalFileContent getContentForInclusion(IIndexFileLocation ifl, String astPath) {
          return getContentUncached(astPath);
        }
      };
  private DeclarationHandler declarationHandler = new DeclarationHandler(this);
  private DeclarationListHandler declarationListHandler = new DeclarationListHandler(this);
  private DeclaratorHandler declaratorHandler = new DeclaratorHandler(this);
  private ExpressionHandler expressionHandler = new ExpressionHandler(this);
  private InitializerHandler initializerHandler = new InitializerHandler(this);
  private ParameterDeclarationHandler parameterDeclarationHandler =
      new ParameterDeclarationHandler(this);
  private StatementHandler statementHandler = new StatementHandler(this);
  private HashMap<IBinding, Declaration> cachedDeclarations = new HashMap<>();
  private HashMap<Integer, String> comments = new HashMap<>();

  public CXXLanguageFrontend(@NonNull TranslationConfiguration config, ScopeManager scopeManager) {
    super(config, scopeManager, "::");
  }

  /**
   * Searches in posPrefix to the left until first occurrence of line break and returns the number
   * of characters.
   *
   * <p>This corresponds to the column number of "end" within "posPrefix".
   *
   * @param posPrefix - the positional prefix, which is the string before the column and contains
   *     the column defining newline.
   */
  private static int getEndColumnIndex(AbstractCharArray posPrefix, int end) {
    int column = 1;

    // In case the current element goes until EOF, we need to back up "end" by one.
    try {
      if ((end - 1) >= posPrefix.getLength() || posPrefix.get(end - 1) == '\n') {
        end = Math.min(end - 1, posPrefix.getLength() - 1);
      }
    } catch (ArrayIndexOutOfBoundsException e) {
      log.error("could not update end ", e);
    }
    for (int i = end - 1; i > 1; i--) {
      if (posPrefix.get(i) == '\n') {
        break;
      }
      column++;
    }

    return column;
  }

  private static void explore(IASTNode node, int indent) {
    IASTNode[] children = node.getChildren();

    StringBuilder s = new StringBuilder();

    s.append(" ".repeat(indent));

    log.debug(
        "{}{} -> {}",
        s,
        node.getClass().getSimpleName(),
        node.getRawSignature().replaceAll("\n", " \\ ").replaceAll("\\s+", "  "));

    for (IASTNode iastNode : children) {
      explore(iastNode, indent + 2);
    }
  }

  @Override
  public TranslationUnitDeclaration parse(File file) throws TranslationException {
    TypeManager.getInstance().setLanguageFrontend(this);
    FileContent content = FileContent.createForExternalFileLocation(file.getAbsolutePath());

    // include paths
    String[] includePaths = config.includePaths;

    ScannerInfo scannerInfo = new ScannerInfo(config.symbols, includePaths);

    DefaultLogService log = new DefaultLogService();

    IncludeFileContentProvider includeProvider;
    if (config.loadIncludes) {
      includeProvider = INCLUDE_FILE_PROVIDER;
    } else {
      includeProvider = IncludeFileContentProvider.getEmptyFilesProvider();
    }

    int opts = ILanguage.OPTION_PARSE_INACTIVE_CODE; // | ILanguage.OPTION_ADD_COMMENTS;

    try {
      Benchmark bench = new Benchmark(this.getClass(), "Parsing sourcefile");
      IASTTranslationUnit translationUnit =
          GPPLanguage.getDefault()
              .getASTTranslationUnit(content, scannerInfo, includeProvider, null, opts, log);
      bench.stop();

      bench = new Benchmark(this.getClass(), "Transform to CPG");

      if (config.debugParser) {
        explore(translationUnit, 0);
      }

      for (IASTComment c : translationUnit.getComments()) {
        comments.put(c.getFileLocation().getStartingLineNumber(), c.getRawSignature());
      }

      TranslationUnitDeclaration translationUnitDeclaration =
          declarationHandler.handleTranslationUnit((CPPASTTranslationUnit) translationUnit);
      bench.stop();
      return translationUnitDeclaration;
    } catch (CoreException ex) {
      throw new TranslationException(ex);
    }
  }

  @Nullable
  @Override
  public <T> String getCodeFromRawNode(T astNode) {
    if (astNode instanceof ASTNode) {
      ASTNode node = (ASTNode) astNode;
      return node.getRawSignature();
    }
    return null;
  }

  @Override
  @NonNull
  @SuppressWarnings("ConstantConditions")
  public <T> Region getRegionFromRawNode(T astNode) {
    if (astNode instanceof ASTNode) {
      ASTNode node = (ASTNode) astNode;
      IASTFileLocation fLocation = node.getFileLocation();
      ASTNode parent = (ASTNode) node.getParent();

      if (fLocation != null) {
        /* Yes, seriously. getRawSignature() is CPU- and heap-costly, because it does an arraycopy. If parent is the whole TranslationUnit and we are doing this repeatedly, we will end up with OOM and waste time.
         * We thus do a shortcut and directly access the field containing the source code of a node as a CharArray.
         * This may break in future versions of CDT parser, when fields are renamed (which is unlikely). In this case, we will go the standard route.
         * Note, the only reason we are doing this is to compute the start and end columns of the current node.
         */
        AbstractCharArray parentRawSig = new CharArray("");
        try {
          Field fLoc = getField(fLocation.getClass(), "fLocationCtx");
          fLoc.setAccessible(true);
          Object locCtx = fLoc.get(fLocation);

          Field fSource = getField(locCtx.getClass(), "fSource");
          fSource.setAccessible(true);
          parentRawSig = (AbstractCharArray) fSource.get(locCtx);
        } catch (ReflectiveOperationException | ClassCastException | NullPointerException e) {
          LOGGER.warn(
              "Reflective retrieval of AST node source failed. Must go the official but costly route via getRawSignature(). Watch your heap!");
          while (parent.getParent() != null) {
            parent = (ASTNode) parent.getParent();
          }
          parentRawSig = new CharArray(parent.getRawSignature());
        }

        // Get start column by stepping backwards from begin of node to first occurrence of '\n'
        int startColumn = 1;
        for (int i = node.getFileLocation().getNodeOffset() - 1; i > 1; i--) {
          if (parentRawSig.get(i) == '\n') {
            break;
          }
          startColumn++;
        }
        int endColumn =
            getEndColumnIndex(
                parentRawSig, node.getFileLocation().getNodeOffset() + node.getLength());

        return new Region(
            fLocation.getStartingLineNumber(),
            startColumn,
            fLocation.getEndingLineNumber(),
            endColumn);
      }
    }
    return new Region();
  }

  private Field getField(Class<?> type, String fieldName) throws NoSuchFieldException {
    try {
      return type.getDeclaredField(fieldName);
    } catch (NoSuchFieldException e) {
      if (type.getSuperclass() != null) {
        return getField(type.getSuperclass(), fieldName);
      }
      throw e;
    }
  }

  public void expressionRefersToDeclaration(Expression expression, IASTExpression iastExpression) {
    if (expression instanceof DeclaredReferenceExpression
        && iastExpression instanceof CPPASTIdExpression) {
      IBinding binding = ((CPPASTIdExpression) iastExpression).getName().resolveBinding();
      Declaration declaration = cachedDeclarations.get(binding);
      //      String name = ((CPPASTIdExpression) iastExpression).getName().toString();
      //      Declaration declaration = currentDeclarations.get(name);

      if (declaration != null) {
        LOGGER.debug("Connecting {} to {}", expression, declaration);

        ((DeclaredReferenceExpression) expression).setRefersTo((ValueDeclaration) declaration);
      }
    } else {
      if (expression == null) {
        LOGGER.warn(
            "Cannot connect, from is NULL, to is {}", iastExpression.getClass().toGenericString());
      } else if (iastExpression == null) {
        LOGGER.warn(
            "Cannot connect, to is NULL, from is {}", expression.getClass().toGenericString());
      } else {
        LOGGER.debug("Cannot connect {} to {}", expression.getClass(), iastExpression.getClass());
      }
    }
  }

  @Nullable
  public Declaration cacheDeclaration(IBinding binding, Declaration declaration) {
    return cachedDeclarations.put(binding, declaration);
  }

  public Declaration getCachedDeclaration(IBinding binding) {
    return cachedDeclarations.get(binding);
  }

  @Override
  public void cleanup() {
    super.cleanup();
  }

  @Override
  public <S, T> void setComment(S s, T ctx) {
    if (ctx instanceof ASTNode && s instanceof de.fraunhofer.aisec.cpg.graph.Node) {
      de.fraunhofer.aisec.cpg.graph.Node cpgNode = (de.fraunhofer.aisec.cpg.graph.Node) s;

      if (comments.containsKey(cpgNode.getRegion().getEndLine())) { // only exact match for now
        cpgNode.setComment(comments.get(cpgNode.getRegion().getEndLine()));
      }
      // TODO: handle orphanComments? i.e. comments which do not correspond to one line
      // todo: what to do with comments which are in a line which contains multiple statements?
    }
  }

  public DeclarationHandler getDeclarationHandler() {
    return declarationHandler;
  }

  public DeclarationListHandler getDeclarationListHandler() {
    return declarationListHandler;
  }

  public DeclaratorHandler getDeclaratorHandler() {
    return declaratorHandler;
  }

  public ExpressionHandler getExpressionHandler() {
    return expressionHandler;
  }

  public InitializerHandler getInitializerHandler() {
    return initializerHandler;
  }

  public ParameterDeclarationHandler getParameterDeclarationHandler() {
    return parameterDeclarationHandler;
  }

  public StatementHandler getStatementHandler() {
    return statementHandler;
  }
}
