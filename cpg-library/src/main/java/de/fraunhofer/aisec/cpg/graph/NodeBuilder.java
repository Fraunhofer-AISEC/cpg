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
package de.fraunhofer.aisec.cpg.graph;

import de.fraunhofer.aisec.cpg.graph.declarations.*;
import de.fraunhofer.aisec.cpg.graph.statements.*;
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*;
import de.fraunhofer.aisec.cpg.graph.types.Type;
import de.fraunhofer.aisec.cpg.graph.types.TypeParser;
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation;
import java.util.ArrayList;
import java.util.List;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Builder for construction code property graph nodes. */
public class NodeBuilder {

  private static final Logger LOGGER = LoggerFactory.getLogger(NodeBuilder.class);

  private NodeBuilder() {
    // nothing to do
  }

  public static UsingDirective newUsingDirective(String code, String qualifiedName) {
    UsingDirective using = new UsingDirective();
    using.setQualifiedName(qualifiedName);
    using.setCode(code);

    log(using);

    return using;
  }

  public static CallExpression newCallExpression(
      String name, String fqn, String code, boolean template) {
    CallExpression node = new CallExpression();
    node.setName(name);
    node.setCode(code);
    node.setFqn(fqn);
    node.setTemplate(template);
    log(node);

    return node;
  }

  public static StaticCallExpression newStaticCallExpression(
      String name, String fqn, String code, String targetRecord) {
    StaticCallExpression node = new StaticCallExpression();
    node.setName(name);
    node.setCode(code);
    node.setFqn(fqn);
    node.setTargetRecord(targetRecord);

    log(node);

    return node;
  }

  public static CastExpression newCastExpression(String code) {
    CastExpression node = new CastExpression();

    node.setCode(code);

    log(node);

    return node;
  }

  public static TypeIdExpression newTypeIdExpression(
      String operatorCode, Type type, Type referencedType, String code) {
    TypeIdExpression node = new TypeIdExpression();
    node.setCode(code);
    node.setOperatorCode(operatorCode);
    node.setName(operatorCode);
    node.setType(type);
    node.setReferencedType(referencedType);

    log(node);

    return node;
  }

  public static TypedefDeclaration newTypedefDeclaration(Type targetType, Type alias, String code) {
    TypedefDeclaration node = new TypedefDeclaration();
    node.setType(targetType);
    node.setAlias(alias);
    node.setCode(code);

    log(node);

    return node;
  }

  public static ArraySubscriptionExpression newArraySubscriptionExpression(String code) {
    ArraySubscriptionExpression node = new ArraySubscriptionExpression();
    node.setCode(code);

    log(node);

    return node;
  }

  public static <T> Literal<T> newLiteral(T value, Type type, String code) {
    Literal<T> node = new Literal<>();
    node.setValue(value);
    node.setType(type);
    node.setCode(code);

    log(node);

    return node;
  }

  public static <T> Literal<T> duplicateLiteral(Literal<T> original, boolean implicit) {
    Literal<T> duplicate =
        NodeBuilder.newLiteral(original.getValue(), original.getType(), original.getCode());
    duplicate.setLocation(original.getLocation());
    duplicate.setLocals(original.getLocals());
    duplicate.setPossibleSubTypes(original.getPossibleSubTypes());
    duplicate.setArgumentIndex(original.getArgumentIndex());
    duplicate.setAnnotations(original.getAnnotations());
    duplicate.setComment(original.getComment());
    duplicate.setFile(original.getFile());
    duplicate.setName(original.getName());
    duplicate.setNextDFG(original.getNextDFG());
    duplicate.setPrevDFG(original.getPrevDFG());
    duplicate.setNextEOG(original.getNextEOG());
    duplicate.setPrevEOG(original.getPrevEOG());

    duplicate.setImplicit(implicit);
    return duplicate;
  }

  public static DeclaredReferenceExpression newDeclaredReferenceExpression(
      String name, Type typeFullName, String code) {
    DeclaredReferenceExpression node = new DeclaredReferenceExpression();
    node.setName(name);
    node.setType(typeFullName);
    node.setCode(code);

    log(node);

    return node;
  }

  public static ArrayRangeExpression newArrayRangeExpression(
      Expression floor, Expression ceil, String code) {
    ArrayRangeExpression node = new ArrayRangeExpression();
    node.setCode(code);
    node.setFloor(floor);
    node.setCeiling(ceil);

    log(node);

    return node;
  }

  public static FunctionDeclaration newFunctionDeclaration(String name, String code) {
    FunctionDeclaration node = new FunctionDeclaration();
    node.setName(name);
    node.setCode(code);

    log(node);

    return node;
  }

