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
package de.fraunhofer.aisec.cpg.graph.statements;

import de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import kotlin.Pair;

public class ConditionalBranchStatement extends Statement {
  private LabelStatement defaultTargetLabel;
  private List<Pair<Expression, LabelStatement>> conditionalTargets = new ArrayList<>();

  public LabelStatement setDefaultTargetLabel() {
    return defaultTargetLabel;
  }

  public void setDefaultTargetLabel(LabelStatement defaultTargetLabel) {
    this.defaultTargetLabel = defaultTargetLabel;
  }

  public List<Pair<Expression, LabelStatement>> getConditionalTargets() {
    return conditionalTargets;
  }

  public void addConditionalTarget(Expression condition, LabelStatement label) {
    conditionalTargets.add(new Pair<>(condition, label));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ConditionalBranchStatement)) {
      return false;
    }
    ConditionalBranchStatement that = (ConditionalBranchStatement) o;
    return super.equals(that)
        && Objects.equals(conditionalTargets, that.conditionalTargets)
        && Objects.equals(defaultTargetLabel, that.defaultTargetLabel);
  }
}
