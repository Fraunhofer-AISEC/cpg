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

import de.fraunhofer.aisec.cpg.graph.types.Type;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Modells C++ operations that inspect types. These are <code>typeof</code>, <code>sizeof</code>,
 * <code>typeid</code>, <code>alignof</code>and are stored as string in ther operator code.
 */
public class TypeIdExpression extends Expression {

  private static final Logger log = LoggerFactory.getLogger(TypeIdExpression.class);

  private Type referencedType;

  private String operatorCode;

  public Type getReferencedType() {
    return referencedType;
  }

  public void setReferencedType(Type referencedType) {
    this.referencedType = referencedType;
  }

  public String getOperatorCode() {
    return operatorCode;
  }

  public void setOperatorCode(String operatorCode) {
    this.operatorCode = operatorCode;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof TypeIdExpression)) {
      return false;
    }
    TypeIdExpression that = (TypeIdExpression) o;
    return super.equals(that)
        && Objects.equals(operatorCode, that.operatorCode)
        && Objects.equals(referencedType, that.referencedType);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }
}
