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

import static de.fraunhofer.aisec.cpg.graph.NodeBuilder.newConstructorDeclaration;
import static de.fraunhofer.aisec.cpg.graph.NodeBuilder.newMethodDeclaration;
import static de.fraunhofer.aisec.cpg.helpers.Util.warnWithFileLocation;
import static java.util.Collections.emptyList;

import de.fraunhofer.aisec.cpg.frontends.Handler;
import de.fraunhofer.aisec.cpg.graph.NodeBuilder;
import de.fraunhofer.aisec.cpg.graph.declarations.*;
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression;
import de.fraunhofer.aisec.cpg.graph.types.IncompleteType;
import de.fraunhofer.aisec.cpg.graph.types.TypeParser;
import de.fraunhofer.aisec.cpg.graph.types.UnknownType;
import de.fraunhofer.aisec.cpg.passes.scopes.RecordScope;
import de.fraunhofer.aisec.cpg.passes.scopes.TemplateScope;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.eclipse.cdt.core.dom.ast.*;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTCompositeTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTParameterDeclaration;
import org.eclipse.cdt.internal.core.dom.parser.cpp.*;

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
    map.put(
        CPPASTSimpleTypeTemplateParameter.class,
        ctx -> handleTemplateTypeParameter((CPPASTSimpleTypeTemplateParameter) ctx));
  }

  private Declaration handleDeclarator(CPPASTDeclarator ctx) {
    // this is just a nested declarator, i.e. () wrapping the real declarator
    if (ctx.getInitializer() == null && ctx.getNestedDeclarator() instanceof CPPASTDeclarator) {
      return handle(ctx.getNestedDeclarator());
    }

    String name = ctx.getName().toString();

    if (lang.getScopeManager().getCurrentScope() instanceof RecordScope
        || name.contains(lang.getNamespaceDelimiter())) {
      // forward it to handleFieldDeclarator
      return handleFieldDeclarator(ctx);
    } else {
      // type will be filled out later
      VariableDeclaration declaration =
          NodeBuilder.newVariableDeclaration(
              ctx.getName().toString(), UnknownType.getUnknownType(), ctx.getRawSignature(), true);

      IASTInitializer init = ctx.getInitializer();

      if (init != null) {
        declaration.setInitializer(lang.getInitializerHandler().handle(init));
      }

      lang.getScopeManager().addDeclaration(declaration);

      return declaration;
    }
  }

  private FieldDeclaration handleFieldDeclarator(CPPASTDeclarator ctx) {
    IASTInitializer init = ctx.getInitializer();
    Expression initializer = null;

    if (init != null) {
      initializer = lang.getInitializerHandler().handle(init);
    }

    String name = ctx.getName().toString();

    FieldDeclaration declaration;

    if (name.contains(lang.getNamespaceDelimiter())) {
      String[] rr = name.split(lang.getNamespaceDelimiter());

      String recordName =
          String.join(lang.getNamespaceDelimiter(), Arrays.asList(rr).subList(0, rr.length - 1));
      String fieldName = rr[rr.length - 1];

      declaration =
          NodeBuilder.newFieldDeclaration(
              fieldName,
              UnknownType.getUnknownType(),
              emptyList(),
              ctx.getRawSignature(),
              this.lang.getLocationFromRawNode(ctx),
              initializer,
              true);

      var recordDeclaration =
          this.lang
              .getScopeManager()
              .getRecordForName(this.lang.getScopeManager().getCurrentScope(), recordName);

      // prepared for PR #223 - to set the definition here
    } else {
      declaration =
          NodeBuilder.newFieldDeclaration(
              name,
              UnknownType.getUnknownType(),
              emptyList(),
              ctx.getRawSignature(),
              this.lang.getLocationFromRawNode(ctx),
              initializer,
              true);
    }

    lang.getScopeManager().addDeclaration(declaration);

    return declaration;
  }

  private MethodDeclaration createMethodOrConstructor(
      String name, String code, @Nullable RecordDeclaration recordDeclaration) {
    // check, if its a constructor
    if (name.equals(recordDeclaration != null ? recordDeclaration.getName() : null)) {
      return newConstructorDeclaration(name, code, recordDeclaration);
    }

    return newMethodDeclaration(name, code, false, recordDeclaration);
  }

  private ValueDeclaration handleFunctionDeclarator(CPPASTFunctionDeclarator ctx) {
    // Programmers can wrap the function name in as many levels of parentheses as they like. CDT
    // treats these levels as separate declarators, so we need to get to the bottom for the
    // actual name...
    IASTDeclarator nameDecl = ctx;
    var hasPointer = false;

    while (nameDecl.getNestedDeclarator() != null) {
      nameDecl = nameDecl.getNestedDeclarator();
      if (nameDecl.getPointerOperators().length > 0) {
        hasPointer = true;
      }
    }

    String name = nameDecl.getName().toString();

    // Attention! This might actually be a function pointer (requires at least one level of
    // parentheses and a pointer operator)
    if (nameDecl != ctx && hasPointer) {
      return handleFunctionPointer(ctx, name);
    }

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

    // remember, if this is a method declaration outside of the record
    var outsideOfRecord =
        !(lang.getScopeManager().getCurrentScope() instanceof RecordScope
            || lang.getScopeManager().getCurrentScope() instanceof TemplateScope);

    // check for function definitions that are really methods and constructors, i.e. if they contain
    // a scope operator
    if (name.contains(lang.getNamespaceDelimiter())) {
      String[] rr = name.split(lang.getNamespaceDelimiter());

      String recordName =
          String.join(lang.getNamespaceDelimiter(), Arrays.asList(rr).subList(0, rr.length - 1));
      String methodName = rr[rr.length - 1];

      recordDeclaration =
          this.lang
              .getScopeManager()
              .getRecordForName(this.lang.getScopeManager().getCurrentScope(), recordName);

      declaration = createMethodOrConstructor(methodName, ctx.getRawSignature(), recordDeclaration);
    } else if (this.lang.getScopeManager().isInRecord()) {
      // if it is inside a record scope, it is a method
      recordDeclaration = this.lang.getScopeManager().getCurrentRecord();

      declaration = createMethodOrConstructor(name, ctx.getRawSignature(), recordDeclaration);
    } else {
      // a plain old function, outside any record scope
      declaration = NodeBuilder.newFunctionDeclaration(name, ctx.getRawSignature());
    }

    lang.getScopeManager().addDeclaration(declaration);

    // if we know our record declaration, but are outside the actual record, we
    // need to temporary enter the record scope
    if (recordDeclaration != null && outsideOfRecord) {
      // to make sure, that the scope of this function is associated to the record
      this.lang.getScopeManager().enterScope(recordDeclaration);
    }

    lang.getScopeManager().enterScope(declaration);

    int i = 0;
    for (ICPPASTParameterDeclaration param : ctx.getParameters()) {
      ParamVariableDeclaration arg = lang.getParameterDeclarationHandler().handle(param);

      // check for void type parameters
      if (arg.getType() instanceof IncompleteType) {
        if (!arg.getName().isEmpty()) {
          warnWithFileLocation(declaration, log, "Named parameter cannot have void type");
        } else {
          // specifying void as first parameter is ok and means that the function has no parameters
          if (i == 0) {
            continue;
          } else {
            warnWithFileLocation(
                declaration, log, "void parameter must be the first and only parameter");
          }
        }
      }

      IBinding binding = ctx.getParameters()[i].getDeclarator().getName().resolveBinding();

      if (binding != null) {
        lang.cacheDeclaration(binding, arg);
      }

      arg.setArgumentIndex(i);
      // Note that this .addValueDeclaration call already adds arg to the function's parameters.
      // This is why the following line has been commented out by @KW
      lang.getScopeManager().addDeclaration(arg);
      // declaration.getParameters().add(arg);
      i++;
    }

    // Check for varargs. Note the difference to Java: here, we don't have a named array
    // containing the varargs, but they are rather treated as kind of an invisible arg list that is
    // appended to the original ones. For coherent graph behaviour, we introduce an implicit
    // declaration that
    // wraps this list
    if (ctx.takesVarArgs()) {
      ParamVariableDeclaration varargs =
          NodeBuilder.newMethodParameterIn("va_args", UnknownType.getUnknownType(), true, "");
      varargs.setImplicit(true);
      varargs.setArgumentIndex(i);
      lang.getScopeManager().addDeclaration(varargs);
    }

    lang.getScopeManager().leaveScope(declaration);

    // if we know our record declaration, but are outside the actual record, we
    // need to leave the record scope again afterwards
    if (recordDeclaration != null && outsideOfRecord) {
      this.lang.getScopeManager().leaveScope(recordDeclaration);
    }

    return declaration;
  }

  private ValueDeclaration handleFunctionPointer(CPPASTFunctionDeclarator ctx, String name) {
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
              name, UnknownType.getUnknownType(), ctx.getRawSignature(), true);
      ((VariableDeclaration) result).setInitializer(initializer);
    } else {
      // field
      String code = ctx.getRawSignature();
      Pattern namePattern = Pattern.compile("\\((\\*|.+\\*)(?<name>[^)]*)");
      Matcher matcher = namePattern.matcher(code);
      String fieldName = "";
      if (matcher.find()) {
        fieldName = matcher.group("name").strip();
      }
      result =
          NodeBuilder.newFieldDeclaration(
              fieldName,
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
      result.setType(TypeParser.createFrom(parent.getRawSignature(), true, lang));
      result.refreshType();
    } else {
      log.warn("Could not find suitable parent ast node for function pointer node: {}", this);
    }

    result.setLocation(lang.getLocationFromRawNode(ctx));
    lang.getScopeManager().addDeclaration(result);

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
            .map(b -> TypeParser.createFrom(b.getNameSpecifier().toString(), true, lang))
            .collect(Collectors.toList()));

    lang.getScopeManager().addDeclaration(recordDeclaration);

    lang.getScopeManager().enterScope(recordDeclaration);
    lang.getScopeManager().addDeclaration(recordDeclaration.getThis());

    processMembers(ctx);

    if (recordDeclaration.getConstructors().isEmpty()) {
      ConstructorDeclaration constructorDeclaration =
          newConstructorDeclaration(
              recordDeclaration.getName(), recordDeclaration.getName(), recordDeclaration);

      // set this as implicit
      constructorDeclaration.setImplicit(true);

      // and set the type, constructors always have implicitly the return type of their class
      constructorDeclaration.setType(
          TypeParser.createFrom(recordDeclaration.getName(), true, lang));

      recordDeclaration.addConstructor(constructorDeclaration);

      lang.getScopeManager().addDeclaration(constructorDeclaration);
    }

    lang.getScopeManager().leaveScope(recordDeclaration);
    return recordDeclaration;
  }

  /**
   * Handles template parameters that are types
   *
   * @param ctx
   * @return TypeParamDeclaration with its name
   */
  private TypeParamDeclaration handleTemplateTypeParameter(CPPASTSimpleTypeTemplateParameter ctx) {
    return NodeBuilder.newTypeParamDeclaration(ctx.getRawSignature(), ctx.getRawSignature());
  }

  private void processMembers(CPPASTCompositeTypeSpecifier ctx) {
    for (IASTDeclaration member : ctx.getMembers()) {
      if (member instanceof CPPASTVisibilityLabel) {
        // TODO: parse visibility
        continue;
      }

      lang.getDeclarationHandler().handle(member);
    }
  }
}
