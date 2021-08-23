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
package de.fraunhofer.aisec.cpg_benchmark;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JavaFile {

  private static final Logger log = LoggerFactory.getLogger(JavaFile.class);
  private static Pattern packagePattern = Pattern.compile("^package (.*);$");

  private File path;
  private String packageName;
  private String className;

  public JavaFile(File path, String packageName, String className) {
    this.path = path;
    this.packageName = packageName;
    this.className = className;
  }

  public static JavaFile parse(File path) {
    try {
      BufferedReader reader = new BufferedReader(new FileReader(path));
      String packageName =
          reader
              .lines()
              .map(packagePattern::matcher)
              .filter(Matcher::matches)
              .map(m -> m.group(1))
              .findFirst()
              .orElse("");

      String className = path.getName().replace(".java", "");
      return new JavaFile(path, packageName, className);
    } catch (IOException e) {
      log.error("Could not parse file {}: {}", path, e.getMessage());
    }
    return null;
  }

  public File getPath() {
    return path;
  }

  public void setPath(File path) {
    this.path = path;
  }

  public String getPackageName() {
    return packageName;
  }

  public void setPackageName(String packageName) {
    this.packageName = packageName;
  }

  public String getClassName() {
    return className;
  }

  public void setClassName(String className) {
    this.className = className;
  }

  public String getFullyQualifiedName() {
    return packageName.isEmpty() ? className : packageName + "." + className;
  }

  public String getModulePrefix() {
    if (packageName.isEmpty()) {
      return path.getParent();
    }
    String packagePath = packageName.replace('.', File.separatorChar);
    if (!packagePath.isEmpty()) {
      // Ensure that this is a directory
      packagePath = File.separatorChar + packagePath + File.separatorChar;
    }
    if (path.toString().contains(packagePath)) {
      return path.toString().substring(0, path.toString().lastIndexOf(packagePath));
    }
    log.warn("Package path not contained in file path: {}", path.toString());
    return "";
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof JavaFile) {
      JavaFile other = (JavaFile) obj;
      return other.getPackageName().equals(this.getPackageName())
          && other.getPath().equals(this.getPath())
          && other.getClassName().equals(this.getClassName());
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return Objects.hash(path, packageName, className);
  }

  @Override
  public String toString() {
    return "JavaFile[" + getFullyQualifiedName() + ", path=" + path + "]";
  }
}
