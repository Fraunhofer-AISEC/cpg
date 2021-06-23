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
import de.fraunhofer.aisec.cpg.graph.*;
import de.fraunhofer.aisec.cpg.graph.declarations.Declaration;
import de.fraunhofer.aisec.cpg.graph.declarations.ValueDeclaration;
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*;
import de.fraunhofer.aisec.cpg.graph.types.*;
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.eclipse.cdt.core.dom.ast.*;
import org.eclipse.cdt.core.dom.ast.IBasicType.Kind;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTDesignator;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTExpression;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTInitializerClause;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPTemplateArgument;
import org.eclipse.cdt.internal.core.dom.parser.CStringValue;
import org.eclipse.cdt.internal.core.dom.parser.ProblemBinding;
import org.eclipse.cdt.internal.core.dom.parser.ProblemType;
import org.eclipse.cdt.internal.core.dom.parser.cpp.*;
import org.eclipse.cdt.internal.core.dom.parser.cpp.semantics.TypeOfDependentExpression;

class ExpressionHandler extends Handler<Expression, IASTInitializerClause, CXXLanguageFrontend> {
  /*
   Note: CDT expresses hierarchies in Interfaces to allow to have multi-inheritance in java. Because some Expressions
   have subelements of type IASTInitalizerClause and in the hierarchy IASTExpression extends IASTInitializerClause.
   The later is the appropriate Interface type for the handler.
  */

  ExpressionHandler(CXXLanguageFrontend lang) {
    super(Expression::new, lang);

    map.put(
        CPPASTLiteralExpression.class,
        ctx -> handleLiteralExpression((CPPASTLiteralExpression) ctx));
    map.put(
        CPPASTBinaryExpression.class, ctx -> handleBinaryExpression((CPPASTBinaryExpression) ctx));
    map.put(CPPASTUnaryExpression.class, ctx -> handleUnaryExpression((CPPASTUnaryExpression) ctx));
    map.put(
        CPPASTConditionalExpression.class,
        ctx -> handleConditionalExpression((CPPASTConditionalExpression) ctx));
    map.put(CPPASTIdExpression.class, ctx -> handleIdExpression((CPPASTIdExpression) ctx));
    map.put(CPPASTFieldReference.class, ctx -> handleFieldReference((CPPASTFieldReference) ctx));
    map.put(
        CPPASTFunctionCallExpression.class,
        ctx -> handleFunctionCallExpression((CPPASTFunctionCallExpression) ctx));
    map.put(CPPASTCastExpression.class, ctx -> handleCastExpression((CPPASTCastExpression) ctx));
    map.put(
        CPPASTSimpleTypeConstructorExpression.class,
        ctx -> handleSimpleTypeConstructorExpression((CPPASTSimpleTypeConstructorExpression) ctx));
    map.put(CPPASTNewExpression.class, ctx -> handleNewExpression((CPPASTNewExpression) ctx));
    map.put(CPPASTInitializerList.class, ctx -> handleInitializerList((CPPASTInitializerList) ctx));
    map.put(
        CPPASTDesignatedInitializer.class,
        ctx -> handleDesignatedInitializer((CPPASTDesignatedInitializer) ctx));
    map.put(CPPASTExpressionList.class, ctx -> handleExpressionList((CPPASTExpressionList) ctx));
    map.put(
        CPPASTDeleteExpression.class, ctx -> handleDeleteExpression((CPPASTDeleteExpression) ctx));
    map.put(
        CPPASTArraySubscriptExpression.class,
        ctx -> handleArraySubscriptExpression((CPPASTArraySubscriptExpression) ctx));
    map.put(
        CPPASTTypeIdExpression.class, ctx -> handleTypeIdExpression((CPPASTTypeIdExpression) ctx));
    map.put(
        CPPASTCompoundStatementExpression.class,
        ctx -> handleCompoundStatementExpression((CPPASTCompoundStatementExpression) ctx));
  }

  /**
   * Tries to return the {@link IType} for a given AST expression. In case this fails, the constant
   * type {@link ProblemType#UNKNOWN_FOR_EXPRESSION} is returned.
   *
   * @param expression the ast expression
   * @return a CDT type
   */
  private IType expressionTypeProxy(ICPPASTExpression expression) {
    IType expressionType = ProblemType.UNKNOWN_FOR_EXPRESSION;

    try {
      expressionType = expression.getExpressionType();
    } catch (AssertionError e) {
      String codeFromRawNode = lang.getCodeFromRawNode(expression);
      warnWithFileLocation(lang, expression, log, "Unknown Expression Type: {}", codeFromRawNode);
    }

    return expressionType;
  }

