/*
 * Copyright (c) 2019, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.sarif;

import java.util.Objects;
import org.checkerframework.checker.nullness.qual.NonNull;

/** Code source location, in a SASP/SARIF-compliant "Region" format. */
public class Region implements Comparable<Region> {

  static final Region UNKNOWN_REGION = new Region();
  private int startLine;
  private int startColumn;
  private int endLine;
  private int endColumn;

  public Region(int startLine, int startColumn, int endLine, int endColumn) {
    this.startLine = startLine;
    this.startColumn = startColumn;
    this.endLine = endLine;
    this.endColumn = endColumn;
  }

  public Region() {
    this(-1, -1, -1, -1);
  }

  public int getStartLine() {
    return this.startLine;
  }

  public void setStartLine(int startLine) {
    this.startLine = startLine;
  }

  public int getStartColumn() {
    return startColumn;
  }

  public void setStartColumn(int startColumn) {
    this.startColumn = startColumn;
  }

  public int getEndLine() {
    return endLine;
  }

  public void setEndLine(int endLine) {
    this.endLine = endLine;
  }

  public int getEndColumn() {
    return endColumn;
  }

  public void setEndColumn(int endColumn) {
    this.endColumn = endColumn;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(startLine);
    sb.append(":");
    sb.append(startColumn);
    sb.append("-");
    sb.append(endLine);
    sb.append(":");
    sb.append(endColumn);
    return sb.toString();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof Region)) {
      return false;
    }
    Region that = (Region) obj;
    return (this.startLine == that.startLine
        && this.startColumn == that.startColumn
        && this.endLine == that.endLine
        && this.endColumn == that.endColumn);
  }

  @Override
  public int compareTo(@NonNull Region region) {
    int comparisonValue;
    if ((comparisonValue = Integer.compare(this.getStartLine(), region.getStartLine())) != 0)
      return comparisonValue;
    if ((comparisonValue = Integer.compare(this.getStartColumn(), region.getStartColumn())) != 0)
      return comparisonValue;

    if ((comparisonValue = Integer.compare(this.getEndLine(), region.getEndLine())) != 0)
      return -comparisonValue;
    if ((comparisonValue = Integer.compare(this.getEndColumn(), region.getEndColumn())) != 0)
      return -comparisonValue;

    return comparisonValue;
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.startColumn, this.startLine, this.endColumn, this.endLine);
  }
}
