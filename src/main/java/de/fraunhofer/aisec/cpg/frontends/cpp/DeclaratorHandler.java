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

import de.fraunhofer.aisec.cpg.frontends.Handler;
import de.fraunhofer.aisec.cpg.graph.*;
import de.fraunhofer.aisec.cpg.graph.type.FunctionPointerType;
import de.fraunhofer.aisec.cpg.graph.type.Type;
import de.fraunhofer.aisec.cpg.graph.type.TypeParser;
import de.fraunhofer.aisec.cpg.graph.type.UnknownType;
import de.fraunhofer.aisec.cpg.passes.scopes.RecordScope;
import de.fraunhofer.aisec.cpg.passes.scopes.Scope;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.eclipse.cdt.core.dom.ast.*;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTCompositeTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTParameterDeclaration;
import org.eclipse.cdt.internal.core.dom.parser.cpp.*;

class DeclaratorHandler extends Handler<Declaration, IASTNameOwner, CXXLanguageFrontend> {

  DeclaratorHandler(CXXLanguageFrontend lang) {
    super(Declaration::new, lang);

    map.put(CPPASTDeclarator.class, ctx -> handleDeclarator((CPPASTDeclarator) ctx));
    map.put(CPPASTArrayDeclarator.class, ctx -> handleDeclarator((CPPASTDeclarator) ctx));
    map.put(
        CPPASTFunctionDeclarator.class,
        ctx -> handleFunctionDeclarator((CPPASTFunctionDeclarator) ctx));
    map.put(
        CPPASTCompositeTypeSpecifier.class,
        ctx -> handleCompositeTypeSpecifier((CPPASTCompositeTypeSpecifier) ctx));
  }

  private int getEvaluatedIntegerValue(IASTExpression exp) {
    try {
      Method method = exp.getClass().getMethod("getEvaluation");
      ICPPEvaluation evaluation = (ICPPEvaluation) method.invoke(exp);
      return evaluation.getValue().numberValue().intValue();
    } catch (Exception e) {
      return -1;
    }
  }

  private VariableDeclaration handleDeclarator(CPPASTDeclarator ctx) {
    // type will be filled out later
    VariableDeclaration declaration =
        NodeBuilder.newVariableDeclaration(
            ctx.getName().toString(), UnknownType.getUnknownType(), ctx.getRawSignature(), true);
    IASTInitializer init = ctx.getInitializer();

    if (init != null) {
      declaration.setInitializer(lang.getInitializerHandler().handle(init));
    }

    lang.getScopeManager().addValueDeclaration(declaration);
    return declaration;
  }

  private ValueDeclaration handleFunctionDeclarator(CPPASTFunctionDeclarator ctx) {
    // Attention! If this declarator has no name, this is not actually a new function but
    // rather a function pointer
    if (ctx.getName().toString().isEmpty()) {
      return handleFunctionPointer(ctx);
    }
    String name = ctx.getName().toString();

    FunctionDeclaration declaration;

    // check for function definitions that are really methods and constructors
    if (name.contains("::")) {
      String[] rr = name.split("::");

      String recordName = rr[0];
      String methodName = rr[1];

      declaration =
          NodeBuilder.newMethodDeclaration(
              methodName,
              ctx.getRawSignature(),
              false,
              this.lang.getRecordForName(recordName).orElse(null));
    } else {
      declaration = NodeBuilder.newFunctionDeclaration(name, ctx.getRawSignature());
    }
    lang.getScopeManager().enterScope(declaration);

    int i = 0;
    for (ICPPASTParameterDeclaration param : ctx.getParameters()) {
      ParamVariableDeclaration arg = lang.getParameterDeclarationHandler().handle(param);

      IBinding binding = ctx.getParameters()[i].getDeclarator().getName().resolveBinding();

      if (binding != null) {
        lang.cacheDeclaration(binding, arg);
      }

      arg.setArgumentIndex(i);
      // Note that this .addValueDeclaration call already adds arg to the function's parameters.
      // This is why the following line has been commented out by @KW
      lang.getScopeManager().addValueDeclaration(arg);
      // declaration.getParameters().add(arg);
      i++;
    }

    // Check for varargs. Note the difference to Java: here, we don't have a named array
    // containing the varargs, but they are rather treated as kind of an invisible arg list that is
    // appended to the original ones. For coherent graph behaviour, we introduce a dummy that
    // wraps this list
    if (ctx.takesVarArgs()) {
      ParamVariableDeclaration varargs =
          NodeBuilder.newMethodParameterIn("va_args", UnknownType.getUnknownType(), true, "");
      varargs.setArgumentIndex(i);
      lang.getScopeManager().addValueDeclaration(varargs);
    }

    //    lang.addFunctionDeclaration(declaration);
    lang.getScopeManager().leaveScope(declaration);
    return declaration;
  }