  private Expression handleCompoundStatementExpression(CPPASTCompoundStatementExpression ctx) {
    CompoundStatementExpression cse =
        NodeBuilder.newCompoundStatementExpression(ctx.getRawSignature());
    cse.setStatement(this.lang.getStatementHandler().handle(ctx.getCompoundStatement()));
    return cse;
  }

  private TypeIdExpression handleTypeIdExpression(CPPASTTypeIdExpression ctx) {
    // Eclipse CDT seems to support the following operators
    // 0 sizeof
    // 1 typeid
    // 2 alignof
    // 3 typeof
    // 22 sizeof... (however does not really work)
    // there are a lot of other constants defined for type traits, but they are not really parsed as
    // type id expressions

    String operatorCode = "";
    Type type = UnknownType.getUnknownType();
    switch (ctx.getOperator()) {
      case IASTTypeIdExpression.op_sizeof:
        operatorCode = "sizeof";
        type = TypeParser.createFrom("std::size_t", true);
        break;
      case IASTTypeIdExpression.op_typeid:
        operatorCode = "typeid";
        type = TypeParser.createFrom("const std::type_info&", true);
        break;
      case IASTTypeIdExpression.op_alignof:
        operatorCode = "alignof";
        type = TypeParser.createFrom("std::size_t", true);
        break;
      case IASTTypeIdExpression.op_typeof:
        // typeof is not an official c++ keyword - not sure why eclipse supports it
        operatorCode = "typeof";
        // not really sure if this really has a type
        break;
      default:
        log.debug("Unknown typeid operator code: {}", ctx.getOperator());
    }

    // TODO: proper type resolve
    Type referencedType =
        TypeParser.createFrom(ctx.getTypeId().getDeclSpecifier().toString(), true, lang);

    return NodeBuilder.newTypeIdExpression(
        operatorCode, type, referencedType, ctx.getRawSignature());
  }

  private Expression handleArraySubscriptExpression(CPPASTArraySubscriptExpression ctx) {
    ArraySubscriptionExpression arraySubsExpression =
        NodeBuilder.newArraySubscriptionExpression(ctx.getRawSignature());
    arraySubsExpression.setArrayExpression(handle(ctx.getArrayExpression()));
    arraySubsExpression.setSubscriptExpression(handle(ctx.getArgument()));
    return arraySubsExpression;
  }

  private Expression handleNewExpression(CPPASTNewExpression ctx) {
    String name = ctx.getTypeId().getDeclSpecifier().toString();
    String code = ctx.getRawSignature();

    // TODO: obsolete?
    Type t = TypeParser.createFrom(expressionTypeProxy(ctx).toString(), true, lang);
    t.reference(PointerType.PointerOrigin.ARRAY);

    Expression expr;

    IASTInitializer init = ctx.getInitializer();

    // we need to check, whether this is an array initialization or a single new expression
    if (ctx.isArrayAllocation()) {
      t.reference(PointerType.PointerOrigin.ARRAY);

      var arrayMods =
          ((IASTArrayDeclarator) ctx.getTypeId().getAbstractDeclarator()).getArrayModifiers();

      var arrayCreate = NodeBuilder.newArrayCreationExpression(code);

      arrayCreate.setType(t);

      for (var arrayMod : arrayMods) {
        arrayCreate.addDimension(this.handle(arrayMod.getConstantExpression()));
      }

      if (init != null) {
        arrayCreate.setInitializer(this.lang.getInitializerHandler().handle(init));
      }

      expr = arrayCreate;
    } else {
      t.reference(PointerType.PointerOrigin.POINTER);

      var newExpression = NodeBuilder.newNewExpression(code, t);

      if (init != null) {
        newExpression.setInitializer(this.lang.getInitializerHandler().handle(init));
      }

      expr = newExpression;
    }

    // try to actually resolve the type
    IASTDeclSpecifier declSpecifier = ctx.getTypeId().getDeclSpecifier();

    if (declSpecifier instanceof CPPASTNamedTypeSpecifier) {
      IBinding binding = ((CPPASTNamedTypeSpecifier) declSpecifier).getName().resolveBinding();

      if (binding != null && !(binding instanceof CPPScope.CPPScopeProblem)) {
        // update the type
        Type type = TypeParser.createFrom(binding.getName(), true, lang);
        if (((CPPASTNamedTypeSpecifier) declSpecifier).getName() instanceof CPPASTTemplateId
            && type instanceof ObjectType) {
          ((ObjectType) type)
              .addGenerics(
                  getTemplateTypeArguments(
                      (CPPASTTemplateId) ((CPPASTNamedTypeSpecifier) declSpecifier).getName()));
        }
        newExpression.setType(type);
      } else {
        log.debug(
            "Could not resolve binding of type {} for {}, it is probably defined somewhere externally",
            name,
            expr);
      }
    }
    if (((CPPASTNamedTypeSpecifier) declSpecifier).getName() instanceof CPPASTTemplateId) {
      List<Node> templateParameters =
          getTemplateArguments(
              (CPPASTTemplateId) ((CPPASTNamedTypeSpecifier) declSpecifier).getName());
      newExpression.setTemplateParameters(templateParameters);
    }

    IASTInitializer init = ctx.getInitializer();

    if (init != null) {
      Expression initializer = this.lang.getInitializerHandler().handle(init);
      newExpression.setInitializer(initializer);
    }

    return newExpression;
  }

