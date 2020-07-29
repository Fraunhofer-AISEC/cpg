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

import static java.util.Collections.emptyList;

import de.fraunhofer.aisec.cpg.frontends.Handler;
import de.fraunhofer.aisec.cpg.graph.ConstructorDeclaration;
import de.fraunhofer.aisec.cpg.graph.Declaration;
import de.fraunhofer.aisec.cpg.graph.Expression;
import de.fraunhofer.aisec.cpg.graph.FieldDeclaration;
import de.fraunhofer.aisec.cpg.graph.FunctionDeclaration;
import de.fraunhofer.aisec.cpg.graph.MethodDeclaration;
import de.fraunhofer.aisec.cpg.graph.NodeBuilder;
import de.fraunhofer.aisec.cpg.graph.ParamVariableDeclaration;
import de.fraunhofer.aisec.cpg.graph.ProblemDeclaration;
import de.fraunhofer.aisec.cpg.graph.RecordDeclaration;
import de.fraunhofer.aisec.cpg.graph.ValueDeclaration;
import de.fraunhofer.aisec.cpg.graph.VariableDeclaration;
import de.fraunhofer.aisec.cpg.graph.type.Type;
import de.fraunhofer.aisec.cpg.graph.type.TypeParser;
import de.fraunhofer.aisec.cpg.graph.type.UnknownType;
import de.fraunhofer.aisec.cpg.passes.scopes.RecordScope;
import de.fraunhofer.aisec.cpg.passes.scopes.Scope;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.eclipse.cdt.core.dom.ast.IASTCompositeTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTInitializer;
import org.eclipse.cdt.core.dom.ast.IASTNameOwner;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IBinding;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTCompositeTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTParameterDeclaration;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTArrayDeclarator;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTCompositeTypeSpecifier;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTDeclarator;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTFieldDeclarator;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTFunctionDeclarator;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTSimpleDeclaration;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTVisibilityLabel;

class DeclaratorHandler extends Handler<Declaration, IASTNameOwner, CXXLanguageFrontend> {

  DeclaratorHandler(CXXLanguageFrontend lang) {
    super(Declaration::new, lang);

    map.put(CPPASTDeclarator.class, ctx -> handleDeclarator((CPPASTDeclarator) ctx));
    map.put(CPPASTArrayDeclarator.class, ctx -> handleDeclarator((CPPASTDeclarator) ctx));
    map.put(CPPASTFieldDeclarator.class, ctx -> handleFieldDeclarator((CPPASTDeclarator) ctx));
    map.put(
        CPPASTFunctionDeclarator.class,
        ctx -> handleFunctionDeclarator((CPPASTFunctionDeclarator) ctx));
    map.put(
        CPPASTCompositeTypeSpecifier.class,
        ctx -> handleCompositeTypeSpecifier((CPPASTCompositeTypeSpecifier) ctx));
  }

