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

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.fraunhofer.aisec.cpg.TranslationResult;
import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend;
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
public abstract class Pass implements Consumer<TranslationResult> {

  protected String name;

  protected static final Logger log = LoggerFactory.getLogger(Pass.class);

  /**
   * Dependencies which - if present - have to be executed before this pass. Note: Dependencies
   * registered here will not be added automatically to the list of active passes. Use
   * [hardDependencies] to add them automatically.
   */
  private Set<Class<? extends Pass>> softDependencies;

  /**
   * Dependencies which have to be executed before this pass. Note: Dependencies registered here
   * will be added to the list of active passes automatically. Use [softDependencies] if this is not
   * desired.
   */
  private Set<Class<? extends Pass>> hardDependencies;

  protected Pass() {
    name = this.getClass().getName();
    hardDependencies = new HashSet<>();
    softDependencies = new HashSet<>();

    // collect all dependencies added by [PassRegisterSoftDependency] annotations.
    if (this.getClass().isAnnotationPresent(PassRegisterSoftDependency.class)) {
      PassRegisterSoftDependency[] dependencies =
          this.getClass().getAnnotationsByType(PassRegisterSoftDependency.class);
      for (PassRegisterSoftDependency d : dependencies) {
        softDependencies.add(d.value());
      }
    }

    // collect all dependencies added by [PassRegisterHardDependency] annotations.
    if (this.getClass().isAnnotationPresent(PassRegisterHardDependency.class)) {
      PassRegisterHardDependency[] dependencies =
          this.getClass().getAnnotationsByType(PassRegisterHardDependency.class);
      for (PassRegisterHardDependency d : dependencies) {
        hardDependencies.add(d.value());
      }
    }
  }

  @JsonIgnore @Nullable protected LanguageFrontend lang;

  /**
   * @return May be null
   */
  @Nullable
  public LanguageFrontend getLang() {
    return lang;
  }

  public String getName() {
    return name;
  }

  /**
   * Passes may need information about what source language they are parsing.
   *
   * @param lang May be null
   */
  public void setLang(@Nullable LanguageFrontend lang) {
    this.lang = lang;
  }

  public abstract void cleanup();

  /**
   * Specifies, whether this pass supports this particular language frontend. This defaults to
   * <code>true</code> and needs to be overridden if a different behaviour is wanted.
   *
   * <p>Note: this is not yet used, since we do not have an easy way at the moment to find out which
   * language frontend a result used.
   *
   * @param lang the language frontend
   * @return <code>true</code> by default
   */
  public boolean supportsLanguageFrontend(LanguageFrontend lang) {
    return true;
  }

  @NotNull
  public Boolean isLastPass() {
    try {
      return this.getClass().isAnnotationPresent(PassIsLastPass.class);
    } catch (Exception e) {
      return false;
    }
  }

  @NotNull
  public Boolean isFirstPass() {
    try {
      return this.getClass().isAnnotationPresent(PassIsFirstPass.class);
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
}