  /**
   * Gets all the types from the provided TypeExpressions that were used to instantiate the template
   *
   * @param template
   * @return List of Types declared by the TypeExpressions included in the template instantiation
   */
  private List<Type> getTemplateTypeArguments(CPPASTTemplateId template) {
    List<Type> typeArguments = new ArrayList<>();
    for (Node argument : getTemplateArguments(template)) {
      if (argument instanceof TypeExpression) {
        typeArguments.add(((TypeExpression) argument).getType());
      }
    }
    return typeArguments;
  }

  /**
   * Gets all arguments a template was instantiated with. Note, that the arguments can either be
   * Expressions referring to a value ot TypeExpressions referring to a type.
   *
   * @param template
   * @return List of Nodes containing the all the arguments the template was instantiated with.
   */
  private List<Node> getTemplateArguments(CPPASTTemplateId template) {
    List<Node> templateArguments = new ArrayList<>();
    for (IASTNode argument : template.getTemplateArguments()) {
      if (argument instanceof CPPASTTypeId) {
        Type type =
            TypeParser.createFrom(((CPPASTTypeId) argument).getDeclSpecifier().toString(), true);
        templateArguments.add(NodeBuilder.newTypeExpression(type.getName(), type));
      } else if (argument instanceof CPPASTLiteralExpression) {
        templateArguments.add(lang.getExpressionHandler().handle((IASTInitializerClause) argument));
      }
    }
    return templateArguments;
  }

  private ConditionalExpression handleConditionalExpression(CPPASTConditionalExpression ctx) {
    Expression condition = handle(ctx.getLogicalConditionExpression());
    return NodeBuilder.newConditionalExpression(
        condition,
        ctx.getPositiveResultExpression() != null
            ? handle(ctx.getPositiveResultExpression())
            : condition,
        handle(ctx.getNegativeResultExpression()),
        TypeParser.createFrom(expressionTypeProxy(ctx).toString(), true, lang));
  }

  private DeleteExpression handleDeleteExpression(CPPASTDeleteExpression ctx) {
    DeleteExpression deleteExpression = NodeBuilder.newDeleteExpression(ctx.getRawSignature());
    for (IASTImplicitDestructorName name : ctx.getImplicitDestructorNames()) {
      log.debug("Implicit constructor name {}", name);
    }
    deleteExpression.setOperand(handle(ctx.getOperand()));
    return deleteExpression;
  }

  private Expression handleCastExpression(CPPASTCastExpression ctx) {
    CastExpression castExpression = NodeBuilder.newCastExpression(ctx.getRawSignature());
    castExpression.setExpression(this.handle(ctx.getOperand()));
    castExpression.setCastOperator(ctx.getOperator());

    Type castType;
    IType iType = expressionTypeProxy(ctx);
    if (iType instanceof CPPPointerType) {
      CPPPointerType pointerType = (CPPPointerType) iType;
      if (pointerType.getType() instanceof IProblemType) {
        // fall back to fTypeId
        castType =
            TypeParser.createFrom(ctx.getTypeId().getDeclSpecifier().toString() + "*", true, lang);
      } else {
        castType = TypeParser.createFrom(pointerType.getType().toString() + "*", true, lang);
      }
    } else if (iType instanceof IProblemType) {
      // fall back to fTypeId
      castType = TypeParser.createFrom(ctx.getTypeId().getDeclSpecifier().toString(), true, lang);
      // TODO: try to actually resolve the type (similar to NewExpression) using
      // ((CPPASTNamedTypeSpecifier) declSpecifier).getName().resolveBinding()
    } else {
      castType = TypeParser.createFrom(expressionTypeProxy(ctx).toString(), true, lang);
    }

    castExpression.setCastType(castType);

    if (TypeManager.getInstance().isPrimitive(castExpression.getCastType())
        || ctx.getOperator() == 4) {
      castExpression.setType(castExpression.getCastType());
    } else {
      castExpression.getExpression().registerTypeListener(castExpression);
    }
    return castExpression;
  }