  private static void log(Node node) {
    LOGGER.trace("Creating {}", node);
  }

  public static ReturnStatement newReturnStatement(String code) {
    ReturnStatement node = new ReturnStatement();
    node.setCode(code);

    log(node);

    return node;
  }

  public static SynchronizedStatement newSynchronizedStatement(String code) {
    SynchronizedStatement node = new SynchronizedStatement();
    node.setCode(code);
    log(node);
    return node;
  }

  public static DeleteExpression newDeleteExpression(String code) {
    DeleteExpression node = new DeleteExpression();
    node.setCode(code);

    log(node);

    return node;
  }

  public static EmptyStatement newEmptyStatement(String code) {
    EmptyStatement node = new EmptyStatement();
    node.setCode(code);

    log(node);

    return node;
  }

  public static ParamVariableDeclaration newMethodParameterIn(
      String name, Type type, boolean variadic, String code) {
    ParamVariableDeclaration node = new ParamVariableDeclaration();
    node.setName(name);
    node.setType(type);
    node.setCode(code);
    node.setVariadic(variadic);

    return node;
  }

  public static TypeParamDeclaration newTypeParamDeclaration(String name, String code) {
    TypeParamDeclaration node = new TypeParamDeclaration();
    node.setName(name);
    node.setCode(code);

    log(node);

    return node;
  }

  public static CompoundStatement newCompoundStatement(String code) {
    CompoundStatement node = new CompoundStatement();
    node.setCode(code);

    log(node);

    return node;
  }

  public static ExpressionList newExpressionList(String code) {
    ExpressionList node = new ExpressionList();
    node.setCode(code);

    log(node);

    return node;
  }

  public static CallExpression newMemberCallExpression(
      String name, String fqn, Expression base, Node member, String operatorCode, String code) {
    MemberCallExpression node = new MemberCallExpression();
    node.setName(name);
    node.setBase(base);
    node.setMember(member);
    node.setOperatorCode(operatorCode);
    node.setCode(code);
    node.setFqn(fqn);

    log(node);

    return node;
  }

  public static TypeExpression newTypeExpression(String name, Type type) {
    TypeExpression node = new TypeExpression();
    node.setName(name);
    node.setType(type);

    log(node);

    return node;
  }

  public static TypeExpression duplicateTypeExpression(TypeExpression original, boolean implicit) {
    TypeExpression duplicate =
        NodeBuilder.newTypeExpression(original.getName(), original.getType());
    duplicate.setImplicit(implicit);
    return duplicate;
  }

  public static UnaryOperator newUnaryOperator(
      String operatorType, boolean postfix, boolean prefix, String code) {
    UnaryOperator node = new UnaryOperator();
    node.setOperatorCode(operatorType);
    node.setName(operatorType);
    node.setPostfix(postfix);
    node.setPrefix(prefix);
    node.setCode(code);

    log(node);

    return node;
  }

  public static VariableDeclaration newVariableDeclaration(
      String name, Type type, String code, boolean implicitInitializerAllowed) {
    VariableDeclaration node = new VariableDeclaration();
    node.setName(name);
    node.setType(type);
    node.setCode(code);
    node.setImplicitInitializerAllowed(implicitInitializerAllowed);

    log(node);

    return node;
  }

  public static DeclarationStatement newDeclarationStatement(String code) {
    DeclarationStatement node = new DeclarationStatement();
    node.setCode(code);

    return node;
  }

  public static IfStatement newIfStatement(String code) {
    IfStatement node = new IfStatement();
    node.setCode(code);

    log(node);

    return node;
  }

  public static LabelStatement newLabelStatement(String code) {
    LabelStatement node = new LabelStatement();
    node.setCode(code);

    log(node);

    return node;
  }

  public static GotoStatement newGotoStatement(String code) {
    GotoStatement node = new GotoStatement();
    node.setCode(code);

    log(node);

    return node;
  }

  public static WhileStatement newWhileStatement(String code) {
    WhileStatement node = new WhileStatement();
    node.setCode(code);

    log(node);

    return node;
  }

  public static DoStatement newDoStatement(String code) {
    DoStatement node = new DoStatement();
    node.setCode(code);

    log(node);

    return node;
  }

  public static ForEachStatement newForEachStatement(String code) {
    ForEachStatement node = new ForEachStatement();
    node.setCode(code);

    log(node);

    return node;
  }

  public static ForStatement newForStatement(String code) {
    ForStatement node = new ForStatement();
    node.setCode(code);

    log(node);

    return node;
  }

  public static ContinueStatement newContinueStatement(String code) {
    ContinueStatement node = new ContinueStatement();
    node.setCode(code);

    log(node);

    return node;
  }

