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

/**
 * A label attached to a statement that is used to change control flow by labeled continue and
 * breaks (Java) or goto(C++).
 */
public class LabelStatement extends Statement {

  /** Statement that the label is attached to. Can be a simple or compound statement. */
  @SubGraph("AST")
  private Statement subStatement;

  /** Label in the form of a String */
  private String label;

  public Statement getSubStatement() {
    return subStatement;
  }

  public void setSubStatement(Statement subStatement) {
    this.subStatement = subStatement;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this, Node.TO_STRING_STYLE)
        .appendSuper(super.toString())
        .append("subStatement", subStatement)
        .append("label", label)
        .toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof LabelStatement)) {
      return false;
    }
    LabelStatement that = (LabelStatement) o;
    return super.equals(that)
        && Objects.equals(subStatement, that.subStatement)
        && Objects.equals(label, that.label);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }
}