  private Expression handleSimpleTypeConstructorExpression(
      CPPASTSimpleTypeConstructorExpression ctx) {
    CastExpression castExpression = NodeBuilder.newCastExpression(ctx.getRawSignature());

    castExpression.setExpression(this.lang.getInitializerHandler().handle(ctx.getInitializer()));
    castExpression.setCastOperator(0); // cast

    Type castType;
    if (expressionTypeProxy(ctx) instanceof CPPPointerType) {
      CPPPointerType pointerType = (CPPPointerType) expressionTypeProxy(ctx);
      castType = TypeParser.createFrom(pointerType.getType().toString() + "*", true, lang);
    } else {
      castType = TypeParser.createFrom(expressionTypeProxy(ctx).toString(), true, lang);
    }

    castExpression.setCastType(castType);

    if (TypeManager.getInstance().isPrimitive(castExpression.getCastType())) {
      castExpression.setType(castExpression.getCastType());
    } else {
      castExpression.getExpression().registerTypeListener(castExpression);
    }
    return castExpression;
  }

  private Expression handleFieldReference(CPPASTFieldReference ctx) {
    Expression base = this.handle(ctx.getFieldOwner());
    // Replace Literal this with a reference pointing to this
    if (base instanceof Literal && ((Literal<?>) base).getValue().equals("this")) {
      PhysicalLocation location = base.getLocation();

      var recordDeclaration = lang.getScopeManager().getCurrentRecord();

      base =
          NodeBuilder.newDeclaredReferenceExpression(
              "this",
              recordDeclaration != null
                  ? recordDeclaration.getThis().getType()
                  : UnknownType.getUnknownType(),
              base.getCode());
      base.setLocation(location);
    }

    MemberExpression memberExpression =
        NodeBuilder.newMemberExpression(
            base,
            UnknownType.getUnknownType(),
            ctx.getFieldName().toString(),
            ctx.isPointerDereference() ? "->" : ".",
            ctx.getRawSignature());

    this.lang.expressionRefersToDeclaration(memberExpression, ctx);

    return memberExpression;
  }

  private Expression handleUnaryExpression(CPPASTUnaryExpression ctx) {

    Expression input = null;
    if (ctx.getOperand() != null) { // can be null e.g. for "throw;"
      input = this.handle(ctx.getOperand());
    }

    String operatorCode = "";
    switch (ctx.getOperator()) {
      case IASTUnaryExpression.op_prefixIncr:
      case IASTUnaryExpression.op_postFixIncr:
        operatorCode = "++";
        break;
      case IASTUnaryExpression.op_prefixDecr:
      case IASTUnaryExpression.op_postFixDecr:
        operatorCode = "--";
        break;
      case IASTUnaryExpression.op_plus:
        operatorCode = "+";
        break;
      case IASTUnaryExpression.op_minus:
        operatorCode = "-";
        break;
      case IASTUnaryExpression.op_star:
        operatorCode = "*";
        break;
      case IASTUnaryExpression.op_amper:
        operatorCode = "&";
        break;
      case IASTUnaryExpression.op_tilde:
        operatorCode = "~";
        break;
      case IASTUnaryExpression.op_not:
        operatorCode = "!";
        break;
      case IASTUnaryExpression.op_sizeof:
        operatorCode = "sizeof";
        break;
      case IASTUnaryExpression.op_bracketedPrimary:
        // ignore this kind of expression and return the input directly
        // operatorCode = "()";
        // break;
        return input;
      case IASTUnaryExpression.op_throw:
        operatorCode = "throw";
        break;
      case IASTUnaryExpression.op_typeid:
        operatorCode = "typeid";
        break;
      case IASTUnaryExpression.op_alignOf:
        operatorCode = "alignof";
        break;
      case IASTUnaryExpression.op_sizeofParameterPack:
        operatorCode = "sizeof...";
        break;
      case IASTUnaryExpression.op_noexcept:
        operatorCode = "noexcept";
        break;
      case IASTUnaryExpression.op_labelReference:
        operatorCode = "";
        break;
      default:
        errorWithFileLocation(this.lang, ctx, log, "unknown operator {}", ctx.getOperator());
    }

    UnaryOperator unaryOperator =
        NodeBuilder.newUnaryOperator(
            operatorCode, ctx.isPostfixOperator(), !ctx.isPostfixOperator(), ctx.getRawSignature());

    if (input != null) {
      unaryOperator.setInput(input);
    }

    return unaryOperator;
  }

