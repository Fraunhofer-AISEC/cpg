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
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import org.neo4j.ogm.typeconversion.ConverterBasedCollectionConverter;

public class TypeSetConverter extends ConverterBasedCollectionConverter<Type, String> {

  public TypeSetConverter() {
    super(Set.class, new SimpleTypeConverter());
  }

  @Override
  public String[] toGraphProperty(Collection<Type> values) {
    Object[] graphProperties = super.toGraphProperty(values);
    return Arrays.stream(graphProperties).toArray(String[]::new);
  }
}
