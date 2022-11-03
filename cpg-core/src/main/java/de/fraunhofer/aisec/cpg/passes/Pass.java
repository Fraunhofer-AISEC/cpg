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
package de.fraunhofer.aisec.cpg.passes;

import de.fraunhofer.aisec.cpg.TranslationConfiguration;
import de.fraunhofer.aisec.cpg.TranslationResult;
import de.fraunhofer.aisec.cpg.frontends.Language;
import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend;
import de.fraunhofer.aisec.cpg.passes.order.*;
import de.fraunhofer.aisec.cpg.passes.scopes.ScopeManager;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents an abstract class that enhances the graph before it is persisted.
 *
 * <p>Passes are expected to mutate the {@code TranslationResult}.
 */
public abstract class Pass implements Consumer<@NotNull TranslationResult> {

  protected String name;

  public static final Logger log = LoggerFactory.getLogger(Pass.class);

  /**
   * Dependencies which - if present - have to be executed before this pass. Note: Dependencies
   * registered here will not be added automatically to the list of active passes. Use
   * [hardDependencies] to add them automatically.
   */
  private final Set<Class<? extends Pass>> softDependencies;

  /**
   * Dependencies which have to be executed before this pass. Note: Dependencies registered here
   * will be added to the list of active passes automatically. Use [softDependencies] if this is not
   * desired.
   */
  private final Set<Class<? extends Pass>> hardDependencies;

  private final Set<Class<? extends Pass>> executeBefore;

  public Set<Class<? extends Pass>> getExecuteBefore() {
    return executeBefore;
  }

  public void addSoftDependency(Class<? extends Pass> toAdd) {
    softDependencies.add(toAdd);
  }

  protected Pass() {
    name = this.getClass().getName();
    hardDependencies = new HashSet<>();
    softDependencies = new HashSet<>();
    executeBefore = new HashSet<>();

    // collect all dependencies added by [DependsOn] annotations.
    if (this.getClass().getAnnotationsByType(DependsOn.class).length != 0) {
      DependsOn[] dependencies = this.getClass().getAnnotationsByType(DependsOn.class);
      for (DependsOn d : dependencies) {
        if (d.softDependency()) {
          softDependencies.add(d.value());
        } else {
          hardDependencies.add(d.value());
        }
      }
    }

    if (this.getClass().getAnnotationsByType(ExecuteBefore.class).length != 0) {
      ExecuteBefore[] dependencies = this.getClass().getAnnotationsByType(ExecuteBefore.class);
      for (ExecuteBefore d : dependencies) {
        executeBefore.add(d.other());
      }
    }
  }

  protected @Nullable ScopeManager scopeManager;
  protected @Nullable TranslationConfiguration config;

  public String getName() {
    return name;
  }

  public abstract void cleanup();

  /**
   * Specifies, whether this pass supports this particular language. This defaults to <code>true
   * </code> and needs to be overridden if a different behaviour is wanted.
   *
   * @param lang the language
   * @return <code>true</code> by default
   */
  public boolean supportsLanguage(Language<? extends LanguageFrontend> lang) {
    return true;
  }

  @NotNull
  public Boolean isLastPass() {
    try {
      return this.getClass().isAnnotationPresent(ExecuteLast.class);
    } catch (Exception e) {
      return false;
    }
  }

  @NotNull
  public Boolean isFirstPass() {
    try {
      return this.getClass().isAnnotationPresent(ExecuteFirst.class);
    } catch (Exception e) {
      return false;
    }
  }

  public Set<Class<? extends Pass>> getSoftDependencies() {
    return this.softDependencies;
  }

  public Set<Class<? extends Pass>> getHardDependencies() {
    return hardDependencies;
  }

  /**
   * Check if the pass requires a specific language frontend and if that frontend has been executed.
   *
   * @return true, if the pass does not require a specific language frontend or if it matches the
   *     [RequiredLanguage]
   */
  public boolean runsWithCurrentFrontend(Collection<LanguageFrontend> usedFrontends) {
    if (!this.getClass().isAnnotationPresent(RequiredFrontend.class)) return true;

    Class<? extends LanguageFrontend> requiredFrontend =
        this.getClass().getAnnotation(RequiredFrontend.class).value();
    for (LanguageFrontend used : usedFrontends) {
      if (used.getClass() == requiredFrontend) return true;
    }
    return false;
  }
}
