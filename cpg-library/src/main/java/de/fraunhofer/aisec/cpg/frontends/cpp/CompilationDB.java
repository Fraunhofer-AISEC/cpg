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
package de.fraunhofer.aisec.cpg.frontends.cpp;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class CompilationDB {

  public static List<String> getIncludeFIlesInTheFolders(List<String> includeFilesDirectories) {
    List<String> includeFiles = new LinkedList<>();
    includeFilesDirectories.forEach(
        item -> {
          try {
            List<String> filesInFolder =
                Files.walk(Paths.get(item))
                    .filter(Files::isRegularFile)
                    .map(Path::toString)
                    .collect(Collectors.toList());
            includeFiles.addAll(filesInFolder);
          } catch (IOException e) {
            e.printStackTrace();
          }
        });
    return includeFiles;
  }

  public static List<String> getIncludeDirectories(String stringVal) {
    if (stringVal == null || stringVal.length() == 0) {
      return null;
    }
    // get all the -I flag files
    List<String> words = List.of(stringVal.split(" "));
    List<String> includeFilesDirectories = new LinkedList<>();
    for (String word : words) {
      if (word.startsWith("-I")) {
        includeFilesDirectories.add(word.substring(2)); // adds the directory excluding the -I field
      }
    }
    return includeFilesDirectories;
  }

  public static List<String> getIncludeDirectories(List<String> commandVals) {
    //     ['clang', 'main.c', '-o', 'main.c.o'],
    // The I vals come after -I
    List<String> includeFilesDirectories = new LinkedList<>();
    for (int i = 0; i < commandVals.size(); i++) {
      if (commandVals.get(i) != null && commandVals.get(i).startsWith("-I")) {
        if (i + 1 != commandVals.size()) {
          includeFilesDirectories.add(commandVals.get(i + 1));
        }
      }
    }
    return includeFilesDirectories;
  }
}
