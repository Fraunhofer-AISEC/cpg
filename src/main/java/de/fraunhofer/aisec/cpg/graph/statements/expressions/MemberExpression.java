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
import de.fraunhofer.aisec.cpg.graph.declarations.RecordDeclaration;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Represents access to a field of a {@link RecordDeclaration}, such as <code>obj.property</code>.
 */
public class MemberExpression extends DeclaredReferenceExpression {

  @SubGraph("AST")
  @NonNull
  private Expression base;

  @NonNull
  public Expression getBase() {
    return base;
  }

  public void setBase(@NonNull Expression base) {
    this.base = base;
  }

  private String operatorCode;

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof MemberExpression)) {
      return false;
    }
    MemberExpression that = (MemberExpression) o;
    return super.equals(that) && Objects.equals(base, that.base);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }

  public void setOperatorCode(String operatorCode) {
    this.operatorCode = operatorCode;
  }

  public String getOperatorCode() {
    return this.operatorCode;
  }
}
