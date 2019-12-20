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
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/** Represents an assert statement */
public class AssertStatement extends Statement {

  /** The condition to be evaluated. */
  @SubGraph("AST")
  private Expression condition;

  /** The _optional_ message that is shown, if the assert is evaluated as true */
  @SubGraph("AST")
  @Nullable
  private Statement message;

  public Expression getCondition() {
    return condition;
  }

  public void setCondition(Expression condition) {
    this.condition = condition;
  }

  @Nullable
  public Statement getMessage() {
    return message;
  }

  public void setMessage(@NonNull Statement message) {
    this.message = message;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof AssertStatement)) {
      return false;
    }
    AssertStatement that = (AssertStatement) o;
    return super.equals(that)
        && Objects.equals(condition, that.condition)
        && Objects.equals(message, that.message);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }
}