  public static BreakStatement newBreakStatement(String code) {
    BreakStatement node = new BreakStatement();
    node.setCode(code);

    log(node);

    return node;
  }

  public static BinaryOperator newBinaryOperator(String operatorCode, String code) {
    BinaryOperator node = new BinaryOperator();
    node.setOperatorCode(operatorCode);
    node.setName(operatorCode);
    node.setCode(code);

    log(node);

    return node;
  }

  public static TranslationUnitDeclaration newTranslationUnitDeclaration(String name, String code) {
    TranslationUnitDeclaration node = new TranslationUnitDeclaration();
    node.setName(name);
    node.setCode(code);

    log(node);

    return node;
  }

  public static RecordDeclaration newRecordDeclaration(String name, String kind, String code) {
    RecordDeclaration node = new RecordDeclaration();
    node.setName(name);
    node.setKind(kind);
    node.setCode(code);

    if (kind.equals("class")) {
      FieldDeclaration thisDeclaration =
          NodeBuilder.newFieldDeclaration(
              "this",
              TypeParser.createFrom(name, true),
              new ArrayList<>(),
              "this",
              null,
              null,
              true);
      node.addField(thisDeclaration);
    }

    log(node);

    return node;
  }

  public static EnumDeclaration newEnumDeclaration(
      String name, String code, PhysicalLocation location) {
    EnumDeclaration node = new EnumDeclaration();
    node.setName(name);
    node.setCode(code);
    node.setLocation(location);

    log(node);

    return node;
  }

  public static FunctionTemplateDeclaration newFunctionTemplateDeclaration(
      String name, String code) {
    FunctionTemplateDeclaration node = new FunctionTemplateDeclaration();
    node.setName(name);
    node.setCode(code);

    log(node);
    return node;
  }

  public static ClassTemplateDeclaration newClassTemplateDeclaration(String name, String code) {
    ClassTemplateDeclaration node = new ClassTemplateDeclaration();
    node.setName(name);
    node.setCode(code);

    log(node);
    return node;
  }

  public static EnumConstantDeclaration newEnumConstantDeclaration(
      String name, String code, PhysicalLocation location) {
    EnumConstantDeclaration node = new EnumConstantDeclaration();
    node.setName(name);
    node.setCode(code);
    node.setLocation(location);

    log(node);

    return node;
  }

  public static FieldDeclaration newFieldDeclaration(
      String name,
      Type type,
      List<String> modifiers,
      String code,
      PhysicalLocation location,
      @Nullable Expression initializer,
      boolean implicitInitializerAllowed) {
    FieldDeclaration node = new FieldDeclaration();
    node.setName(name);
    node.setType(type);
    node.setModifiers(modifiers);
    node.setCode(code);
    node.setLocation(location);
    node.setImplicitInitializerAllowed(implicitInitializerAllowed);
    if (initializer != null) {
      if (initializer instanceof ArrayCreationExpression) {
        node.setIsArray(true);
      }
      node.setInitializer(initializer);
    }

    log(node);

    return node;
  }

  public static MemberExpression newMemberExpression(
      Expression base, Type memberType, String name, String operatorCode, String code) {
    MemberExpression node = new MemberExpression();
    node.setBase(base);
    node.setOperatorCode(operatorCode);
    node.setCode(code);
    node.setName(name);
    node.setType(memberType);

    log(node);

    return node;
  }

  public static Statement newStatement(String code) {
    Statement node = new Statement();
    node.setCode(code);

    log(node);

    return node;
  }

  public static Expression newExpression(String code) {
    Expression node = new Expression();
    node.setCode(code);

    log(node);

    return node;
  }

  public static InitializerListExpression newInitializerListExpression(String code) {
    InitializerListExpression node = new InitializerListExpression();
    node.setCode(code);

    log(node);

    return node;
  }

  public static DesignatedInitializerExpression newDesignatedInitializerExpression(String code) {
    DesignatedInitializerExpression node = new DesignatedInitializerExpression();
    node.setCode(code);

    log(node);

    return node;
  }

  public static ArrayCreationExpression newArrayCreationExpression(String code) {
    ArrayCreationExpression node = new ArrayCreationExpression();
    node.setCode(code);

    log(node);

    return node;
  }

  public static ConstructExpression newConstructExpression(String code) {
    ConstructExpression node = new ConstructExpression();
    node.setCode(code);

    log(node);

    return node;
  }

  public static MethodDeclaration newMethodDeclaration(
      String name, String code, boolean isStatic, @Nullable RecordDeclaration recordDeclaration) {
    MethodDeclaration node = new MethodDeclaration();
    node.setName(name);
    node.setCode(code);
    node.setStatic(isStatic);
    node.setRecordDeclaration(recordDeclaration);

    log(node);

    return node;
  }