  private CallExpression handleFunctionCallExpression(CPPASTFunctionCallExpression ctx) {
    Expression reference = this.handle(ctx.getFunctionNameExpression());

    CallExpression callExpression;
    if (reference instanceof MemberExpression) {
      String baseTypename;
      // Pointer types contain * or []. We do not want that here.
      Type baseType = ((MemberExpression) reference).getBase().getType().getRoot();
      assert !(baseType instanceof SecondOrderType);
      baseTypename = baseType.getTypeName();
      DeclaredReferenceExpression member =
          NodeBuilder.newDeclaredReferenceExpression(
              reference.getName(), UnknownType.getUnknownType(), reference.getName());

      member.setLocation(lang.getLocationFromRawNode(reference));

      callExpression =
          NodeBuilder.newMemberCallExpression(
              member.getName(),
              baseTypename + "." + member.getName(),
              ((MemberExpression) reference).getBase(),
              member,
              ((MemberExpression) reference).getOperatorCode(),
              ctx.getRawSignature());

      if (((CPPASTFieldReference) ctx.getFunctionNameExpression()).getFieldName()
          instanceof CPPASTTemplateId) {
        // Make necessary adjustments if we are handling a function template
        String name =
            ((CPPASTTemplateId)
                    ((CPPASTFieldReference) ctx.getFunctionNameExpression()).getFieldName())
                .getTemplateName()
                .toString();

        callExpression.setName(name);
        callExpression.addExplicitTemplateParameters(
            getTemplateArguments(
                (CPPASTTemplateId)
                    ((CPPASTFieldReference) ctx.getFunctionNameExpression()).getFieldName()));
      }

    } else if (reference instanceof BinaryOperator
        && ((BinaryOperator) reference).getOperatorCode().equals(".")) {
      // We have a dot operator that was not classified as a member expression. This happens when
      // dealing with function pointer calls that happen on an explicit object
      callExpression =
          NodeBuilder.newMemberCallExpression(
              reference.getCode(),
              "",
              ((BinaryOperator) reference).getLhs(),
              ((BinaryOperator) reference).getRhs(),
              ((BinaryOperator) reference).getOperatorCode(),
              reference.getCode());
    } else if (reference instanceof UnaryOperator
        && ((UnaryOperator) reference).getOperatorCode().equals("*")) {
      // Classic C-style function pointer call -> let's extract the target
      callExpression =
          NodeBuilder.newCallExpression(
              ((UnaryOperator) reference).getInput().getName(), "", reference.getCode(), false);
    } else if (((CPPASTIdExpression) ctx.getFunctionNameExpression()).getName()
        instanceof CPPASTTemplateId) {
      String name =
          ((CPPASTTemplateId) ((CPPASTIdExpression) ctx.getFunctionNameExpression()).getName())
              .getTemplateName()
              .toString();
      callExpression = NodeBuilder.newCallExpression(name, name, ctx.getRawSignature(), true);

      callExpression.addExplicitTemplateParameters(
          getTemplateArguments(
              (CPPASTTemplateId) ((CPPASTIdExpression) ctx.getFunctionNameExpression()).getName()));

    } else {
      String fqn = reference.getName();
      String name = fqn;
      if (name.contains("::")) {
        name = name.substring(name.lastIndexOf("::") + 2);
      }
      if (name.contains("<")) {
        // The characters < and > are not allowed in identifier names, as they denote the usage of a
        // template
        name = name.substring(0, name.indexOf("<"));
      }
      fqn = fqn.replace("::", ".");
      // FIXME this is only true if we are in a namespace! If we are in a class, this is wrong!
      //  happens again in l367
      // String fullNamePrefix = lang.getScopeManager().getFullNamePrefix();
      // if (!fullNamePrefix.isEmpty()) {
      //  fqn = fullNamePrefix + "." + fqn;
      // }
      callExpression = NodeBuilder.newCallExpression(name, fqn, ctx.getRawSignature(), false);
    }

    int i = 0;
    for (IASTInitializerClause argument : ctx.getArguments()) {
      Expression arg = this.handle(argument);
      arg.setArgumentIndex(i);

      callExpression.addArgument(arg);

      i++;
    }

    // Important: we don't really need the reference node, but even its temporary creation might
    // leave unwanted artifacts behind in the final graph!
    reference.disconnectFromGraph();
    return callExpression;
  }

  private DeclaredReferenceExpression handleIdExpression(CPPASTIdExpression ctx) {
    DeclaredReferenceExpression declaredReferenceExpression =
        NodeBuilder.newDeclaredReferenceExpression(
            ctx.getName().toString(), UnknownType.getUnknownType(), ctx.getRawSignature());

    IType proxy = expressionTypeProxy(ctx);

    if (proxy instanceof ProblemType
        || (proxy instanceof IQualifierType
            && ((IQualifierType) proxy).getType() instanceof ProblemType)
        || proxy instanceof TypeOfDependentExpression) {
      deduceTypeManually(ctx, declaredReferenceExpression);
    } else {
      if (proxy instanceof CPPClassInstance) {
        // Handle Template Types separately
        handleTemplateTypeOfDeclaredReferenceExpression(proxy, declaredReferenceExpression);
      } else {
        declaredReferenceExpression.setType(
            TypeParser.createFrom(expressionTypeProxy(ctx).toString(), true, lang));
      }
    }

    /* this expression could actually be a field / member expression, but somehow CDT only recognizes them as a member expression if it has an explicit 'this'
     */
    // TODO: handle this? convert the declared reference expression into a member expression?

    this.lang.expressionRefersToDeclaration(declaredReferenceExpression, ctx);

    return declaredReferenceExpression;
  }

