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

import de.fraunhofer.aisec.cpg.helpers.TypeConverter;
import java.util.Objects;
import org.neo4j.ogm.annotation.typeconversion.Convert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TypeIdExpression extends Expression {

  private static final Logger log = LoggerFactory.getLogger(TypeIdExpression.class);

  @Convert(TypeConverter.class)
  private Type referencedType;

  private int operatorCode;

  public Type getReferencedType() {
    return referencedType;
  }

  public void setReferencedType(Type referencedType) {
    this.referencedType = referencedType;
  }

  public int getOperatorCode() {
    return operatorCode;
  }

  public enum Operator {
    SIZEOF(0),
    TYPEID(1),
    ALIGNOF(2),
    TYPEOF(3),
    HAS_NOTHROW_ASSIGN(4),
    HAS_NOTHROW_COPY(5),
    HAS_NOTHROW_CONSTRUCTOR(6),
    HAS_TRIVIAL_ASSIGN(7),
    HAS_TRIVIAL_COPY(8),
    HAS_TRIVIAL_CONSTRUCTOR(9),
    HAS_TRIVIAL_DESTRUCTOR(10),
    IS_ABSTRACT(11),
    IS_CLASS(12);

    Operator(int operatorCode) {
      this.operatorCode = operatorCode;
    }

    int operatorCode;

    public int getOperatorCode() {
      return this.operatorCode;
    }
  }

  public void setOperatorCode(int operatorCode) {
    this.operatorCode = operatorCode;
    switch (operatorCode) {
      case 0:
        name = "sizeof";
        break;
      case 1:
        name = "typeid";
        break;
      case 2:
        name = "alignof";
        break;
      case 3:
        name = "typeof";
        break;
      case 4:
        name = "has_nothrow_assign";
        break;
      case 5:
        name = "has_nothrow_copy";
        break;
      case 6:
        name = "has_nothrow_constructor";
        break;
      case 7:
        name = "has_trivial_assign";
        break;
      case 8:
        name = "has_trivial_copy";
        break;
      case 9:
        name = "has_trivial_constructor";
        break;
      case 10:
        name = "has_trivial_destructor";
        break;
      case 11:
        name = "has_virtual_destructor";
        break;
      case 12:
        name = "is_abstract";
        break;
      case 13:
        name = "is_class";
        break;
      case 14:
        name = "is_empty";
        break;
      case 15:
        name = "is_enum";
        break;
      case 16:
        name = "is_pod";
        break;
      case 17:
        name = "is_polymorphic";
        break;
      case 18:
        name = "is_union";
        break;
      case 19:
        name = "is_literal_type";
        break;
      case 20:
        name = "is_standard_layout";
        break;
      case 21:
        name = "is_trivial";
        break;
      case 22:
        name = "sizeofParameterPack";
        break;
      case 23:
        name = "is_final";
        break;
      case 24:
        name = "is_trivially_copyable";
        break;
      default:
        log.error("unknown operator {}", operatorCode);
    }
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
        && operatorCode == that.operatorCode
        && Objects.equals(referencedType, that.referencedType);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }
}
