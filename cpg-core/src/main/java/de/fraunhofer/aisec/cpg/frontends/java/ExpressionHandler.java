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
package de.fraunhofer.aisec.cpg.frontends.java;

import com.github.javaparser.TokenRange;
import com.github.javaparser.ast.ArrayCreationLevel;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.declarations.ResolvedFieldDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedTypeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedValueDeclaration;
import de.fraunhofer.aisec.cpg.frontends.Handler;
import de.fraunhofer.aisec.cpg.graph.*;
import de.fraunhofer.aisec.cpg.graph.declarations.RecordDeclaration;
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration;
import de.fraunhofer.aisec.cpg.graph.statements.DeclarationStatement;
import de.fraunhofer.aisec.cpg.graph.statements.Statement;
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*;
import de.fraunhofer.aisec.cpg.graph.types.PointerType;
import de.fraunhofer.aisec.cpg.graph.types.Type;
import de.fraunhofer.aisec.cpg.graph.types.TypeParser;
import de.fraunhofer.aisec.cpg.graph.types.UnknownType;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExpressionHandler extends Handler<Statement, Expression, JavaLanguageFrontend> {

  private static final Logger log = LoggerFactory.getLogger(ExpressionHandler.class);

  public ExpressionHandler(JavaLanguageFrontend lang) {
    super(ProblemExpression::new, lang);

    map.put(AssignExpr.class, this::handleAssignmentExpression);
    map.put(FieldAccessExpr.class, this::handleFieldAccessExpression);

    map.put(LiteralExpr.class, this::handleLiteralExpression);

    map.put(ThisExpr.class, this::handleThisExpression);
    map.put(SuperExpr.class, this::handleSuperExpression);
    map.put(ClassExpr.class, this::handleClassExpression);
    map.put(NameExpr.class, this::handleNameExpression);
    map.put(InstanceOfExpr.class, this::handleInstanceOfExpression);
    map.put(UnaryExpr.class, this::handleUnaryExpression);
    map.put(BinaryExpr.class, this::handleBinaryExpression);
    map.put(VariableDeclarationExpr.class, this::handleVariableDeclarationExpr);
    map.put(MethodCallExpr.class, this::handleMethodCallExpression);
    map.put(ObjectCreationExpr.class, this::handleObjectCreationExpr);
    map.put(ConditionalExpr.class, this::handleConditionalExpression);
    map.put(EnclosedExpr.class, this::handleEnclosedExpression);
    map.put(ArrayAccessExpr.class, this::handleArrayAccessExpr);
    map.put(ArrayCreationExpr.class, this::handleArrayCreationExpr);
    map.put(ArrayInitializerExpr.class, this::handleArrayInitializerExpr);
    map.put(CastExpr.class, this::handleCastExpr);
  }

  private Statement handleCastExpr(Expression expr) {
    CastExpr castExpr = expr.asCastExpr();
    CastExpression castExpression = NodeBuilder.newCastExpression(expr.toString());

    de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression expression =
        (de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression)
            handle(castExpr.getExpression());

    castExpression.setExpression(expression);
    castExpression.setCastOperator(2);
    Type t = this.lang.getTypeAsGoodAsPossible(castExpr.getType());
    castExpression.setCastType(t);
    if (castExpr.getType().isPrimitiveType()) {
      // Set Type based on the Casting type as it will result in a conversion for primitive types
      castExpression.setType(
          TypeParser.createFrom(castExpr.getType().resolve().asPrimitive().describe(), true));
    } else {
      // Get Runtime type from cast expression for complex types;

      castExpression.getExpression().registerTypeListener(castExpression);
    }
    return castExpression;
  }

  /**
   * Creates a new {@link ArrayCreationExpression}, which is usually used as an initializer of a
   * {@link VariableDeclaration}.
   *
   * @param expr the expression
   * @return the {@link ArrayCreationExpression}
   */
  private Statement handleArrayCreationExpr(Expression expr) {
    ArrayCreationExpr arrayCreationExpr = (ArrayCreationExpr) expr;
    ArrayCreationExpression creationExpression =
        NodeBuilder.newArrayCreationExpression(expr.toString());

    // in Java, an array creation expression either specifies an initializer or dimensions

    // parse initializer, if present
    arrayCreationExpr
        .getInitializer()
        .ifPresent(
            init -> creationExpression.setInitializer((InitializerListExpression) handle(init)));

    // dimensions are only present if you specify them explicitly, such as new int[1]
    for (ArrayCreationLevel lvl : arrayCreationExpr.getLevels()) {
      lvl.getDimension()
          .ifPresent(
              expression ->
                  creationExpression.addDimension(
                      (de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression)
                          handle(expression)));
    }

    return creationExpression;
  }

  private Statement handleArrayInitializerExpr(Expression expr) {
    ArrayInitializerExpr arrayInitializerExpr = (ArrayInitializerExpr) expr;
    // ArrayInitializerExpressions are converted into InitializerListExpressions to reduce the
    // syntactic distance a CPP and JAVA CPG
    InitializerListExpression initList = NodeBuilder.newInitializerListExpression(expr.toString());
    List<de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression> initializers =
        arrayInitializerExpr.getValues().stream()
            .map(this::handle)
            .map(de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression.class::cast)
            .collect(Collectors.toList());
    initList.setInitializers(initializers);
    return initList;
  }

  private ArraySubscriptionExpression handleArrayAccessExpr(Expression expr) {
    ArrayAccessExpr arrayAccessExpr = (ArrayAccessExpr) expr;
    ArraySubscriptionExpression arraySubsExpression =
        NodeBuilder.newArraySubscriptionExpression(expr.toString());
    arraySubsExpression.setArrayExpression(
        (de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression)
            handle(arrayAccessExpr.getName()));
    arraySubsExpression.setSubscriptExpression(
        (de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression)
            handle(arrayAccessExpr.getIndex()));
    return arraySubsExpression;
  }

  private Statement handleEnclosedExpression(Expression expr) {
    return handle(((EnclosedExpr) expr).getInner());
  }

  private ConditionalExpression handleConditionalExpression(Expression expr) {
    ConditionalExpr conditionalExpr = expr.asConditionalExpr();
    Type superType;
    try {
      superType = TypeParser.createFrom(conditionalExpr.calculateResolvedType().describe(), true);
    } catch (RuntimeException | NoClassDefFoundError e) {
      String s = this.lang.recoverTypeFromUnsolvedException(e);
      if (s != null) {
        superType = TypeParser.createFrom(s, true);
      } else {
        superType = null;
      }
    }

    de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression condition =
        (de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression)
            handle(conditionalExpr.getCondition());
    de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression thenExpr =
        (de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression)
            handle(conditionalExpr.getThenExpr());
    de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression elseExpr =
        (de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression)
            handle(conditionalExpr.getElseExpr());
    return NodeBuilder.newConditionalExpression(condition, thenExpr, elseExpr, superType);
  }

  private BinaryOperator handleAssignmentExpression(Expression expr) {
    AssignExpr assignExpr = expr.asAssignExpr();

    // first, handle the target. this is the first argument of the operator call
    de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression lhs =
        (de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression)
            this.handle(assignExpr.getTarget());

    // second, handle the value. this is the second argument of the operator call
    de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression rhs =
        (de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression)
            handle(assignExpr.getValue());

    BinaryOperator binaryOperator =
        NodeBuilder.newBinaryOperator(assignExpr.getOperator().asString(), assignExpr.toString());

    binaryOperator.setLhs(lhs);
    binaryOperator.setRhs(rhs);

    return binaryOperator;
  }

  // Not sure how to handle this exactly yet
  private DeclarationStatement handleVariableDeclarationExpr(Expression expr) {
    VariableDeclarationExpr variableDeclarationExpr = expr.asVariableDeclarationExpr();

    DeclarationStatement declarationStatement =
        NodeBuilder.newDeclarationStatement(variableDeclarationExpr.toString());

    for (VariableDeclarator variable : variableDeclarationExpr.getVariables()) {
      ResolvedValueDeclaration resolved = variable.resolve();

      Type declarationType = this.lang.getTypeAsGoodAsPossible(variable, resolved);
      declarationType.setAdditionalTypeKeywords(
          variableDeclarationExpr.getModifiers().stream()
              .map(m -> m.getKeyword().asString())
              .collect(Collectors.joining(" ")));

      VariableDeclaration declaration =
          NodeBuilder.newVariableDeclaration(
              resolved.getName(), declarationType, variable.toString(), false, lang, variable);

      if (declarationType instanceof PointerType && ((PointerType) declarationType).isArray()) {
        declaration.setIsArray(true);
      }

      Optional<Expression> oInitializer = variable.getInitializer();

      if (oInitializer.isPresent()) {
        de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression initializer =
            (de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression)
                handle(oInitializer.get());
        if (initializer instanceof ArrayCreationExpression) {
          declaration.setIsArray(true);
        }
        declaration.setInitializer(initializer);
      } else {
        de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression uninitialzedInitializer =
            new UninitializedValue();
        declaration.setInitializer(uninitialzedInitializer);
      }
      lang.setCodeAndRegion(declaration, variable);
      declarationStatement.addToPropertyEdgeDeclaration(declaration);

      lang.processAnnotations(declaration, variableDeclarationExpr);

      lang.getScopeManager().addDeclaration(declaration);
    }

    return declarationStatement;
  }

  private de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression
      handleFieldAccessExpression(Expression expr) {
    FieldAccessExpr fieldAccessExpr = expr.asFieldAccessExpr();
    de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression base;
    // first, resolve the scope. this adds the necessary nodes, such as IDENTIFIER for the scope.
    // it also acts as the first argument of the operator call
    Expression scope = fieldAccessExpr.getScope();
    if (scope.isNameExpr()) {
      boolean isStaticAccess = false;
      Type baseType;
      try {
        ResolvedValueDeclaration resolve = fieldAccessExpr.resolve();
        if (resolve.asField().isStatic()) {
          isStaticAccess = true;
        }
        baseType =
            TypeParser.createFrom(resolve.asField().declaringType().getQualifiedName(), true);

      } catch (RuntimeException | NoClassDefFoundError ex) {
        isStaticAccess = true;
        String typeString = this.lang.recoverTypeFromUnsolvedException(ex);
        if (typeString != null) {
          baseType = TypeParser.createFrom(typeString, true);
        } else {
          // try to get the name
          String name;
          Optional<TokenRange> tokenRange = scope.asNameExpr().getTokenRange();
          if (tokenRange.isPresent()) {
            name = tokenRange.get().toString();
          } else {
            name = scope.asNameExpr().getNameAsString();
          }
          String qualifiedNameFromImports = this.lang.getQualifiedNameFromImports(name);
          if (qualifiedNameFromImports != null) {
            baseType = TypeParser.createFrom(qualifiedNameFromImports, true);
          } else {
            log.info("Unknown base type 1 for {}", fieldAccessExpr);
            baseType = UnknownType.getUnknownType();
          }
        }
      }
      base =
          NodeBuilder.newDeclaredReferenceExpression(
              scope.asNameExpr().getNameAsString(), baseType, scope.toString());
      ((DeclaredReferenceExpression) base).setStaticAccess(isStaticAccess);

      lang.setCodeAndRegion(base, fieldAccessExpr.getScope());
    } else if (scope.isFieldAccessExpr()) {
      base = (de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression) handle(scope);
      de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression tester = base;
      while (tester instanceof MemberExpression) {
        // we need to check if any base is only a static access, otherwise, this is a member access
        // to this base
        tester = ((MemberExpression) tester).getBase();
      }
      if (tester instanceof DeclaredReferenceExpression
          && ((DeclaredReferenceExpression) tester).isStaticAccess()) {
        // try to get the name
        String name;
        Optional<TokenRange> tokenRange = scope.asFieldAccessExpr().getTokenRange();
        if (tokenRange.isPresent()) {
          name = tokenRange.get().toString();
        } else {
          name = scope.asFieldAccessExpr().getNameAsString();
        }
        String qualifiedNameFromImports = this.lang.getQualifiedNameFromImports(name);
        Type baseType;
        if (qualifiedNameFromImports != null) {
          baseType = TypeParser.createFrom(qualifiedNameFromImports, true);
        } else {
          log.info("Unknown base type 2 for {}", fieldAccessExpr);
          baseType = UnknownType.getUnknownType();
        }
        base =
            NodeBuilder.newDeclaredReferenceExpression(
                scope.asFieldAccessExpr().getNameAsString(), baseType, scope.toString());
        ((DeclaredReferenceExpression) base).setStaticAccess(true);
      }
      lang.setCodeAndRegion(base, fieldAccessExpr.getScope());
    } else {
      base = (de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression) handle(scope);
    }

    Type fieldType;
    try {
      ResolvedValueDeclaration symbol = fieldAccessExpr.resolve();
      fieldType =
          TypeManager.getInstance()
              .getTypeParameter(
                  this.lang.getScopeManager().getCurrentRecord(),
                  symbol.asField().getType().describe());
      if (fieldType == null) {
        fieldType = TypeParser.createFrom(symbol.asField().getType().describe(), true);
      }
    } catch (RuntimeException | NoClassDefFoundError ex) {
      String typeString = this.lang.recoverTypeFromUnsolvedException(ex);
      if (typeString != null) {
        fieldType = TypeParser.createFrom(typeString, true);
      } else if (fieldAccessExpr.toString().endsWith(".length")) {
        fieldType = TypeParser.createFrom("int", true);
      } else {
        log.info("Unknown field type for {}", fieldAccessExpr);
        fieldType = UnknownType.getUnknownType();
      }

      MemberExpression memberExpression =
          NodeBuilder.newMemberExpression(
              base,
              fieldType,
              fieldAccessExpr.getName().getIdentifier(),
              ".", // there is only "." in java
              fieldAccessExpr.toString());
      memberExpression.setStaticAccess(true);
      return memberExpression;
    }

    if (base.getLocation() == null) {
      base.setLocation(lang.getLocationFromRawNode(fieldAccessExpr));
    }

    return NodeBuilder.newMemberExpression(
        base,
        fieldType,
        fieldAccessExpr.getName().getIdentifier(),
        ".",
        fieldAccessExpr.toString());
  }

  private Literal handleLiteralExpression(Expression expr) {
    LiteralExpr literalExpr = expr.asLiteralExpr();

    String value = literalExpr.toString();
    if (literalExpr instanceof IntegerLiteralExpr) {
      return NodeBuilder.newLiteral(
          literalExpr.asIntegerLiteralExpr().asNumber(), TypeParser.createFrom("int", true), value);
    } else if (literalExpr instanceof StringLiteralExpr) {
      return NodeBuilder.newLiteral(
          literalExpr.asStringLiteralExpr().asString(),
          TypeParser.createFrom("java.lang.String", true),
          value);
    } else if (literalExpr instanceof BooleanLiteralExpr) {
      return NodeBuilder.newLiteral(
          literalExpr.asBooleanLiteralExpr().getValue(),
          TypeParser.createFrom("boolean", true),
          value);
    } else if (literalExpr instanceof CharLiteralExpr) {
      return NodeBuilder.newLiteral(
          literalExpr.asCharLiteralExpr().asChar(), TypeParser.createFrom("char", true), value);
    } else if (literalExpr instanceof DoubleLiteralExpr) {
      return NodeBuilder.newLiteral(
          literalExpr.asDoubleLiteralExpr().asDouble(),
          TypeParser.createFrom("double", true),
          value);
    } else if (literalExpr instanceof LongLiteralExpr) {
      return NodeBuilder.newLiteral(
          literalExpr.asLongLiteralExpr().asNumber(), TypeParser.createFrom("long", true), value);
    } else if (literalExpr instanceof NullLiteralExpr) {
      return NodeBuilder.newLiteral(null, TypeParser.createFrom("null", true), value);
    }

    return null;
  }

  private DeclaredReferenceExpression handleClassExpression(Expression expr) {
    ClassExpr classExpr = expr.asClassExpr();

    Type type = TypeParser.createFrom(classExpr.getType().asString(), true);

    DeclaredReferenceExpression thisExpression =
        NodeBuilder.newDeclaredReferenceExpression(
            classExpr.toString().substring(classExpr.toString().lastIndexOf('.') + 1),
            type,
            classExpr.toString());
    thisExpression.setStaticAccess(true);
    lang.setCodeAndRegion(thisExpression, classExpr);

    return thisExpression;
  }

  private DeclaredReferenceExpression handleThisExpression(Expression expr) {
    // TODO: use a separate ThisExpression (issue #8)
    ThisExpr thisExpr = expr.asThisExpr();
    ResolvedTypeDeclaration resolvedValueDeclaration = thisExpr.resolve();

    Type type = TypeParser.createFrom(resolvedValueDeclaration.getQualifiedName(), true);

    DeclaredReferenceExpression thisExpression =
        NodeBuilder.newDeclaredReferenceExpression(thisExpr.toString(), type, thisExpr.toString());
    lang.setCodeAndRegion(thisExpression, thisExpr);

    return thisExpression;
  }

  private DeclaredReferenceExpression handleSuperExpression(Expression expr) {
    // The actual type is hard to determine at this point, as we may not have full information
    // about the inheritance structure. Thus we delay the resolving to the variable resolving
    // process
    DeclaredReferenceExpression superExpression =
        NodeBuilder.newDeclaredReferenceExpression(
            expr.toString(), UnknownType.getUnknownType(), expr.toString());
    lang.setCodeAndRegion(superExpression, expr);

    return superExpression;
  }

  // TODO: this function needs a MAJOR overhaul!
  private de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression handleNameExpression(
      Expression expr) {
    NameExpr nameExpr = expr.asNameExpr();

    // TODO this commented code breaks field accesses to fields that don't have a primitive type.
    //  How should this be handled correctly?
    //    try {
    //      ResolvedType resolvedType = nameExpr.calculateResolvedType();
    //      if (resolvedType.isReferenceType()) {
    //        return NodeBuilder.newDeclaredReferenceExpression(
    //            nameExpr.getNameAsString(),
    //            new Type(((ReferenceTypeImpl) resolvedType).getQualifiedName()),
    //            nameExpr.toString());
    //      }
    //    } catch (
    //        UnsolvedSymbolException
    //            e) { // this might throw, e.g. if the type is simply not defined (i.e., syntax
    // error)
    //      return NodeBuilder.newDeclaredReferenceExpression(
    //          nameExpr.getNameAsString(), new Type(UNKNOWN_TYPE), nameExpr.toString());
    //    }

    try {
      ResolvedValueDeclaration symbol = nameExpr.resolve();

      if (symbol.isField()) {
        ResolvedFieldDeclaration field = symbol.asField();

        if (!field.isStatic()) {
          // convert to FieldAccessExpr
          FieldAccessExpr fieldAccessExpr = new FieldAccessExpr(new ThisExpr(), field.getName());
          expr.getRange().ifPresent(fieldAccessExpr::setRange);
          expr.getTokenRange().ifPresent(fieldAccessExpr::setTokenRange);
          expr.getParentNode().ifPresent(fieldAccessExpr::setParentNode);
          expr.replace(fieldAccessExpr);
          fieldAccessExpr.getParentNode().ifPresent(expr::setParentNode);

          // handle it as a field expression
          return (de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression)
              handle(fieldAccessExpr);
        } else {
          FieldAccessExpr fieldAccessExpr =
              new FieldAccessExpr(
                  new NameExpr(field.declaringType().getClassName()), field.getName());
          expr.getRange().ifPresent(fieldAccessExpr::setRange);
          expr.getTokenRange().ifPresent(fieldAccessExpr::setTokenRange);
          expr.getParentNode().ifPresent(fieldAccessExpr::setParentNode);
          expr.replace(fieldAccessExpr);
          fieldAccessExpr.getParentNode().ifPresent(expr::setParentNode);

          // handle it as a field expression
          return (de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression)
              handle(fieldAccessExpr);
        }
      } else {
        // Resolve type first with ParameterizedType
        Type type =
            TypeManager.getInstance()
                .getTypeParameter(
                    this.lang.getScopeManager().getCurrentRecord(), symbol.getType().describe());

        if (type == null) {
          type = TypeParser.createFrom(symbol.getType().describe(), true);
        }

        DeclaredReferenceExpression declaredReferenceExpression =
            NodeBuilder.newDeclaredReferenceExpression(symbol.getName(), type, nameExpr.toString());

        return declaredReferenceExpression;
      }
    } catch (UnsolvedSymbolException ex) {
      String typeString;
      if (ex.getName().startsWith("We are unable to find the value declaration corresponding to")) {
        typeString = nameExpr.getNameAsString();
      } else {
        typeString = this.lang.recoverTypeFromUnsolvedException(ex);
      }
      Type t;
      if (typeString == null) {
        t = TypeParser.createFrom("UNKNOWN3", true);
        log.info("Unresolved symbol: {}", nameExpr.getNameAsString());
      } else {
        t = TypeParser.createFrom(typeString, true);
        t.setTypeOrigin(Type.Origin.GUESSED);
      }

      var name = nameExpr.getNameAsString();

      DeclaredReferenceExpression declaredReferenceExpression =
          NodeBuilder.newDeclaredReferenceExpression(name, t, nameExpr.toString());

      var recordDeclaration = this.lang.getScopeManager().getCurrentRecord();

      if (recordDeclaration != null && Objects.equals(recordDeclaration.getName(), name)) {
        declaredReferenceExpression.setRefersTo(recordDeclaration);
      }

      return declaredReferenceExpression;
    } catch (RuntimeException | NoClassDefFoundError ex) {
      Type t = TypeParser.createFrom("UNKNOWN4", true);
      log.info("Unresolved symbol: {}", nameExpr.getNameAsString());

      DeclaredReferenceExpression declaredReferenceExpression =
          NodeBuilder.newDeclaredReferenceExpression(
              nameExpr.getNameAsString(), t, nameExpr.toString());

      return declaredReferenceExpression;
    }
  }

  private BinaryOperator handleInstanceOfExpression(Expression expr) {
    InstanceOfExpr binaryExpr = expr.asInstanceOfExpr();

    // first, handle the target. this is the first argument of the operator callUnresolved symbol
    de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression lhs =
        (de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression)
            handle(binaryExpr.getExpression());

    Type typeAsGoodAsPossible = this.lang.getTypeAsGoodAsPossible(binaryExpr.getType());

    // second, handle the value. this is the second argument of the operator call
    de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression rhs =
        NodeBuilder.newLiteral(
            typeAsGoodAsPossible.getTypeName(),
            TypeParser.createFrom("class", true),
            binaryExpr.getTypeAsString());

    BinaryOperator binaryOperator =
        NodeBuilder.newBinaryOperator("instanceof", binaryExpr.toString());

    binaryOperator.setLhs(lhs);
    binaryOperator.setRhs(rhs);

    return binaryOperator;
  }

  private UnaryOperator handleUnaryExpression(Expression expr) {
    UnaryExpr unaryExpr = expr.asUnaryExpr();

    // handle the 'inner' expression, which is affected by the unary expression
    de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression expression =
        (de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression)
            handle(unaryExpr.getExpression());

    UnaryOperator unaryOperator =
        NodeBuilder.newUnaryOperator(
            unaryExpr.getOperator().asString(),
            unaryExpr.isPostfix(),
            unaryExpr.isPrefix(),
            unaryExpr.toString());

    unaryOperator.setInput(expression);

    return unaryOperator;
  }

  private BinaryOperator handleBinaryExpression(Expression expr) {
    BinaryExpr binaryExpr = expr.asBinaryExpr();

    // first, handle the target. this is the first argument of the operator call
    de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression lhs =
        (de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression)
            handle(binaryExpr.getLeft());

    // second, handle the value. this is the second argument of the operator call
    de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression rhs =
        (de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression)
            handle(binaryExpr.getRight());

    BinaryOperator binaryOperator =
        NodeBuilder.newBinaryOperator(binaryExpr.getOperator().asString(), binaryExpr.toString());

    binaryOperator.setLhs(lhs);
    binaryOperator.setRhs(rhs);

    return binaryOperator;
  }

  private CallExpression handleMethodCallExpression(Expression expr) {
    MethodCallExpr methodCallExpr = expr.asMethodCallExpr();

    CallExpression callExpression;
    Optional<Expression> o = methodCallExpr.getScope();
    String qualifiedName = this.lang.getQualifiedMethodNameAsGoodAsPossible(methodCallExpr);
    String name = qualifiedName;
    if (name.contains(".")) {
      name = name.substring(name.lastIndexOf('.') + 1);
    }

    var typeString = UnknownType.UNKNOWN_TYPE_STRING;
    var isStatic = false;

    ResolvedMethodDeclaration resolved = null;
    try {
      // try resolving the method to learn more about it
      resolved = methodCallExpr.resolve();
      isStatic = resolved.isStatic();
      typeString = resolved.getReturnType().describe();
    } catch (NoClassDefFoundError | RuntimeException ignored) {
      // Unfortunately, JavaParser also throws a simple RuntimeException instead of an
      // UnsolvedSymbolException within resolve() if it fails to resolve it under certain
      // circumstances, we catch all that and continue on our own
      log.debug("Could not resolve method {}", methodCallExpr);
    }

    // the scope could either be a variable or also the class name (static call!)
    // thus, only because the scope is present, this is not automatically a member call
    if (o.isPresent()) {
      Expression scope = o.get();
      String scopeName = null;

      if (scope instanceof NameExpr) {
        scopeName = ((NameExpr) scope).getNameAsString();
      } else if (scope instanceof SuperExpr) {
        scopeName = scope.toString();
      }

      de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression base =
          (de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression) handle(scope);

      // If the base directly refers to a record, then this is a static call
      if (base instanceof DeclaredReferenceExpression
          && ((DeclaredReferenceExpression) base).getRefersTo() instanceof RecordDeclaration) {
        isStatic = true;
      }

      // Or if the base is a reference to an import
      if (base instanceof DeclaredReferenceExpression
          && this.lang.getQualifiedNameFromImports(base.getName()) != null) {
        isStatic = true;
      }

      if (!isStatic) {
        DeclaredReferenceExpression member =
            NodeBuilder.newDeclaredReferenceExpression(name, UnknownType.getUnknownType(), "");

        lang.setCodeAndRegion(
            member,
            methodCallExpr
                .getName()); // This will also overwrite the code set to the empty string set above
        callExpression =
            NodeBuilder.newMemberCallExpression(
                name, qualifiedName, base, member, ".", methodCallExpr.toString());
      } else {
        String targetClass;
        if (resolved != null) {
          targetClass = resolved.declaringType().getQualifiedName();
        } else {
          targetClass = this.lang.getQualifiedNameFromImports(scopeName);
        }

        if (targetClass == null) {
          targetClass = scopeName;
        }

        callExpression =
            NodeBuilder.newStaticCallExpression(
                name, qualifiedName, methodCallExpr.toString(), targetClass);
      }
    } else {
      callExpression =
          NodeBuilder.newCallExpression(name, qualifiedName, methodCallExpr.toString(), false);
    }

    callExpression.setType(TypeParser.createFrom(typeString, true));

    NodeList<Expression> arguments = methodCallExpr.getArguments();

    // handle the arguments
    for (int i = 0; i < arguments.size(); i++) {
      de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression argument =
          (de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression)
              handle(arguments.get(i));

      argument.setArgumentIndex(i);

      callExpression.addArgument(argument);
    }

    return callExpression;
  }

  private NewExpression handleObjectCreationExpr(Expression expr) {
    ObjectCreationExpr objectCreationExpr = expr.asObjectCreationExpr();

    // scope refers to the constructor arguments
    Optional<Expression> o = objectCreationExpr.getScope();
    if (o.isPresent()) {
      // TODO: what to do with it?
      log.warn("Scope {}", o);
    }

    // todo can we merge newNewExpression and newConstructExpression?
    Type t = this.lang.getTypeAsGoodAsPossible(objectCreationExpr.getType());

    NewExpression newExpression = NodeBuilder.newNewExpression(expr.toString(), t);

    NodeList<Expression> arguments = objectCreationExpr.getArguments();

    String code = expr.toString();
    if (code.length() > 4) {
      code = code.substring(4); // remove "new "
    }

    ConstructExpression ctor = NodeBuilder.newConstructExpression(code);
    ctor.setType(t);
    lang.setCodeAndRegion(ctor, expr);

    // handle the arguments
    for (int i = 0; i < arguments.size(); i++) {
      de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression argument =
          (de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression)
              handle(arguments.get(i));

      argument.setArgumentIndex(i);

      ctor.addArgument(argument);
    }

    newExpression.setInitializer(ctor);
    return newExpression;
  }
}