  private ValueDeclaration handleFunctionPointer(CPPASTFunctionDeclarator ctx) {
    Expression initializer =
        ctx.getInitializer() == null
            ? null
            : lang.getInitializerHandler().handle(ctx.getInitializer());
    // unfortunately we are not told whether this is a field or not, so we have to find it out
    // ourselves
    ValueDeclaration result;
    FunctionDeclaration currFunction = lang.getScopeManager().getCurrentFunction();
    if (currFunction != null) {
      // variable
      result =
          NodeBuilder.newVariableDeclaration(
              ctx.getNestedDeclarator().getName().toString(),
              UnknownType.getUnknownType(),
              ctx.getRawSignature(),
              true);
      ((VariableDeclaration) result).setInitializer(initializer);
      result.setLocation(lang.getLocationFromRawNode(ctx));
      result.setType(
          new FunctionPointerType(
              new Type.Qualifier(), Type.Storage.AUTO, new ArrayList<>(), null));
      result.refreshType();
    } else {
      RecordScope recordScope =
          (RecordScope) lang.getScopeManager().getFirstScopeThat(RecordScope.class::isInstance);
      // if (recordScope != null) {
      // field
      String code = ctx.getRawSignature();
      Pattern namePattern = Pattern.compile("\\((\\*|.+\\*)(?<name>[^)]*)");
      Matcher matcher = namePattern.matcher(code);
      String name = "";
      if (matcher.find()) {
        name = matcher.group("name").strip();
      }
      result =
          NodeBuilder.newFieldDeclaration(
              name,
              UnknownType.getUnknownType(),
              Collections.emptyList(),
              code,
              lang.getLocationFromRawNode(ctx),
              initializer,
              true);
      result.setLocation(lang.getLocationFromRawNode(ctx));
      result.setType(
          new FunctionPointerType(
              new Type.Qualifier(), Type.Storage.AUTO, new ArrayList<>(), null));
      result.refreshType();
      /*} else {
        // not in a record and not in a field, strange. This should not happen
        log.error(
            "Function pointer declaration that is neither in a function nor in a record. "
                + "This should not happen!");
        return null;
      }*/
    }
    return result;
  }

  private RecordDeclaration handleCompositeTypeSpecifier(CPPASTCompositeTypeSpecifier ctx) {
    String kind;
    switch (ctx.getKey()) {
      default:
      case IASTCompositeTypeSpecifier.k_struct:
        kind = "struct";
        break;
      case IASTCompositeTypeSpecifier.k_union:
        kind = "union";
        break;
      case ICPPASTCompositeTypeSpecifier.k_class:
        kind = "class";
        break;
    }
    RecordDeclaration recordDeclaration =
        NodeBuilder.newRecordDeclaration(
            lang.getScopeManager().getCurrentNamePrefixWithDelimiter() + ctx.getName().toString(),
            new ArrayList<>(),
            kind,
            ctx.getRawSignature());

    this.lang.addRecord(recordDeclaration);

    lang.getScopeManager().enterScope(recordDeclaration);

    if (kind.equals("class")) {
      de.fraunhofer.aisec.cpg.graph.FieldDeclaration thisDeclaration =
          NodeBuilder.newFieldDeclaration(
              "this",
              TypeParser.createFrom(ctx.getName().toString(), true),
              new ArrayList<>(),
              "this",
              null,
              null,
              true);
      recordDeclaration.getFields().add(thisDeclaration);
      lang.getScopeManager().addValueDeclaration(thisDeclaration);
    }

    for (IASTDeclaration member : ctx.getMembers()) {
      if (member instanceof CPPASTVisibilityLabel) {
        // TODO: parse visibility
        continue;
      }
      Declaration declaration = lang.getDeclarationHandler().handle(member);
      Scope declarationScope = lang.getScopeManager().getScopeOfStatment(declaration);

      if (declaration instanceof FunctionDeclaration) {
        MethodDeclaration method =
            MethodDeclaration.from((FunctionDeclaration) declaration, recordDeclaration);
        declaration.disconnectFromGraph();

        // check, if its a constructor
        if (declaration.getName().equals(recordDeclaration.getName())) {
          ConstructorDeclaration constructor = ConstructorDeclaration.from(method);
          if (declarationScope != null) {
            declarationScope.setAstNode(
                constructor); // Adjust cpg Node by which scopes are identified
          }
          recordDeclaration.getConstructors().add(constructor);
        } else {
          recordDeclaration.getMethods().add(method);
        }

        if (declarationScope != null) {
          declarationScope.setAstNode(method); // Adjust cpg Node by which scopes are identified
        }
      } else if (declaration instanceof VariableDeclaration) {
        recordDeclaration.getFields().add(FieldDeclaration.from((VariableDeclaration) declaration));
      } else if (declaration instanceof FieldDeclaration) {
        recordDeclaration.getFields().add((FieldDeclaration) declaration);
      } else if (declaration instanceof RecordDeclaration) {
        recordDeclaration.getRecords().add((RecordDeclaration) declaration);
      }
    }

    if (recordDeclaration.getConstructors().isEmpty()) {
      de.fraunhofer.aisec.cpg.graph.ConstructorDeclaration constructorDeclaration =
          NodeBuilder.newConstructorDeclaration(
              recordDeclaration.getName(), recordDeclaration.getName(), recordDeclaration);
      recordDeclaration.getConstructors().add(constructorDeclaration);
      lang.getScopeManager().addValueDeclaration(constructorDeclaration);
    }

    lang.getScopeManager().leaveScope(recordDeclaration);
    return recordDeclaration;
  }
}
