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
package de.fraunhofer.aisec.cpg.helpers;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Benchmark {

  private static final Logger log = LoggerFactory.getLogger(Benchmark.class);
  private final String message;
  private boolean stoped = false;
  private final String caller;
  private final Instant start;
  private long duration = -1;

  private Benchmark parentBenchmark = null;
  protected List<Benchmark> childBenchmark = new ArrayList<>();

  public Benchmark(Class c, String message) {
    this.message = message;
    this.caller = c.getSimpleName();
    this.start = Instant.now();
  }

  public Benchmark(Class c, String message, Benchmark parentBenchmark) {
    this.message = message;
    this.caller = c.getSimpleName();
    this.start = Instant.now();
    this.parentBenchmark = parentBenchmark;
    parentBenchmark.childBenchmark.add(this);
  }

  public long stop() {
    if (!this.stoped) {
      childBenchmark.stream().forEach(benchm -> benchm.stop());
      duration = Duration.between(start, Instant.now()).toNanos() / 1000;
      log.info("{} {} done in {} mics", caller, message, duration);
      this.stoped = true;
    }
    return duration;
  }

  public long getDuration() {
    return duration;
  }

  /**
   * Durations have the associated caller as key. Other Metrics have a predefined static key
   *
   * @return
   */
  public Map<String, Object> getMetricMap() {
    Map<String, Object> ret = new HashMap<>();
    for (Benchmark child : this.childBenchmark) {
      Map<String, Object> tmp = child.getMetricMap();
      ret.putAll(tmp);
    }
    ret.put(caller, duration);
    return ret;
  }
}
