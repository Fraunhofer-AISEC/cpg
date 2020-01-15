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

import com.github.javaparser.TokenRange;
import com.github.javaparser.ast.ArrayCreationLevel;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.ArrayAccessExpr;
import com.github.javaparser.ast.expr.ArrayCreationExpr;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.expr.CastExpr;
import com.github.javaparser.ast.expr.CharLiteralExpr;
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.ast.expr.ConditionalExpr;
import com.github.javaparser.ast.expr.DoubleLiteralExpr;
import com.github.javaparser.ast.expr.EnclosedExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.InstanceOfExpr;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.LiteralExpr;
import com.github.javaparser.ast.expr.LongLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.declarations.ResolvedFieldDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedTypeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedValueDeclaration;
import de.fraunhofer.aisec.cpg.frontends.Handler;
import de.fraunhofer.aisec.cpg.graph.ArrayCreationExpression;
import de.fraunhofer.aisec.cpg.graph.ArraySubscriptionExpression;
import de.fraunhofer.aisec.cpg.graph.BinaryOperator;
import de.fraunhofer.aisec.cpg.graph.CallExpression;
import de.fraunhofer.aisec.cpg.graph.CastExpression;
import de.fraunhofer.aisec.cpg.graph.ConditionalExpression;
import de.fraunhofer.aisec.cpg.graph.ConstructExpression;
import de.fraunhofer.aisec.cpg.graph.DeclarationStatement;
import de.fraunhofer.aisec.cpg.graph.DeclaredReferenceExpression;
import de.fraunhofer.aisec.cpg.graph.InitializerListExpression;
import de.fraunhofer.aisec.cpg.graph.Literal;
import de.fraunhofer.aisec.cpg.graph.MemberExpression;
import de.fraunhofer.aisec.cpg.graph.NewExpression;
import de.fraunhofer.aisec.cpg.graph.NodeBuilder;
import de.fraunhofer.aisec.cpg.graph.Statement;
import de.fraunhofer.aisec.cpg.graph.StaticReferenceExpression;
import de.fraunhofer.aisec.cpg.graph.Type;
import de.fraunhofer.aisec.cpg.graph.UnaryOperator;
import de.fraunhofer.aisec.cpg.graph.VariableDeclaration;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExpressionHandler
    extends Handler<de.fraunhofer.aisec.cpg.graph.Statement, Expression, JavaLanguageFrontend> {

  private static final Logger log = LoggerFactory.getLogger(ExpressionHandler.class);

  public ExpressionHandler(JavaLanguageFrontend lang) {
    super(de.fraunhofer.aisec.cpg.graph.Expression::new, lang);

    map.put(AssignExpr.class, this::handleAssignmentExpression);
    map.put(FieldAccessExpr.class, this::handleFieldAccessExpression);

    map.put(LiteralExpr.class, this::handleLiteralExpression);

    map.put(ThisExpr.class, this::handleThisExpression);
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

    de.fraunhofer.aisec.cpg.graph.Expression expression =
        (de.fraunhofer.aisec.cpg.graph.Expression) handle(castExpr.getExpression());

    castExpression.setExpression(expression);
    castExpression.setCastOperator(2);
    Type t = this.lang.getTypeAsGoodAsPossible(castExpr.getType());
    castExpression.setCastType(t);
    if (castExpr.getType().isPrimitiveType()) {
      // Set Type based on the Casting type as it will result in a conversion for primitive types
      castExpression.setType(new Type(castExpr.getType().resolve().asPrimitive().describe()));
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
                  creationExpression
                      .getDimensions()
                      .add((de.fraunhofer.aisec.cpg.graph.Expression) handle(expression)));
    }

    return creationExpression;
  }

  private Statement handleArrayInitializerExpr(Expression expr) {
    ArrayInitializerExpr arrayInitializerExpr = (ArrayInitializerExpr) expr;
    // ArrayInitializerExpressions are converted into InitializerListExpressions to reduce the
    // syntactic distance a CPP and JAVA CPG
    InitializerListExpression initList = NodeBuilder.newInitializerListExpression(expr.toString());
    List<de.fraunhofer.aisec.cpg.graph.Expression> initializers =
        arrayInitializerExpr.getValues().stream()
            .map(this::handle)
            .map(de.fraunhofer.aisec.cpg.graph.Expression.class::cast)
            .collect(Collectors.toList());
    initList.setInitializers(initializers);
    return initList;
  }

  private ArraySubscriptionExpression handleArrayAccessExpr(Expression expr) {
    ArrayAccessExpr arrayAccessExpr = (ArrayAccessExpr) expr;
    ArraySubscriptionExpression arraySubsExpression =
        NodeBuilder.newArraySubscriptionExpression(expr.toString());
    arraySubsExpression.setArrayExpression(
        (de.fraunhofer.aisec.cpg.graph.Expression) handle(arrayAccessExpr.getName()));
    arraySubsExpression.setSubscriptExpression(
        (de.fraunhofer.aisec.cpg.graph.Expression) handle(arrayAccessExpr.getIndex()));
    return arraySubsExpression;
  }

  private Statement handleEnclosedExpression(Expression expr) {
    return handle(((EnclosedExpr) expr).getInner());
  }

  private ConditionalExpression handleConditionalExpression(Expression expr) {
    ConditionalExpr conditionalExpr = expr.asConditionalExpr();
    Type superType;
    try {
      superType = new Type(conditionalExpr.calculateResolvedType().describe());
    } catch (RuntimeException | NoClassDefFoundError e) {
      String s = this.lang.recoverTypeFromUnsolvedException(e);
      if (s != null) {
        superType = new Type(s);
      } else {
        superType = null;
      }
    }

    de.fraunhofer.aisec.cpg.graph.Expression condition =
        (de.fraunhofer.aisec.cpg.graph.Expression) handle(conditionalExpr.getCondition());
    de.fraunhofer.aisec.cpg.graph.Expression thenExpr =
        (de.fraunhofer.aisec.cpg.graph.Expression) handle(conditionalExpr.getThenExpr());
    de.fraunhofer.aisec.cpg.graph.Expression elseExpr =
        (de.fraunhofer.aisec.cpg.graph.Expression) handle(conditionalExpr.getElseExpr());
    return NodeBuilder.newConditionalExpression(condition, thenExpr, elseExpr, superType);
  }

  private BinaryOperator handleAssignmentExpression(Expression expr) {
    AssignExpr assignExpr = expr.asAssignExpr();

    // first, handle the target. this is the first argument of the operator call
    de.fraunhofer.aisec.cpg.graph.Expression lhs =
        (de.fraunhofer.aisec.cpg.graph.Expression) this.handle(assignExpr.getTarget());

    // second, handle the value. this is the second argument of the operator call
    de.fraunhofer.aisec.cpg.graph.Expression rhs =
        (de.fraunhofer.aisec.cpg.graph.Expression) handle(assignExpr.getValue());

    BinaryOperator binaryOperator =
        NodeBuilder.newBinaryOperator(assignExpr.getOperator().asString(), assignExpr.toString());

    binaryOperator.setLhs(lhs);
    binaryOperator.setRhs(rhs);
    binaryOperator.setType(lhs.getType());

    return binaryOperator;
  }

  // Not sure how to handle this exactly yet
  private DeclarationStatement handleVariableDeclarationExpr(Expression expr) {
    VariableDeclarationExpr variableDeclarationExpr = expr.asVariableDeclarationExpr();

    DeclarationStatement declarationStatement =
        NodeBuilder.newDeclarationStatement(variableDeclarationExpr.toString());

    for (VariableDeclarator variable : variableDeclarationExpr.getVariables()) {
      ResolvedValueDeclaration resolved = variable.resolve();

      VariableDeclaration declaration =
          NodeBuilder.newVariableDeclaration(
              resolved.getName(),
              this.lang.getTypeAsGoodAsPossible(variable, resolved),
              variable.toString());

      declaration
          .getType()
          .setTypeModifier(
              variableDeclarationExpr.getModifiers().stream()
                  .map(m -> m.getKeyword().asString())
                  .collect(Collectors.joining(" ")));

      Optional<Expression> oInitializer = variable.getInitializer();

      if (oInitializer.isPresent()) {
        de.fraunhofer.aisec.cpg.graph.Expression initializer =
            (de.fraunhofer.aisec.cpg.graph.Expression) handle(oInitializer.get());

        declaration.setInitializer(initializer);
      }
      lang.setCodeAndRegion(declaration, variable);
      declarationStatement.getDeclarations().add(declaration);

      lang.getScopeManager().addValueDeclaration(declaration);
    }

    return declarationStatement;
  }

  private de.fraunhofer.aisec.cpg.graph.Expression handleFieldAccessExpression(Expression expr) {
    FieldAccessExpr fieldAccessExpr = expr.asFieldAccessExpr();
    de.fraunhofer.aisec.cpg.graph.Expression member;
    de.fraunhofer.aisec.cpg.graph.Expression base;
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
        baseType = new Type(resolve.asField().declaringType().getQualifiedName());

      } catch (RuntimeException | NoClassDefFoundError ex) {
        isStaticAccess = true;
        String typeString = this.lang.recoverTypeFromUnsolvedException(ex);
        if (typeString != null) {
          baseType = new Type(typeString);
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
            baseType = new Type(qualifiedNameFromImports);
          } else {
            log.info("Unknown base type 1 for {}", fieldAccessExpr);
            baseType = Type.UNKNOWN;
          }
        }
      }
      if (isStaticAccess) {
        base =
            NodeBuilder.newStaticReferenceExpression(
                scope.asNameExpr().getNameAsString(), baseType, scope.toString());
      } else {
        base =
            NodeBuilder.newDeclaredReferenceExpression(
                scope.asNameExpr().getNameAsString(), baseType, scope.toString());
      }

      lang.setCodeAndRegion(base, fieldAccessExpr.getScope());
    } else if (scope.isFieldAccessExpr()) {
      base = (de.fraunhofer.aisec.cpg.graph.Expression) handle(scope);
      de.fraunhofer.aisec.cpg.graph.Expression tester = base;
      while (tester instanceof MemberExpression) {
        // we need to check if any base is only a static access, otherwise, this is a member access
        // to this base
        tester = (de.fraunhofer.aisec.cpg.graph.Expression) ((MemberExpression) tester).getBase();
      }
      if (tester instanceof StaticReferenceExpression) {
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
          baseType = new Type(qualifiedNameFromImports);
        } else {
          log.info("Unknown base type 2 for {}", fieldAccessExpr);
          baseType = Type.UNKNOWN;
        }
        base =
            NodeBuilder.newStaticReferenceExpression(
                scope.asFieldAccessExpr().getNameAsString(), baseType, scope.toString());
      }
      lang.setCodeAndRegion(base, fieldAccessExpr.getScope());
    } else {
      base = (de.fraunhofer.aisec.cpg.graph.Expression) handle(scope);
    }

    Type fieldType;
    try {
      ResolvedValueDeclaration symbol = fieldAccessExpr.resolve();
      fieldType = new Type(symbol.asField().getType().describe());
      member =
          NodeBuilder.newDeclaredReferenceExpression(
              fieldAccessExpr.getName().getIdentifier(), fieldType, fieldAccessExpr.toString());
    } catch (RuntimeException | NoClassDefFoundError ex) {
      String typeString = this.lang.recoverTypeFromUnsolvedException(ex);
      if (typeString != null) {
        fieldType = new Type(typeString);
      } else if (fieldAccessExpr.toString().endsWith(".length")) {
        fieldType = new Type("int");
      } else {
        log.info("Unknown field type for {}", fieldAccessExpr);
        fieldType = Type.UNKNOWN;
      }
      member =
          NodeBuilder.newStaticReferenceExpression(
              fieldAccessExpr.getName().getIdentifier(), fieldType, fieldAccessExpr.toString());
    }

    lang.setCodeAndRegion(member, fieldAccessExpr.getName());

    return NodeBuilder.newMemberExpression(base, member, fieldAccessExpr.toString());
  }

  private Literal handleLiteralExpression(Expression expr) {
    LiteralExpr literalExpr = expr.asLiteralExpr();

    String value = literalExpr.toString();
    if (literalExpr instanceof IntegerLiteralExpr) {
      return NodeBuilder.newLiteral(
          literalExpr.asIntegerLiteralExpr().asNumber(), Type.createFrom("int"), value);
    } else if (literalExpr instanceof StringLiteralExpr) {
      return NodeBuilder.newLiteral(
          literalExpr.asStringLiteralExpr().asString(), Type.createFrom("java.lang.String"), value);
    } else if (literalExpr instanceof BooleanLiteralExpr) {
      return NodeBuilder.newLiteral(
          literalExpr.asBooleanLiteralExpr().getValue(), Type.createFrom("boolean"), value);
    } else if (literalExpr instanceof CharLiteralExpr) {
      return NodeBuilder.newLiteral(
          literalExpr.asCharLiteralExpr().asChar(), Type.createFrom("char"), value);
    } else if (literalExpr instanceof DoubleLiteralExpr) {
      return NodeBuilder.newLiteral(
          literalExpr.asDoubleLiteralExpr().asDouble(), Type.createFrom("double"), value);
    } else if (literalExpr instanceof LongLiteralExpr) {
      return NodeBuilder.newLiteral(
          literalExpr.asLongLiteralExpr().asNumber(), Type.createFrom("long"), value);
    } else if (literalExpr instanceof NullLiteralExpr) {
      return NodeBuilder.newLiteral(null, Type.createFrom("null"), value);
    }

    return null;
  }

  private DeclaredReferenceExpression handleClassExpression(Expression expr) {
    ClassExpr classExpr = expr.asClassExpr();

    Type type = new Type(classExpr.getType().asString());

    DeclaredReferenceExpression thisExpression =
        NodeBuilder.newStaticReferenceExpression(
            classExpr.toString().substring(classExpr.toString().lastIndexOf('.') + 1),
            type,
            classExpr.toString());
    lang.setCodeAndRegion(thisExpression, classExpr);

    return thisExpression;
  }

  private DeclaredReferenceExpression handleThisExpression(Expression expr) {
    // TODO: use a separate ThisExpression (issue #8)
    ThisExpr thisExpr = expr.asThisExpr();
    ResolvedTypeDeclaration resolvedValueDeclaration = thisExpr.resolve();

    Type type = new Type(resolvedValueDeclaration.getQualifiedName());

    DeclaredReferenceExpression thisExpression =
        NodeBuilder.newDeclaredReferenceExpression(thisExpr.toString(), type, thisExpr.toString());
    lang.setCodeAndRegion(thisExpression, thisExpr);

    return thisExpression;
  }

  // TODO: this function needs a MAJOR overhaul!
  private de.fraunhofer.aisec.cpg.graph.Expression handleNameExpression(Expression expr) {
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
          expr.replace(fieldAccessExpr);

          // handle it as a field expression
          return (de.fraunhofer.aisec.cpg.graph.Expression) handle(fieldAccessExpr);
        } else {
          FieldAccessExpr fieldAccessExpr =
              new FieldAccessExpr(
                  new NameExpr(field.declaringType().getClassName()), field.getName());
          expr.replace(fieldAccessExpr);

          // handle it as a field expression
          return (de.fraunhofer.aisec.cpg.graph.Expression) handle(fieldAccessExpr);
        }
      } else {
        Type type = new Type(symbol.getType().describe());

        DeclaredReferenceExpression declaredReferenceExpression =
            NodeBuilder.newDeclaredReferenceExpression(symbol.getName(), type, nameExpr.toString());

        lang.getScopeManager().connectToLocal(declaredReferenceExpression);

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
        t = new Type("UNKNOWN3", Type.Origin.UNRESOLVED);
        log.info("Unresolved symbol: {}", nameExpr.getNameAsString());
      } else {
        t = new Type(typeString, Type.Origin.GUESSED);
      }

      DeclaredReferenceExpression declaredReferenceExpression =
          NodeBuilder.newDeclaredReferenceExpression(
              nameExpr.getNameAsString(), t, nameExpr.toString());

      lang.getScopeManager().connectToLocal(declaredReferenceExpression);

      return declaredReferenceExpression;
    } catch (RuntimeException | NoClassDefFoundError ex) {
      Type t = new Type("UNKNOWN4", Type.Origin.UNRESOLVED);
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
    de.fraunhofer.aisec.cpg.graph.Expression lhs =
        (de.fraunhofer.aisec.cpg.graph.Expression) handle(binaryExpr.getExpression());

    Type typeAsGoodAsPossible = this.lang.getTypeAsGoodAsPossible(binaryExpr.getType());

    // second, handle the value. this is the second argument of the operator call
    de.fraunhofer.aisec.cpg.graph.Expression rhs =
        NodeBuilder.newLiteral(
            typeAsGoodAsPossible.getTypeName(), new Type("class"), binaryExpr.getTypeAsString());

    BinaryOperator binaryOperator =
        NodeBuilder.newBinaryOperator("instanceof", binaryExpr.toString());

    binaryOperator.setLhs(lhs);
    binaryOperator.setRhs(rhs);

    return binaryOperator;
  }

  private UnaryOperator handleUnaryExpression(Expression expr) {
    UnaryExpr unaryExpr = expr.asUnaryExpr();

    // handle the 'inner' expression, which is affected by the unary expression
    de.fraunhofer.aisec.cpg.graph.Expression expression =
        (de.fraunhofer.aisec.cpg.graph.Expression) handle(unaryExpr.getExpression());

    UnaryOperator unaryOperator =
        NodeBuilder.newUnaryOperator(
            unaryExpr.getOperator().asString(),
            unaryExpr.isPostfix(),
            unaryExpr.isPrefix(),
            unaryExpr.toString());

    unaryOperator.setInput(expression);
    unaryOperator.setType(expression.getType());

    return unaryOperator;
  }

  private BinaryOperator handleBinaryExpression(Expression expr) {
    BinaryExpr binaryExpr = expr.asBinaryExpr();

    // first, handle the target. this is the first argument of the operator call
    de.fraunhofer.aisec.cpg.graph.Expression lhs =
        (de.fraunhofer.aisec.cpg.graph.Expression) handle(binaryExpr.getLeft());

    // second, handle the value. this is the second argument of the operator call
    de.fraunhofer.aisec.cpg.graph.Expression rhs =
        (de.fraunhofer.aisec.cpg.graph.Expression) handle(binaryExpr.getRight());

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
    // the scope could either be a variable or also the class name (static call!)
    // thus, only because the scope is present, this is not automatically a member call
    if (o.isPresent()) {
      Expression scope = o.get();
      // we need to check if there is a valuedecl corresponding to the base. this cannot easily be
      // done, but we can try to resolve the Expression, and if the Javaparser does not know about
      // it, we assume that this is a static call
      boolean isresolvable = false;
      String scopeName = null;
      try {
        if (scope instanceof NameExpr) {
          scopeName = ((NameExpr) scope).getNameAsString();
          ((NameExpr) scope).resolve();
          isresolvable = true;
        }
      } catch (UnsolvedSymbolException ex) {
        if (!ex.getName()
            .startsWith("We are unable to find the value declaration corresponding to")) {
          isresolvable = true;
        }
      } catch (RuntimeException | NoClassDefFoundError ex) {
        isresolvable = true;
      }
      if (isresolvable) {
        Statement base = handle(scope);

        callExpression =
            NodeBuilder.newMemberCallExpression(
                name, qualifiedName, base, methodCallExpr.toString());
      } else {
        callExpression =
            NodeBuilder.newStaticCallExpression(
                name, qualifiedName, methodCallExpr.toString(), scopeName);
      }
    } else {
      callExpression =
          NodeBuilder.newCallExpression(name, qualifiedName, methodCallExpr.toString());
    }

    String typeString = Type.UNKNOWN_TYPE_STRING;
    try {
      typeString = methodCallExpr.resolve().getReturnType().describe();
    } catch (Throwable e) {
      log.debug("Could not resolve return type for {}", methodCallExpr);
    }

    callExpression.setType(Type.createFrom(typeString));

    NodeList<Expression> arguments = methodCallExpr.getArguments();

    // handle the arguments
    for (int i = 0; i < arguments.size(); i++) {
      de.fraunhofer.aisec.cpg.graph.Expression argument =
          (de.fraunhofer.aisec.cpg.graph.Expression) handle(arguments.get(i));

      argument.setArgumentIndex(i);

      callExpression.getArguments().add(argument);
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
      de.fraunhofer.aisec.cpg.graph.Expression argument =
          (de.fraunhofer.aisec.cpg.graph.Expression) handle(arguments.get(i));

      argument.setArgumentIndex(i);

      ctor.getArguments().add(argument);
    }

    newExpression.setInitializer(ctor);
    return newExpression;
  }
}
