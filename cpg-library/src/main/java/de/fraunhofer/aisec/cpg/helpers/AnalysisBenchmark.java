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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AnalysisBenchmark extends Benchmark {

  private static final Logger log = LoggerFactory.getLogger(AnalysisBenchmark.class);

  public AnalysisBenchmark(Class c, String message) {
    super(c, message);
  }

  public AnalysisBenchmark(Class c, String message, Benchmark parentBenchmark) {
    super(c, message, parentBenchmark);
  }

  /**
   * Durations have the associated caller as key. Other Metrics have a predefined static key
   *
   * @return
   */
  public Map<String, Object> getMetricMap() {
    Map<String, Object> ret = super.getMetricMap();

    for (Benchmark child : this.childBenchmark) {
      if (child instanceof FileBenchmark) {
        FileBenchmark fileb = (FileBenchmark) child;
        Map<String, Object> childMetric = fileb.getMetricMap();
        ret.put(
            FileBenchmark.KEY_SOURCE_LOC,
            ((Integer) ret.get(FileBenchmark.KEY_SOURCE_LOC))
                + ((Integer) childMetric.get(FileBenchmark.KEY_SOURCE_LOC)));
      }
    }
    return ret;
  }
}
