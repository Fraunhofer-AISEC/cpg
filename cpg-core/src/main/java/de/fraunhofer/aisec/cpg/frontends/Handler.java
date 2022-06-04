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
package de.fraunhofer.aisec.cpg.frontends;

import static de.fraunhofer.aisec.cpg.helpers.Util.errorWithFileLocation;

import de.fraunhofer.aisec.cpg.graph.Node;
import de.fraunhofer.aisec.cpg.graph.ProblemNode;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.function.Supplier;
import org.eclipse.cdt.internal.core.dom.parser.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link Handler} is an abstract base class for a class that translates AST nodes from a raw ast
 * type, usually supplied by a language parser into our generic CPG nodes.
 *
 * @param <S> the result node or a collection of nodes
 * @param <T> the raw ast node specific to the parser
 * @param <L> the language frontend
 */
public abstract class Handler<S, T, L extends LanguageFrontend> {

  protected static final Logger log = LoggerFactory.getLogger(Handler.class);

  protected final HashMap<Class<? extends T>, HandlerInterface<S, T>> map = new HashMap<>();
  private final Supplier<S> configConstructor;
  protected @NotNull L lang;
  @Nullable private final Class<?> typeOfT;

  public Handler(Supplier<S> configConstructor, @NotNull L lang) {
    this.configConstructor = configConstructor;
    this.lang = lang;
    this.typeOfT = retrieveTypeParameter();
  }

  /**
   * Searches for a handler matching the most specific superclass of {@link T}. The created map
   * should thus contain a handler for every semantically different AST node and can reuse handler
   * code as long as the handled AST nodes have a common ancestor.
   *
   * @param ctx The AST node, whose handler is matched with respect to the AST node class.
   * @return most specific handler.
   */
  public S handle(T ctx) {
    S ret;
    if (ctx == null) {
      log.error(
          "ctx is NULL. This can happen when ast children are optional in {}. Called by {}",
          this.getClass(),
          Thread.currentThread().getStackTrace()[2]);
      return null;
    }

    // If we do not want to load includes into the CPG and the current fileLocation was included
    if (!this.lang.config.loadIncludes && ctx instanceof ASTNode) {
      ASTNode astNode = (ASTNode) ctx;

      if (astNode.getFileLocation() != null
          && astNode.getFileLocation().getContextInclusionStatement() != null) {
        log.debug("Skip parsing include file" + astNode.getContainingFilename());
        return null;
      }
    }

    Class<?> toHandle = ctx.getClass();
    HandlerInterface<S, T> handler = map.get(toHandle);
    while (handler == null) {
      toHandle = toHandle.getSuperclass();
      handler = map.get(toHandle);
      if (handler != null
          &&
          // always ok to handle as generic literal expr
          !ctx.getClass().getSimpleName().contains("LiteralExpr")) {
        errorWithFileLocation(
            lang,
            ctx,
            log,
            "No handler for type {}, resolving for its superclass {}.",
            ctx.getClass(),
            toHandle);
      }
      if (toHandle == typeOfT || (typeOfT != null && !typeOfT.isAssignableFrom(toHandle))) {
        break;
      }
    }
    if (handler != null) {
      S s = handler.handle(ctx);

      if (s != null) {
        // The language frontend might set a location, which we should respect. Otherwise, we will
        // set the location here.
        if (((Node) s).getLocation() == null) {
          lang.setCodeAndRegion(s, ctx);
        }

        lang.setComment(s, ctx);
      }

      ret = s;
    } else {
      errorWithFileLocation(
          lang, ctx, log, "Parsing of type {} is not supported (yet)", ctx.getClass());
      ret = this.configConstructor.get();
      if (ret instanceof ProblemNode) {
        ProblemNode problem = (ProblemNode) ret;
        problem.setProblem(
            String.format("Parsing of type {} is not supported (yet)", ctx.getClass()));
      }
    }

    lang.process(ctx, ret);
    return ret;
  }

  private Class<?> retrieveTypeParameter() {
    Class<?> clazz = this.getClass();

    while (clazz.getSuperclass() != null && !clazz.getSuperclass().equals(Handler.class)) {
      clazz = clazz.getSuperclass();
    }

    var type = clazz.getGenericSuperclass();
    if (type instanceof ParameterizedType) {
      var parameterizedType = (ParameterizedType) type;

      var rawType = parameterizedType.getActualTypeArguments()[1];

      return getBaseClass(rawType);
    }

    log.error("Could not determine generic type of raw AST node in handler");

    return null;
  }

  private Class<?> getBaseClass(Type type) {
    if (type instanceof Class) {
      return (Class<?>) type;
    } else if (type instanceof ParameterizedType) {
      return getBaseClass(((ParameterizedType) type).getRawType());
    }

    return null;
  }
}
