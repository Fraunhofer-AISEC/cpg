/*
 * Copyright (c) 2020, Fraunhofer AISEC. All rights reserved.
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

package de.fraunhofer.aisec.cpg.graph.statements.expressions;

import de.fraunhofer.aisec.cpg.graph.SubGraph;
import java.util.Objects;

/** Represents the creation of a new object through the <code>new</code> keyword. */
public class NewExpression extends Expression {

  /** The initializer expression. */
  @SubGraph("AST")
  private Expression initializer;

  public Expression getInitializer() {
    return initializer;
  }

  public void setInitializer(Expression initializer) {
    // TODO: The VariableDeclaration::setInitializer does some DFG stuff. Needed here aswell?

    if (this.initializer instanceof TypeListener) {
      this.unregisterTypeListener((TypeListener) this.initializer);
    }

    this.initializer = initializer;

    // if the initializer implements a type listener, inform it about our type changes
    // since the type is tied to the declaration but it is convenient to have the type
    // information in the initializer, i.e. in a ConstructExpression.
    if (initializer instanceof TypeListener) {
      this.registerTypeListener((TypeListener) initializer);
    }
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
    return super.equals(that) && Objects.equals(initializer, that.initializer);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }
}