  public static ConstructorDeclaration newConstructorDeclaration(
      String name, String code, @Nullable RecordDeclaration recordDeclaration) {
    ConstructorDeclaration node = new ConstructorDeclaration();
    node.setName(name);
    node.setCode(code);
    node.setRecordDeclaration(recordDeclaration);

    log(node);

    return node;
  }

  public static Declaration newDeclaration(String code) {
    Declaration node = new Declaration();
    node.setCode(code);

    log(node);

    return node;
  }

  public static ProblemDeclaration newProblemDeclaration(
      String filename, String problem, String problemLocation) {
    ProblemDeclaration node = new ProblemDeclaration();
    node.setFilename(filename);
    node.setProblem(problem);
    node.setProblemLocation(problemLocation);

    log(node);

    return node;
  }

  public static IncludeDeclaration newIncludeDeclaration(String includeFilename) {
    IncludeDeclaration node = new IncludeDeclaration();

    String name = includeFilename.substring(includeFilename.lastIndexOf('/') + 1);
    node.setName(name);
    node.setFilename(includeFilename);

    log(node);

    return node;
  }

  public static NewExpression newNewExpression(String code, Type type) {
    NewExpression node = new NewExpression();
    node.setCode(code);
    node.setType(type);

    log(node);

    return node;
  }

  public static SwitchStatement newSwitchStatement(String code) {
    SwitchStatement node = new SwitchStatement();
    node.setCode(code);

    log(node);

    return node;
  }

  public static CaseStatement newCaseStatement(String code) {
    CaseStatement node = new CaseStatement();
    node.setCode(code);

    log(node);

    return node;
  }

  public static DefaultStatement newDefaultStatement(String code) {
    DefaultStatement node = new DefaultStatement();
    node.setCode(code);

    log(node);

    return node;
  }

  public static ConditionalExpression newConditionalExpression(
      Expression condition, Expression thenExpr, Expression elseExpr, Type type) {
    ConditionalExpression node = new ConditionalExpression();
    node.setCondition(condition);
    node.setThenExpr(thenExpr);
    node.setElseExpr(elseExpr);
    node.setType(type);

    log(node);

    return node;
  }

  public static ExplicitConstructorInvocation newExplicitConstructorInvocation(
      String containingClass, String code) {
    ExplicitConstructorInvocation node = new ExplicitConstructorInvocation();
    node.setContainingClass(containingClass);
    node.setCode(code);

    log(node);

    return node;
  }

  public static NamespaceDeclaration newNamespaceDeclaration(@NonNull String name, String code) {
    NamespaceDeclaration node = new NamespaceDeclaration();
    node.setName(name);
    node.setCode(code);

    log(node);
    return node;
  }

  public static CatchClause newCatchClause(@NonNull String code) {
    CatchClause catchClause = new CatchClause();

    catchClause.setCode(code);
    return catchClause;
  }

  public static TryStatement newTryStatement(@NonNull String code) {
    TryStatement tryStatement = new TryStatement();
    tryStatement.setCode(code);
    return tryStatement;
  }

  public static AssertStatement newAssertStatement(@NonNull String code) {
    AssertStatement assertStatement = new AssertStatement();
    assertStatement.setCode(code);
    return assertStatement;
  }

  public static ASMDeclarationStatement newASMDeclarationStatement(@NonNull String code) {
    ASMDeclarationStatement asmStatement = new ASMDeclarationStatement();
    asmStatement.setCode(code);
    return asmStatement;
  }

  public static CompoundStatementExpression newCompoundStatementExpression(@NonNull String code) {
    CompoundStatementExpression cse = new CompoundStatementExpression();
    cse.setCode(code);
    return cse;
  }

  public static Annotation newAnnotation(String name, @NonNull String code) {
    Annotation annotation = new Annotation();
    annotation.setName(name);
    annotation.setCode(code);

    return annotation;
  }

  public static AnnotationMember newAnnotationMember(
      String name, Expression value, @NonNull String code) {
    var member = new AnnotationMember();
    member.setName(name);
    member.setValue(value);
    member.setCode(code);

    return member;
  }

  @NotNull
  public static KeyValueExpression newKeyValueExpression(
      @org.jetbrains.annotations.Nullable Expression key,
      @org.jetbrains.annotations.Nullable Expression value,
      @org.jetbrains.annotations.Nullable String code) {
    var keyValue = new KeyValueExpression();
    keyValue.setKey(key);
    keyValue.setValue(value);
    keyValue.setCode(code);

    return keyValue;
  }
}
