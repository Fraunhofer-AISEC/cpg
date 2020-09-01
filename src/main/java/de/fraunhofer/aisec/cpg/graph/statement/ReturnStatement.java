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

package de.fraunhofer.aisec.cpg.graph.statement;

import de.fraunhofer.aisec.cpg.graph.Node;
import de.fraunhofer.aisec.cpg.graph.SubGraph;
import de.fraunhofer.aisec.cpg.graph.statement.expression.Expression;
import java.util.Objects;
import org.apache.commons.lang3.builder.ToStringBuilder;

/** Represents a statement that returns out of the current function. */
public class ReturnStatement extends Statement {

  /** The expression whose value will be returned. */
  @SubGraph("AST")
  private Expression returnValue;

  public Expression getReturnValue() {
    return returnValue;
  }

  public void setReturnValue(Expression returnValue) {
    if (this.returnValue != null) {
      this.removePrevDFG(this.returnValue);
    }

    this.returnValue = returnValue;

    if (returnValue != null) {
      this.addPrevDFG(returnValue);
    }
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this, Node.TO_STRING_STYLE)
        .appendSuper(super.toString())
        .append("returnValue", returnValue)
        .toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ReturnStatement)) {
      return false;
    }
    ReturnStatement that = (ReturnStatement) o;
    return super.equals(that) && Objects.equals(returnValue, that.returnValue);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }
}
