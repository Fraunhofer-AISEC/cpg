/*
 * Copyright (c) 2020, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.graph.statements.expressions;

import de.fraunhofer.aisec.cpg.graph.HasInitializer;
import de.fraunhofer.aisec.cpg.graph.Node;
import de.fraunhofer.aisec.cpg.graph.SubGraph;
import java.util.List;
import java.util.Objects;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jetbrains.annotations.NotNull;
import org.neo4j.ogm.annotation.Relationship;

/** Represents the creation of a new object through the <code>new</code> keyword. */
public class NewExpression extends Expression implements HasInitializer {

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

  /**
   * We need a way to store the templateParameters that a NewExpression might have before the
   * ConstructExpression is created
   */
  @Relationship(value = "TEMPLATE_PARAMETERS", direction = "OUTGOING")
  @SubGraph("AST")
  @Nullable
  private List<Node> templateParameters = null;

  public List<Node> getTemplateParameters() {
    return templateParameters;
  }

  public void setTemplateParameters(List<Node> templateParameters) {
    this.templateParameters = templateParameters;
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this, Node.TO_STRING_STYLE)
        .appendSuper(super.toString())
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
    return super.equals(that) && Objects.equals(initializer, that.initializer);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }
}
