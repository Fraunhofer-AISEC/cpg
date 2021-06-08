/*
 * Copyright (c) 2021, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.helpers;

import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import de.fraunhofer.aisec.cpg.sarif.Region;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileBenchmark extends Benchmark {

  public static final String KEY_LOC = "LOC";
  public static final String KEY_SOURCE_LOC = "SLOC";

  private int loc = 0;

  /** Contains the linenumbers of the file analysis that only contains whitespace or comments. */
  private SortedSet<Integer> emptyLines = new TreeSet<Integer>();

  private SortedSet<Integer> codeLines = new TreeSet<Integer>();

  private static final Logger log = LoggerFactory.getLogger(FileBenchmark.class);

  public FileBenchmark(Class c, String message) {
    super(c, message);
  }

  public FileBenchmark(Class c, String message, Benchmark parentBenchmark) {
    super(c, message, parentBenchmark);
  }

  public int getLoc() {
    return loc;
  }

  public void setLoc(int loc) {
    this.loc = loc;
  }

  public int getSLoc() {
    return this.loc - getEmptyLines().size();
  }

  public SortedSet<Integer> getEmptyLines() {
    return emptyLines;
  }

  public void setEmptyLines(SortedSet<Integer> emptyLines) {
    this.emptyLines = emptyLines;
  }

  public SortedSet<Integer> getCodeLines() {
    return codeLines;
  }

  public void setCodeLines(SortedSet<Integer> codeLines) {
    this.codeLines = codeLines;
  }

  /**
   * Durations have the associated caller as key. Other Metrics have a predefined static key
   *
   * @return
   */
  public Map<String, Object> getMetricMap() {
    Map<String, Object> ret = super.getMetricMap();
    ret.put(KEY_LOC, this.loc);
    ret.put(KEY_SOURCE_LOC, getSLoc());
    return ret;
  }

  public static SortedSet<Integer> getLinesfromRegion(Region region){
    SortedSet<Integer> lines = new TreeSet<>();
    for(int i = region.getStartLine(); i <= region.getEndLine(); i++){
      lines.add(i);
    }
    return lines;
  }
}
