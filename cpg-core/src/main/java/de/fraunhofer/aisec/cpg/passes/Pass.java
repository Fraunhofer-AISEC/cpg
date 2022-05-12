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
import java.util.function.Consumer;
import org.checkerframework.checker.nullness.qual.Nullable;
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

  protected Pass() {
    name = this.getClass().getName();
  }

  @JsonIgnore @Nullable protected LanguageFrontend lang;

  /** @return May be null */
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
}