  /**
   * Sets the type of the DeclaredReferenceExpression
   *
   * @param ctx
   * @param declaredReferenceExpression
   */
  private void deduceTypeManually(
      CPPASTIdExpression ctx, DeclaredReferenceExpression declaredReferenceExpression) {
    log.debug("CDT could not deduce type. Trying manually");

    IBinding binding = ctx.getName().resolveBinding();
    Declaration declaration = this.lang.getCachedDeclaration(binding);
    if (declaration != null) {
      if (declaration instanceof ValueDeclaration) {
        declaredReferenceExpression.setType(((ValueDeclaration) declaration).getType());
      } else {
        log.debug("Unknown declaration type, setting to UNKNOWN");
        declaredReferenceExpression.setType(UnknownType.getUnknownType());
      }
    } else {
      log.debug("Could not deduce type manually, setting to UNKNOWN");
      declaredReferenceExpression.setType(UnknownType.getUnknownType());
    }
  }

  /**
   * Sets type of DeclaredReferenceExpression if type represents a template
   *
   * @param proxy
   * @param declaredReferenceExpression
   */
  private void handleTemplateTypeOfDeclaredReferenceExpression(
      IType proxy, DeclaredReferenceExpression declaredReferenceExpression) {
    ObjectType type =
        (ObjectType)
            TypeParser.createFrom(
                ((CPPClassInstance) proxy).getTemplateDefinition().toString(), true);
    for (ICPPTemplateArgument templateArgument :
        ((CPPClassInstance) proxy).getTemplateArguments()) {
      if (templateArgument instanceof CPPTemplateTypeArgument) {
        type.addGeneric(TypeParser.createFrom(templateArgument.toString(), true));
      }
    }
    declaredReferenceExpression.setType(type);
  }

  private ExpressionList handleExpressionList(CPPASTExpressionList exprList) {
    ExpressionList expressionList = NodeBuilder.newExpressionList(exprList.getRawSignature());
    for (IASTExpression expr : exprList.getExpressions()) {
      expressionList.addExpression(handle(expr));
    }

    return expressionList;
  }

  private BinaryOperator handleBinaryExpression(CPPASTBinaryExpression ctx) {
    String operatorCode = "";
    switch (ctx.getOperator()) {
      case IASTBinaryExpression.op_multiply:
        operatorCode = "*";
        break;
      case IASTBinaryExpression.op_divide:
        operatorCode = "/";
        break;
      case IASTBinaryExpression.op_modulo:
        operatorCode = "%";
        break;
      case IASTBinaryExpression.op_plus:
        operatorCode = "+";
        break;
      case IASTBinaryExpression.op_minus:
        operatorCode = "-";
        break;
      case IASTBinaryExpression.op_shiftLeft:
        operatorCode = "<<";
        break;
      case IASTBinaryExpression.op_shiftRight:
        operatorCode = ">>";
        break;
      case IASTBinaryExpression.op_lessThan:
        operatorCode = "<";
        break;
      case IASTBinaryExpression.op_greaterThan:
        operatorCode = ">";
        break;
      case IASTBinaryExpression.op_lessEqual:
        operatorCode = "<=";
        break;
      case IASTBinaryExpression.op_greaterEqual:
        operatorCode = ">=";
        break;
      case IASTBinaryExpression.op_binaryAnd:
        operatorCode = "&";
        break;
      case IASTBinaryExpression.op_binaryXor:
        operatorCode = "^";
        break;
      case IASTBinaryExpression.op_binaryOr:
        operatorCode = "|";
        break;
      case IASTBinaryExpression.op_logicalAnd:
        operatorCode = "&&";
        break;
      case IASTBinaryExpression.op_logicalOr:
        operatorCode = "||";
        break;
      case IASTBinaryExpression.op_assign:
        operatorCode = "=";
        break;
      case IASTBinaryExpression.op_multiplyAssign:
        operatorCode = "*=";
        break;
      case IASTBinaryExpression.op_divideAssign:
        operatorCode = "/=";
        break;
      case IASTBinaryExpression.op_moduloAssign:
        operatorCode = "%=";
        break;
      case IASTBinaryExpression.op_plusAssign:
        operatorCode = "+=";
        break;
      case IASTBinaryExpression.op_minusAssign:
        operatorCode = "-=";
        break;
      case IASTBinaryExpression.op_shiftLeftAssign:
        operatorCode = "<<=";
        break;
      case IASTBinaryExpression.op_shiftRightAssign:
        operatorCode = ">>=";
        break;
      case IASTBinaryExpression.op_binaryAndAssign:
        operatorCode = "&=";
        break;
      case IASTBinaryExpression.op_binaryXorAssign:
        operatorCode = "^=";
        break;
      case IASTBinaryExpression.op_binaryOrAssign:
        operatorCode = "|=";
        break;
      case IASTBinaryExpression.op_equals:
        operatorCode = "==";
        break;
      case IASTBinaryExpression.op_notequals:
        operatorCode = "!=";
        break;
      case IASTBinaryExpression.op_pmdot:
        operatorCode = ".";
        break;
      case IASTBinaryExpression.op_pmarrow:
        operatorCode = "->";
        break;
      case IASTBinaryExpression.op_max:
        operatorCode = ">?";
        break;
      case IASTBinaryExpression.op_min:
        operatorCode = "?<";
        break;
      case IASTBinaryExpression.op_ellipses:
        operatorCode = "...";
        break;
      default:
        errorWithFileLocation(this.lang, ctx, log, "unknown operator {}", ctx.getOperator());
    }
    BinaryOperator binaryOperator =
        NodeBuilder.newBinaryOperator(operatorCode, ctx.getRawSignature());

    Expression lhs = this.handle(ctx.getOperand1());

    Expression rhs;
    if (ctx.getOperand2() != null) {
      rhs = this.handle(ctx.getOperand2());
    } else {
      rhs = this.handle(ctx.getInitOperand2());
    }

    binaryOperator.setLhs(lhs);
    binaryOperator.setRhs(rhs);

    IType expressionType = expressionTypeProxy(ctx);

    if (expressionType == null
        || expressionType instanceof ProblemType
        || expressionType instanceof ProblemBinding) {
      log.debug("CDT could not deduce type. Type is set to null");
    } else if (expressionType instanceof TypeOfDependentExpression) {
      log.debug("Type of Expression depends on the type the template is initialized with");
      binaryOperator.setType(UnknownType.getUnknownType());
    } else {
      binaryOperator.setType(
          TypeParser.createFrom(expressionTypeProxy(ctx).toString(), true, lang));
    }

    return binaryOperator;
  }

