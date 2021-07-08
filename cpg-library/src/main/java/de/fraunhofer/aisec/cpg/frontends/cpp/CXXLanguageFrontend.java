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

import static de.fraunhofer.aisec.cpg.graph.NodeBuilder.newAnnotation;
import static de.fraunhofer.aisec.cpg.graph.NodeBuilder.newAnnotationMember;
import static de.fraunhofer.aisec.cpg.graph.NodeBuilder.newDeclaredReferenceExpression;
import static de.fraunhofer.aisec.cpg.graph.NodeBuilder.newLiteral;

import de.fraunhofer.aisec.cpg.TranslationConfiguration;
import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend;
import de.fraunhofer.aisec.cpg.frontends.TranslationException;
import de.fraunhofer.aisec.cpg.graph.Annotation;
import de.fraunhofer.aisec.cpg.graph.AnnotationMember;
import de.fraunhofer.aisec.cpg.graph.Node;
import de.fraunhofer.aisec.cpg.graph.TypeManager;
import de.fraunhofer.aisec.cpg.graph.declarations.Declaration;
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration;
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression;
import de.fraunhofer.aisec.cpg.graph.types.TypeParser;
import de.fraunhofer.aisec.cpg.graph.types.UnknownType;
import de.fraunhofer.aisec.cpg.helpers.Benchmark;
import de.fraunhofer.aisec.cpg.passes.scopes.ScopeManager;
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation;
import de.fraunhofer.aisec.cpg.sarif.Region;
import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.eclipse.cdt.core.dom.ast.IASTAttribute;
import org.eclipse.cdt.core.dom.ast.IASTAttributeOwner;
import org.eclipse.cdt.core.dom.ast.IASTComment;
import org.eclipse.cdt.core.dom.ast.IASTFileLocation;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTToken;
import org.eclipse.cdt.core.dom.ast.IASTTokenList;
import org.eclipse.cdt.core.dom.ast.IBinding;
import org.eclipse.cdt.core.dom.ast.gnu.cpp.GPPLanguage;
import org.eclipse.cdt.core.index.IIndexFileLocation;
import org.eclipse.cdt.core.model.ILanguage;
import org.eclipse.cdt.core.parser.DefaultLogService;
import org.eclipse.cdt.core.parser.FileContent;
import org.eclipse.cdt.core.parser.IncludeFileContentProvider;
import org.eclipse.cdt.core.parser.ScannerInfo;
import org.eclipse.cdt.internal.core.dom.parser.ASTNode;
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

  public static final List<String> CXX_EXTENSIONS = List.of(".c", ".cpp", ".cc");
  public static final List<String> CXX_HEADER_EXTENSIONS = List.of(".h", ".hpp");

  private static final Logger LOGGER = LoggerFactory.getLogger(CXXLanguageFrontend.class);
  private final IncludeFileContentProvider includeFileContentProvider =
      new InternalFileContentProvider() {
        @Nullable
        private InternalFileContent getContentUncached(String path) {
          if (!getInclusionExists(path)) {
            LOGGER.debug("Include file not found: {}", path);
            return null;
          }

          // check, if the file is on the blacklist
          if (absoluteOrRelativePathIsInList(path, config.includeBlacklist)) {
            LOGGER.debug("Blacklisting include file: {}", path);
            return null;
          }

          // check, if the white-list exists at all
          if (hasIncludeWhitelist()
              &&
              // and ignore the file if it is not on the whitelist
              !absoluteOrRelativePathIsInList(path, config.includeWhitelist)) {
            LOGGER.debug("Include file {} not on the whitelist. Ignoring.", path);
            return null;
          }

          LOGGER.debug("Loading include file {}", path);
          FileContent content = FileContent.createForExternalFileLocation(path);
          return (InternalFileContent) content;
        }

        private boolean hasIncludeWhitelist() {
          return config.includeWhitelist != null && !config.includeWhitelist.isEmpty();
        }

        /**
         * This utility function checks, if the specified path is in the included list, either as an
         * absolute path or as a path relative to the translation configurations top level or
         * include paths
         *
         * @param path the absolute path to look for
         * @param list the list of paths to look for, either relative or absolute
         * @return true, if the path is in the list, false otherwise
         */
        private boolean absoluteOrRelativePathIsInList(String path, List<String> list) {
          // path cannot be in the list if its empty or null
          if (list == null || list.isEmpty()) {
            return false;
          }

          // check, if the absolute header path is in the list
          if (list.contains(path)) {
            return true;
          }

          // check for relative path based on the top level and all include paths
          List<Path> includeLocations = new ArrayList<>();
          File topLevel = config.getTopLevel();

          if (topLevel != null) {
            includeLocations.add(topLevel.toPath().toAbsolutePath());
          }

          includeLocations.addAll(
              Arrays.stream(config.includePaths)
                  .map(s -> Path.of(s).toAbsolutePath())
                  .collect(Collectors.toList()));

          for (Path includeLocation : includeLocations) {
            // try to resolve path relatively
            Path includeFile = Path.of(path);
            Path relative = includeLocation.relativize(includeFile);

            if (list.contains(relative.toString())) {
              return true;
            }
          }

          return false;
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
  private final DeclarationHandler declarationHandler = new DeclarationHandler(this);
  private final DeclaratorHandler declaratorHandler = new DeclaratorHandler(this);
  private final ExpressionHandler expressionHandler = new ExpressionHandler(this);
  private final InitializerHandler initializerHandler = new InitializerHandler(this);
  private final ParameterDeclarationHandler parameterDeclarationHandler =
      new ParameterDeclarationHandler(this);
  private final StatementHandler statementHandler = new StatementHandler(this);
  private final HashMap<IBinding, Declaration> cachedDeclarations = new HashMap<>();
  private final HashMap<IBinding, List<Expression>> cachedExpressions = new HashMap<>();
  private final HashMap<Integer, String> comments = new HashMap<>();

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

    log.trace(
        "{}{} -> {}",
        s,
        node.getClass().getSimpleName(),
        node.getRawSignature().replace('\n', '\\').replace('\t', ' '));

    for (IASTNode iastNode : children) {
      explore(iastNode, indent + 2);
    }
  }

  @Override
  public TranslationUnitDeclaration parse(File file) throws TranslationException {
    TypeManager.getInstance().setLanguageFrontend(this);
    FileContent content = FileContent.createForExternalFileLocation(file.getAbsolutePath());

    // include paths
    List<String> includePaths = new ArrayList<>();
    if (config.getTopLevel() != null) {
      includePaths.add(config.getTopLevel().toPath().toAbsolutePath().toString());
    }
    includePaths.addAll(Arrays.asList(config.includePaths));

    ScannerInfo scannerInfo =
        new ScannerInfo(config.symbols, includePaths.toArray(new String[] {}));

    DefaultLogService log = new DefaultLogService();

    int opts = ILanguage.OPTION_PARSE_INACTIVE_CODE; // | ILanguage.OPTION_ADD_COMMENTS;

    try {
      Benchmark bench = new Benchmark(this.getClass(), "Parsing sourcefile");
      CPPASTTranslationUnit translationUnit =
          (CPPASTTranslationUnit)
              GPPLanguage.getDefault()
                  .getASTTranslationUnit(
                      content, scannerInfo, includeFileContentProvider, null, opts, log);

      var length = translationUnit.getLength();
      LOGGER.info("Parsed {} bytes corresponding roughly to {} LoC", length, length / 50);
      bench.stop();

      bench = new Benchmark(this.getClass(), "Transform to CPG");

      if (config.debugParser) {
        explore(translationUnit, 0);
      }

      for (IASTComment c : translationUnit.getComments()) {
        if (c.getFileLocation() == null) {
          LOGGER.warn("Found comment with null location in {}", translationUnit.getFilePath());
          continue;
        }
        comments.put(c.getFileLocation().getStartingLineNumber(), c.getRawSignature());
      }

      TranslationUnitDeclaration translationUnitDeclaration =
          declarationHandler.handleTranslationUnit(translationUnit);
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

  @Nullable
  @Override
  public <T> PhysicalLocation getLocationFromRawNode(T astNode) {
    if (astNode instanceof ASTNode) {
      ASTNode node = (ASTNode) astNode;
      IASTFileLocation fLocation = node.getFileLocation();

      if (fLocation != null) {
        /* Yes, seriously. getRawSignature() is CPU- and heap-costly, because it does an arraycopy. If parent is the whole TranslationUnit and we are doing this repeatedly, we will end up with OOM and waste time.
         * We thus do a shortcut and directly access the field containing the source code of a node as a CharArray.
         * This may break in future versions of CDT parser, when fields are renamed (which is unlikely). In this case, we will go the standard route.
         * Note, the only reason we are doing this is to compute the start and end columns of the current node.
         */
        AbstractCharArray translationUnitRawSignature = new CharArray("");
        try {
          Field fLoc = getField(fLocation.getClass(), "fLocationCtx");
          fLoc.trySetAccessible();
          Object locCtx = fLoc.get(fLocation);

          Field fSource = getField(locCtx.getClass(), "fSource");
          fSource.trySetAccessible();
          translationUnitRawSignature = (AbstractCharArray) fSource.get(locCtx);
        } catch (ReflectiveOperationException | ClassCastException | NullPointerException e) {
          LOGGER.warn(
              "Reflective retrieval of AST node source failed. Cannot reliably determine content of the file that contains the node");
          return null;
        }

        // Get start column by stepping backwards from begin of node to first occurrence of '\n'
        int startColumn = 1;
        for (int i = node.getFileLocation().getNodeOffset() - 1; i > 1; i--) {
          if (i >= translationUnitRawSignature.getLength()) {
            // Fail gracefully, so that we can at least find out why this fails
            LOGGER.warn(
                "Requested index {} exceeds length of translation unit code ({})",
                i,
                translationUnitRawSignature.getLength());

            return null;
          }

          if (translationUnitRawSignature.get(i) == '\n') {
            break;
          }
          startColumn++;
        }
        int endColumn =
            getEndColumnIndex(
                translationUnitRawSignature,
                node.getFileLocation().getNodeOffset() + node.getLength());

        Region region =
            new Region(
                fLocation.getStartingLineNumber(),
                startColumn,
                fLocation.getEndingLineNumber(),
                endColumn);

        return new PhysicalLocation(Path.of(node.getContainingFilename()).toUri(), region);
      }
    }

    return null;
  }

  /**
   * Processes C++ attributes into {@link Annotation} nodes.
   *
   * @param node the node to process
   * @param owner the AST node which holds the attribute
   */
  public void processAttributes(@NonNull Node node, @NonNull IASTAttributeOwner owner) {
    if (this.config.processAnnotations) {
      // set attributes
      node.addAnnotations(handleAttributes(owner));
    }
  }

  private List<Annotation> handleAttributes(IASTAttributeOwner owner) {
    List<Annotation> list = new ArrayList<>();

    for (IASTAttribute attribute : owner.getAttributes()) {
      Annotation annotation =
          newAnnotation(new String(attribute.getName()), attribute.getRawSignature());

      // go over the parameters
      if (attribute.getArgumentClause() instanceof IASTTokenList) {
        List<AnnotationMember> members =
            handleTokenList((IASTTokenList) attribute.getArgumentClause());

        annotation.setMembers(members);
      }

      list.add(annotation);
    }

    return list;
  }

  private List<AnnotationMember> handleTokenList(IASTTokenList tokenList) {
    List<AnnotationMember> list = new ArrayList<>();

    for (IASTToken token : tokenList.getTokens()) {
      // skip commas and such
      if (token.getTokenType() == 6) {
        continue;
      }

      list.add(handleToken(token));
    }

    return list;
  }

  private AnnotationMember handleToken(IASTToken token) {
    String code = new String(token.getTokenCharImage());

    Expression expression;
    switch (token.getTokenType()) {
      case 1:
        // a variable
        expression = newDeclaredReferenceExpression(code, UnknownType.getUnknownType(), code);
        break;
      case 2:
        // an integer
        expression = newLiteral(Integer.parseInt(code), TypeParser.createFrom("int", true), code);
        break;
      case 130:
        // a string
        expression =
            newLiteral(
                code.length() >= 2 ? code.substring(1, code.length() - 1) : "",
                TypeParser.createFrom("const char*", false),
                code);
        break;
      default:
        expression = newLiteral(code, TypeParser.createFrom("const char*", false), code);
    }

    return newAnnotationMember("", expression, code);
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

  @Nullable
  public Declaration cacheDeclaration(IBinding binding, Declaration declaration) {
    return cachedDeclarations.put(binding, declaration);
  }

  public Declaration getCachedDeclaration(IBinding binding) {
    return cachedDeclarations.get(binding);
  }

  public Map<IBinding, Declaration> getCachedDeclarations() {
    return cachedDeclarations;
  }

  @Override
  public void cleanup() {
    super.cleanup();
  }

  @Override
  public <S, T> void setComment(S s, T ctx) {
    if (ctx instanceof ASTNode && s instanceof de.fraunhofer.aisec.cpg.graph.Node) {
      de.fraunhofer.aisec.cpg.graph.Node cpgNode = (de.fraunhofer.aisec.cpg.graph.Node) s;

      if (comments.containsKey(
          cpgNode.getLocation().getRegion().getEndLine())) { // only exact match for now
        cpgNode.setComment(comments.get(cpgNode.getLocation().getRegion().getEndLine()));
      }
      // TODO: handle orphanComments? i.e. comments which do not correspond to one line
      // todo: what to do with comments which are in a line which contains multiple statements?
    }
  }

  public DeclarationHandler getDeclarationHandler() {
    return declarationHandler;
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
