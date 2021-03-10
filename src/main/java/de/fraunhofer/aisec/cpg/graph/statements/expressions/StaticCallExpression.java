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

import de.fraunhofer.aisec.cpg.graph.declarations.RecordDeclaration;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * A {@link CallExpression} that targets a static function of a different {@link RecordDeclaration},
 * without using a static import: <code>SomeClass.invoke()</code>
 */
public class StaticCallExpression extends CallExpression {

  private String targetRecord;

  public String getTargetRecord() {
    return targetRecord;
  }

  @Override
  public void setName(@NonNull String name) {
    super.setName(name);
    updateFqn();
  }

  public void setTargetRecord(String targetRecord) {
    this.targetRecord = targetRecord;
    updateFqn();
  }

  private void updateFqn() {
    if (targetRecord != null && !targetRecord.isEmpty() && name != null && !name.isEmpty()) {
      setFqn(targetRecord + "." + name);
    }
  }
}