  private Literal handleLiteralExpression(CPPASTLiteralExpression ctx) {
    IType type = expressionTypeProxy(ctx);
    IValue value = ctx.getEvaluation().getValue();

    Type generatedType = TypeParser.createFrom(type.toString(), true, lang);
    if (value.numberValue() == null // e.g. for 0x1p-52
        && !(value instanceof CStringValue)) {
      return NodeBuilder.newLiteral(value.toString(), generatedType, ctx.getRawSignature());
    }

    if (type instanceof CPPBasicType && ((CPPBasicType) type).getKind() == Kind.eInt) {
      return handleIntegerLiteral(ctx);
    } else if (type.isSameType(CPPBasicType.BOOLEAN)) {
      return NodeBuilder.newLiteral(
          value.numberValue().intValue() == 1, generatedType, ctx.getRawSignature());
    } else if (value instanceof CStringValue) {
      return NodeBuilder.newLiteral(
          ((CStringValue) value).cStringValue(), generatedType, ctx.getRawSignature());
    } else if (type instanceof CPPBasicType && ((CPPBasicType) type).getKind() == Kind.eFloat) {
      return NodeBuilder.newLiteral(
          value.numberValue().floatValue(), generatedType, ctx.getRawSignature());
    } else if (type instanceof CPPBasicType && ((CPPBasicType) type).getKind() == Kind.eDouble) {
      return NodeBuilder.newLiteral(
          value.numberValue().doubleValue(), generatedType, ctx.getRawSignature());
    } else if (type instanceof CPPBasicType && ((CPPBasicType) type).getKind() == Kind.eChar) {
      return NodeBuilder.newLiteral(
          (char) value.numberValue().intValue(), generatedType, ctx.getRawSignature());
    }

    return NodeBuilder.newLiteral(value.toString(), generatedType, ctx.getRawSignature());
  }

  private InitializerListExpression handleInitializerList(CPPASTInitializerList ctx) {
    InitializerListExpression expression =
        NodeBuilder.newInitializerListExpression(ctx.getRawSignature());

    List<Expression> initializers = new ArrayList<>();

    for (ICPPASTInitializerClause clause : ctx.getClauses()) {
      initializers.add(this.handle(clause));
    }

    expression.setInitializers(initializers);

    return expression;
  }

