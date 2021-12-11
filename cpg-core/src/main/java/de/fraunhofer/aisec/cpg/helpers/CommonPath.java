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

import java.io.File;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.checkerframework.checker.nullness.qual.Nullable;

/** Find the common root path for a list of files */
public class CommonPath {

  // hide ctor
  private CommonPath() {}

  @Nullable
  public static File commonPath(Collection<File> paths) {
    if (paths.isEmpty()) {
      return null;
    }
    StringBuilder longestPrefix = new StringBuilder();
    List<String[]> splittedPaths =
        paths.stream()
            .map(File::getAbsolutePath)
            .map(p -> p.split(Pattern.quote(File.separator)))
            .sorted(Comparator.comparingInt(s -> s.length))
            .collect(Collectors.toList());

    String[] shortest = splittedPaths.get(0);
    for (int i = 0; i < shortest.length; i++) {
      String part = shortest[i];
      int position = i;
      if (splittedPaths.stream().allMatch(p -> p[position].equals(part))) {
        longestPrefix.append(part).append(File.separator);
      } else {
        break;
      }
    }

    File result = new File(longestPrefix.toString());
    if (result.exists()) {
      return getNearestDirectory(result);
    } else return null;
  }

  private static File getNearestDirectory(File file) {
    if (file.isDirectory()) {
      return file;
    } else {
      return getNearestDirectory(file.getParentFile());
    }
  }
}
