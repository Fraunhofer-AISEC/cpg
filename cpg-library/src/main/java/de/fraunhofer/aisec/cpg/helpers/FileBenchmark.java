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

import de.fraunhofer.aisec.cpg.graph.Node;
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation;
import de.fraunhofer.aisec.cpg.sarif.Region;
import java.util.Map;
import java.util.SortedSet;
import java.util.Stack;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileBenchmark extends Benchmark {

  public static final String KEY_LOC = "LOC";
  public static final String KEY_SOURCE_LOC = "SLOC";
  public static final String COVERED = "COVERED";
  public static final String UNCOVERED = "UNCOVERED";
  public static final String PARTIAL = "PARTIAL";

  private Stack<LineCoverageFrame> lineCoverageStack = new Stack<>();

  private int loc = 0;

  /** Contains the linenumbers of the file analysis that only contains whitespace or comments. */
  private SortedSet<Integer> emptyLines = new TreeSet<Integer>();

  private SortedSet<Integer> codeLines = new TreeSet<Integer>();

  private int covered = -1;
  private int uncovered = -1;
  private int partial = -1;
  private int sloc = -1;

  private PhysicalLocation rootLocation = null;

  private static final Logger log = LoggerFactory.getLogger(FileBenchmark.class);

  public FileBenchmark(Class c, String message) {
    super(c, message);
  }

  public FileBenchmark(Class c, String message, Benchmark parentBenchmark) {
    super(c, message, parentBenchmark);
  }

  public int getCovered() {
    return covered;
  }

  public void setCovered(int covered) {
    this.covered = covered;
  }

  public int getUncovered() {
    return uncovered;
  }

  public void setUncovered(int uncovered) {
    this.uncovered = uncovered;
  }

  public int getPartial() {
    return partial;
  }

  public void setPartial(int partial) {
    this.partial = partial;
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
    ret.put(KEY_SOURCE_LOC, this.sloc);
    ret.put(COVERED, this.covered);
    ret.put(UNCOVERED, this.uncovered);
    ret.put(PARTIAL, this.partial);
    return ret;
  }

  public static SortedSet<Integer> getLinesfromRegion(Region region) {
    SortedSet<Integer> lines = new TreeSet<>();
    for (int i = region.getStartLine(); i <= region.getEndLine(); i++) {
      lines.add(i);
    }
    return lines;
  }

  public void pushNewLoCFrame(PhysicalLocation location) {
    this.lineCoverageStack.push(new LineCoverageFrame());
  }

  public void popLoCFrame(Node node) {
    LineCoverageFrame child = this.lineCoverageStack.pop();
    if (this.lineCoverageStack.empty()) {
      child.removeEmptyLines(this.emptyLines);
      SortedSet<Integer> coveredLines = child.covered;
      SortedSet<Integer> uncoveredLines = child.uncovered;
      SortedSet<Integer> partialLines = child.partial;
      coveredLines.removeAll(emptyLines);
      uncoveredLines.removeAll(emptyLines);
      partialLines.removeAll(emptyLines);

      this.covered = coveredLines.size();
      this.uncovered = uncoveredLines.size();
      this.partial = partialLines.size();

      this.sloc = this.getSLoc();

      for (Benchmark childBench : childBenchmark) {
        if (childBench instanceof FileBenchmark) {
          FileBenchmark cfb = (FileBenchmark) childBench;
          this.covered += cfb.covered;
          this.uncovered += cfb.uncovered;
          this.partial += cfb.partial;
          this.sloc += cfb.sloc;
        }
      }

      log.info(
          "Filebench at:"
              + node.getClass().getSimpleName()
              + "Total SLoc: "
              + sloc
              + " Covered: "
              + this.covered
              + " Uncovered: "
              + this.uncovered
              + " Partial: "
              + this.partial
              + " CoveredRelative: "
              + (1.0 * this.covered / sloc)
              + " UncoveredRelative: "
              + (1.0 * this.uncovered / sloc)
              + " PartialRelative: "
              + (1.0 * this.partial / sloc));
      if (this.covered > sloc || this.uncovered > sloc || this.partial > sloc) {
        throw new RuntimeException("Computation of Source code lines and coverage faulty");
      }

    } else {
      this.lineCoverageStack.peek().mergeChildFrame(child);
    }
  }

  public void handleCovered(Object ret, boolean handledSpecifically) {
    if (ret != null && ((Node) ret).getLocation() != null) {
      SortedSet<Integer> lines =
          FileBenchmark.getLinesfromRegion(((Node) ret).getLocation().getRegion());
      if (handledSpecifically) {
        this.wrapUpTop(((Node) ret).getLocation().getRegion());
      } else {
        this.lineCoverageStack.peek().addUncoverdLines(lines);
      }
    }
    this.popLoCFrame((Node) ret);
  }

  public void wrapUpTop(Region region) {
    this.lineCoverageStack.peek().wrapUp(region);
  }

  public static class LineCoverageFrame {
    private SortedSet<Integer> covered = new TreeSet<Integer>();

    private SortedSet<Integer> uncovered = new TreeSet<Integer>();
    private SortedSet<Integer> partial = new TreeSet<Integer>();

    public void addCoveredLines(SortedSet<Integer> covered) {
      SortedSet<Integer> intersection = intesect(covered, this.uncovered);

      this.covered.addAll(covered);

      this.covered.removeAll(intersection);
      this.uncovered.removeAll(intersection);

      this.partial.addAll(intersection);
    }

    public void addUncoverdLines(SortedSet<Integer> uncovered) {
      SortedSet<Integer> intersection = intesect(uncovered, this.covered);

      this.uncovered.addAll(uncovered);

      this.covered.removeAll(intersection);
      this.uncovered.removeAll(intersection);

      this.partial.addAll(intersection);
    }

    /**
     * Adds all lines that are not covered, uncovered, partial by its children to the covered list.
     *
     * @param region
     */
    public void wrapUp(Region region) {
      SortedSet<Integer> nodeLines = FileBenchmark.getLinesfromRegion(region);

      nodeLines.removeAll(this.uncovered);
      nodeLines.removeAll(this.partial);

      this.covered.addAll(nodeLines);
    }

    public void removeEmptyLines(SortedSet<Integer> emptyLines) {
      this.covered.removeAll(emptyLines);
      this.uncovered.removeAll(emptyLines);
      this.partial.removeAll(emptyLines);
    }

    public static SortedSet<Integer> intesect(SortedSet<Integer> a, SortedSet<Integer> b) {
      return new TreeSet<>(a.stream().filter(b::contains).collect(Collectors.toSet()));
    }

    public void mergeChildFrame(LineCoverageFrame child) {
      this.addCoveredLines(child.covered);
      this.addUncoverdLines(child.uncovered);

      this.partial.addAll(child.partial);
      this.covered.removeAll(child.partial);
      this.uncovered.removeAll(child.partial);
    }
  }
}