  private DesignatedInitializerExpression handleDesignatedInitializer(
      CPPASTDesignatedInitializer ctx) {

    Expression rhs = handle(ctx.getOperand());
    ArrayList<Expression> lhs = new ArrayList<>();
    if (ctx.getDesignators().length == 0) {
      errorWithFileLocation(this.lang, ctx, log, "no designator found");
    } else {
      for (ICPPASTDesignator des : ctx.getDesignators()) {
        Expression oneLhs = null;
        if (des instanceof CPPASTArrayDesignator) {
          oneLhs = handle(((CPPASTArrayDesignator) des).getSubscriptExpression());
        } else if (des instanceof CPPASTFieldDesignator) {
          oneLhs =
              NodeBuilder.newDeclaredReferenceExpression(
                  ((CPPASTFieldDesignator) des).getName().toString(),
                  UnknownType.getUnknownType(),
                  des.getRawSignature());
        } else if (des instanceof CPPASTArrayRangeDesignator) {
          oneLhs =
              NodeBuilder.newArrayRangeExpression(
                  handle(((CPPASTArrayRangeDesignator) des).getRangeFloor()),
                  handle(((CPPASTArrayRangeDesignator) des).getRangeCeiling()),
                  des.getRawSignature());
        } else {
          errorWithFileLocation(
              this.lang, ctx, log, "Unknown designated lhs {}", des.getClass().toGenericString());
        }
        if (oneLhs != null) {
          lhs.add(oneLhs);
        }
      }
    }

    DesignatedInitializerExpression die =
        NodeBuilder.newDesignatedInitializerExpression(ctx.getRawSignature());
    die.setLhs(lhs);
    die.setRhs(rhs);
    return die;
  }

  private Literal handleIntegerLiteral(CPPASTLiteralExpression ctx) {
    String value = new String(ctx.getValue()).toLowerCase();

    BigInteger bigValue;

    String suffix = getSuffix(value);

    // first, strip the suffix from the value
    String strippedValue = value.substring(0, value.length() - suffix.length());

    // next, check for possible prefixes
    int radix = 10;
    int offset = 0;
    if (value.startsWith("0b")) {
      radix = 2; // binary
      offset = 2; // len("0b")
    } else if (value.startsWith("0x")) {
      radix = 16; // hex
      offset = 2; // len("0x")
    } else if (value.startsWith("0") && strippedValue.length() > 1) {
      radix = 8; // octal
      offset = 1; // len("0")
    }

    // strip the prefix
    strippedValue = strippedValue.substring(offset);

    // basically we parse everything as BigInteger and then decide what to do
    bigValue = new BigInteger(strippedValue, radix);

    Number numberValue;
    Type type;

    if (Objects.equals("ull", suffix) || Objects.equals("ul", suffix)) {
      // unsigned long (long) will always be represented as BigInteger
      numberValue = bigValue;
    } else if (Objects.equals("ll", suffix) || Objects.equals("l", suffix)) {
      // both long and long long can be represented in Java long, but only within Long.MAX_VALUE
      if (bigValue.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) > 0) {
        // keep it as BigInteger
        numberValue = bigValue;

        warnWithFileLocation(
            this.lang,
            ctx,
            log,
            "Integer literal {} is too large to represented in a signed type, interpreting it as unsigned.",
            ctx);
      } else {
        numberValue = bigValue.longValue();
      }
    } else {
      // no suffix, we just cast it to the appropriate signed type that is required, but only within
      // Long.MAX_VALUE
      if (bigValue.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) > 0) {
        // keep it as BigInteger
        numberValue = bigValue;

        warnWithFileLocation(
            this.lang,
            ctx,
            log,
            "Integer literal {} is too large to represented in a signed type, interpreting it as unsigned.",
            ctx);
      } else if (bigValue.longValue() > Integer.MAX_VALUE) {
        numberValue = bigValue.longValue();
      } else {
        numberValue = bigValue.intValue();
      }
    }

    // retrieve type based on stored Java number
    if (numberValue instanceof BigInteger) {
      // we follow the way clang/llvm handles this and this seems to always
      // be an unsigned long long, except if it is explicitly specified as ul
      type =
          Objects.equals("ul", suffix)
              ? TypeParser.createFrom("unsigned long", true)
              : TypeParser.createFrom("unsigned long long", true);
    } else if (numberValue instanceof Long) {
      // differentiate between long and long long
      type =
          Objects.equals("ll", suffix)
              ? TypeParser.createFrom("long long", true)
              : TypeParser.createFrom("long", true);
    } else {
      type = TypeParser.createFrom("int", true);
    }

    return NodeBuilder.newLiteral(numberValue, type, ctx.getRawSignature());
  }

  private String getSuffix(String value) {
    String suffix = "";

    // maximum suffix length is 3
    for (int i = 1; i <= 3; i++) {
      String digit = value.substring(Math.max(0, value.length() - i));
      if (digit.chars().allMatch(character -> character == 'u' || character == 'l')) {
        suffix = digit;
      } else {
        break;
      }
    }

    return suffix;
  }
}
