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
package de.fraunhofer.aisec.cpg.graph.declarations;

import de.fraunhofer.aisec.cpg.graph.HasDefault;
import de.fraunhofer.aisec.cpg.graph.SubGraph;
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.neo4j.ogm.annotation.Relationship;

/** A declaration of a function or nontype template parameter. */
public class ParamVariableDeclaration extends ValueDeclaration implements HasDefault<Expression> {

  @NotNull private boolean variadic = false;

  @Relationship(value = "DEFAULT", direction = "OUTGOING")
  @SubGraph("AST")
  private Expression defaultValue;

  public boolean isVariadic() {
    return variadic;
  }

  public void setVariadic(boolean variadic) {
    this.variadic = variadic;
  }

  public Expression getDefault() {
    return defaultValue;
  }

  public void setDefault(Expression defaultValue) {
    this.defaultValue = defaultValue;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;
    ParamVariableDeclaration that = (ParamVariableDeclaration) o;
    return variadic == that.variadic && Objects.equals(defaultValue, that.defaultValue);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), variadic, defaultValue);
  }
}
