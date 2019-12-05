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

package de.fraunhofer.aisec.cpg.graph;

import java.util.Objects;
import org.apache.commons.lang3.builder.ToStringBuilder;

/** Represents the creation of a new object through the <code>new</code> keyword. */
public class NewExpression extends Expression {

  /** The {@link Declaration} of the type this expression instantiates. */
  private Declaration instantiates;

  /** The initializer expression. */
  @SubGraph("AST")
  private Expression initializer;

  public Declaration getInstantiates() {
    return instantiates;
  }

  public void setInstantiates(Declaration instantiates) {
    this.instantiates = instantiates;
    if (instantiates != null) {
      setType(new Type(instantiates.getName()));
    }
  }

  public Expression getInitializer() {
    return initializer;
  }

  public void setInitializer(Expression initializer) {
    this.initializer = initializer;
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this)
        .appendSuper(super.toString())
        .append("instantiates", instantiates)
        .append("initializer", initializer)
        .toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof NewExpression)) {
      return false;
    }
    NewExpression that = (NewExpression) o;
    return super.equals(that)
        && Objects.equals(instantiates, that.instantiates)
        && Objects.equals(initializer, that.initializer);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }
}