  private Declaration handleDeclarator(CPPASTDeclarator ctx) {
    // this is just a nested declarator, i.e. () wrapping the real declarator
    if (ctx.getInitializer() == null && ctx.getNestedDeclarator() instanceof CPPASTDeclarator) {
      return handle(ctx.getNestedDeclarator());
    }

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

  private FieldDeclaration handleFieldDeclarator(CPPASTDeclarator ctx) {
    IASTInitializer init = ctx.getInitializer();
    Expression initializer = null;

    if (init != null) {
      initializer = lang.getInitializerHandler().handle(init);
    }

    // type will be filled out later
    FieldDeclaration declaration =
        NodeBuilder.newFieldDeclaration(
            ctx.getName().toString(),
            UnknownType.getUnknownType(),
            emptyList(),
            ctx.getRawSignature(),
            this.lang.getLocationFromRawNode(ctx),
            initializer,
            true);

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

    /*
     * As always, there are some special cases to consider and one of those are C++ operators.
     * They are regarded as functions and eclipse CDT for some reason introduces a whitespace in the function name, which will complicate things later on
     */
    if (name.startsWith("operator")) {
      name = name.replace(" ", "");
    }

    FunctionDeclaration declaration;

    // If this is a method, this is its record declaration
    RecordDeclaration recordDeclaration = null;

    // check for function definitions that are really methods and constructors
    if (name.contains("::")) {
      String[] rr = name.split("::");

      String recordName = rr[0];
      String methodName = rr[1];

      recordDeclaration = this.lang.getRecordForName(recordName).orElse(null);

      if (recordDeclaration != null) {
        // to make sure, that the scope of this function is associated to the record
        this.lang.getScopeManager().enterScope(recordDeclaration);
      }

      declaration =
          NodeBuilder.newMethodDeclaration(
              methodName, ctx.getRawSignature(), false, recordDeclaration);
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

    if (recordDeclaration != null) {
      this.lang.getScopeManager().enterScope(recordDeclaration);
    }

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
    RecordDeclaration recordDeclaration = lang.getScopeManager().getCurrentRecord();
    if (recordDeclaration == null) {
      // variable
      result =
          NodeBuilder.newVariableDeclaration(
              ctx.getNestedDeclarator().getName().toString(),
              UnknownType.getUnknownType(),
              ctx.getRawSignature(),
              true);
      ((VariableDeclaration) result).setInitializer(initializer);
    } else {
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
              emptyList(),
              code,
              lang.getLocationFromRawNode(ctx),
              initializer,
              true);
    }

    /*
     * Now it gets tricky, because we are looking for the parent declaration to get the full
     * raw signature. However it could be that the declarator is wrapped in nested declarators,
     * so we need to loop.
     *
     * Comment from @oxisto: I think it would still be better to parse the type in the handleSimpleDeclaration
     * and going downwards into the decl-specifiers and declarator and see whether we can re-construct them in
     * the correct order for the function type rather than going upwards from the declarator and use the raw string,
     * but that is the way it is for now.
     */
    IASTNode parent = ctx.getParent();

    while (parent != null && !(parent instanceof CPPASTSimpleDeclaration)) {
      parent = parent.getParent();
    }

    if (parent != null) {
      result.setType(TypeParser.createFrom(parent.getRawSignature(), true));
      result.refreshType();
    } else {
      log.warn("Could not find suitable parent ast node for function pointer node: {}", this);
    }

    result.setLocation(lang.getLocationFromRawNode(ctx));

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
            kind,
            ctx.getRawSignature());
    recordDeclaration.setSuperClasses(
        Arrays.stream(ctx.getBaseSpecifiers())
            .map(b -> TypeParser.createFrom(b.getNameSpecifier().toString(), true))
            .collect(Collectors.toList()));

    this.lang.addRecord(recordDeclaration);

    lang.getScopeManager().enterScope(recordDeclaration);
    lang.getScopeManager().addValueDeclaration(recordDeclaration.getThis());

    processMembers(ctx, recordDeclaration);

    if (recordDeclaration.getConstructors().isEmpty()) {
      de.fraunhofer.aisec.cpg.graph.ConstructorDeclaration constructorDeclaration =
          NodeBuilder.newConstructorDeclaration(
              recordDeclaration.getName(), recordDeclaration.getName(), recordDeclaration);

      // set this as implicit
      constructorDeclaration.setImplicit(true);

      // and set the type, constructors always have implicitly the return type of their class
      constructorDeclaration.setType(TypeParser.createFrom(recordDeclaration.getName(), true));

      recordDeclaration.getConstructors().add(constructorDeclaration);

      lang.getScopeManager().addValueDeclaration(constructorDeclaration);
    }

    lang.getScopeManager().leaveScope(recordDeclaration);
    return recordDeclaration;
  }

  private void processMembers(
      CPPASTCompositeTypeSpecifier ctx, RecordDeclaration recordDeclaration) {
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
          Type type =
              TypeParser.createFrom(
                  lang.getScopeManager()
                      .getFirstScopeThat(RecordScope.class::isInstance)
                      .getAstNode()
                      .getName(),
                  true);
          constructor.setType(type);
          recordDeclaration.getConstructors().add(constructor);

          // update scope manager, otherwise we point at the old function declaration
          this.lang.getScopeManager().replaceNode(constructor, declaration);
        } else {
          recordDeclaration.getMethods().add(method);

          // update scope manager, otherwise we point at the old function declaration
          this.lang.getScopeManager().replaceNode(method, declaration);
        }

        if (declarationScope != null) {
          declarationScope.setAstNode(method); // Adjust cpg Node by which scopes are identified
        }
      } else if (declaration instanceof VariableDeclaration) {
        FieldDeclaration fieldDeclaration =
            FieldDeclaration.from((VariableDeclaration) declaration);
        recordDeclaration.getFields().add(fieldDeclaration);
        this.lang.replaceDeclarationInExpression(fieldDeclaration, declaration);

      } else if (declaration instanceof FieldDeclaration) {
        recordDeclaration.getFields().add((FieldDeclaration) declaration);
      } else if (declaration instanceof RecordDeclaration) {
        recordDeclaration.getRecords().add((RecordDeclaration) declaration);
      } else if (declaration instanceof ProblemDeclaration) {
        // there is no place to put them here so let's attach them to the translation unit so that
        // we do not loose them
        lang.getCurrentTU().add(declaration);
      }
    }
  }
}
