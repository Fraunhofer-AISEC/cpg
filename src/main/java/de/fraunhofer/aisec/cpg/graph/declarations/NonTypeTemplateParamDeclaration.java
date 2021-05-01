/*
 * Copyright (c) 2021, Fraunhofer AISEC. All rights reserved.
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

import de.fraunhofer.aisec.cpg.graph.SubGraph;
import de.fraunhofer.aisec.cpg.graph.TemplateParameter;
import de.fraunhofer.aisec.cpg.graph.TypeManager;
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression;
import java.util.*;
import org.neo4j.ogm.annotation.Relationship;

public class NonTypeTemplateParamDeclaration extends ParamVariableDeclaration
    implements TemplateParameter<Expression> {
  @Relationship(value = "POSSIBLE_INITIALIZATIONS", direction = "OUTGOING")
  @SubGraph("AST")
  protected Set<Expression> possibleInitializations = new HashSet<>();

  public Set<Expression> getPossibleInitializations() {
    return this.possibleInitializations;
  }

  public void addPossibleInitialization(Expression expression) {
    this.possibleInitializations.add(expression);
  }

  public boolean canBeInstantiated(Expression expression) {
    return expression.getType().equals(this.getType())
        || TypeManager.getInstance().isSupertypeOf(this.getType(), expression.getType());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;
    NonTypeTemplateParamDeclaration that = (NonTypeTemplateParamDeclaration) o;
    return possibleInitializations.equals(that.possibleInitializations);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), possibleInitializations);
  }
}
