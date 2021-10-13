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

import java.lang.reflect.ParameterizedType;
import java.util.HashMap;
import java.util.function.Function;
import java.util.function.Supplier;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.eclipse.cdt.internal.core.dom.parser.ASTNode;
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

  protected final HashMap<Class<? extends T>, @NonNull Function<T, @NonNull S>> map =
      new HashMap<>();
  private final Supplier<S> configConstructor;
  protected L lang;
  private Class<S> typeOfT =
      (Class<S>)
          ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[1];

  public Handler(Supplier<S> configConstructor, L lang) {
    this.configConstructor = configConstructor;
    this.lang = lang;
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
    var handler = map.get(toHandle);
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
      if (toHandle == typeOfT || !typeOfT.isAssignableFrom(toHandle)) break;
    }
    if (handler != null) {
      S s = handler.apply(ctx);
      lang.setCodeAndRegion(s, ctx);
      lang.setComment(s, ctx);
      ret = s;
    } else {
      errorWithFileLocation(
          lang, ctx, log, "Parsing of type {} is not supported (yet)", ctx.getClass());
      ret = this.configConstructor.get();
    }

    lang.process(ctx, ret);
    return ret;
  }
}
