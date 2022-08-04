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
   * Indicates whether this pass should be executed as the first pass. Note: setting this flag to
   * `true` for more than one active pass will yield an error. Note: setting this flag will not
   * activate the pass. You must register the pass manually.
   */
  private Boolean firstPass;

  /**
   * Indicates whether this pass should be executed as the last pass. Note: setting this flag to
   * `true` for more than one active pass will yield an error. Note: setting this flag will not
   * activate the pass. You must register the pass manually.
   */
  private Boolean lastPass;

  private Set<Class<? extends Pass>> afterPass;

  private Set<Class<? extends Pass>> dependsOn;

  protected Pass() {
    name = this.getClass().getName();
    dependsOn = new HashSet<>();
    afterPass = new HashSet<>();
    firstPass = false;
    lastPass = false;
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

  public boolean getFirstPass() {
    return this.firstPass;
  }

  public Set<Class<? extends Pass>> getDependsOn() {
    return this.dependsOn;
  }

  public void registerDependency(Class<? extends Pass> p) {
    this.dependsOn.add(p);
  }

  public void registerSoftDependency(Class<? extends Pass> p) {
    this.afterPass.add(p);
  }

  public void setFirstPass(Boolean flag) {
    this.firstPass = flag;
  }

  public void setLastPass(Boolean flag) {
    this.lastPass = flag;
  }

  public boolean getLastPass() {
    return this.lastPass;
  }

  public Set<Class<? extends Pass>> getAfterPass() {
    return this.afterPass;
  }
}
