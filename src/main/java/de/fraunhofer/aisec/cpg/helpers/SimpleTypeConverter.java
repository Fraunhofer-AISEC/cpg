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

package de.fraunhofer.aisec.cpg.helpers;

import de.fraunhofer.aisec.cpg.graph.Type;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.neo4j.ogm.typeconversion.AttributeConverter;

public class SimpleTypeConverter implements AttributeConverter<Type, String> {

  private static final Pattern pattern = Pattern.compile("(?<type>.*)(?: (?<adjustment>.*))?");

  private Type stringToType(String value) {
    Matcher matcher = pattern.matcher(value);
    if (matcher.matches()) {
      String type = matcher.group("type");
      if (type == null) {
        type = "";
      }
      String adjustment = matcher.group("adjustment");
      if (adjustment == null) {
        adjustment = "";
      }
      return new Type(type, adjustment);
    } else {
      return null;
    }
  }

  @Override
  public String toGraphProperty(Type value) {
    String property = value.getTypeName();
    if (!value.getTypeAdjustment().isEmpty()) {
      property += " " + value.getTypeAdjustment();
    }
    return property;
  }

  @Override
  public Type toEntityAttribute(String value) {
    return stringToType(value);
  }
}
