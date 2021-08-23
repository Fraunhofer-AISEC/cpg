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

import de.fraunhofer.aisec.cpg.TranslationConfiguration;
import de.fraunhofer.aisec.cpg.TranslationManager;
import de.fraunhofer.aisec.cpg.TranslationResult;
import de.fraunhofer.aisec.cpg.graph.TypeManager;
import de.fraunhofer.aisec.cpg.graph.types.TypeParser;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import kotlin.Pair;
import org.apache.commons.cli.*;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.revwalk.RevCommit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CpgBenchmark {
  private static final Logger log = LoggerFactory.getLogger(CpgBenchmark.class);
  private final Path outputPath;
  private final ExecutorService executor;
  private int timeout;

  public CpgBenchmark(CommandLine cmd) throws IOException {
    executor = Executors.newSingleThreadExecutor();
    timeout = Integer.MAX_VALUE;
    if (cmd.hasOption("timeout")) {
      try {
        timeout = Integer.parseInt(cmd.getOptionValue("timeout"));
      } catch (NumberFormatException e) {
        log.error("Timeout value {} can't be parsed!", cmd.getOptionValue("timeout"));
        System.exit(1);
      }
    }

    outputPath = cmd.hasOption("output") ? Paths.get(cmd.getOptionValue("output")) : null;

    File done = new File("done-repos");
    done.createNewFile();

    BufferedReader reader = new BufferedReader(new FileReader(done));
    Set<String> doneRepos = reader.lines().collect(Collectors.toSet());
    reader.close();

    for (Option option : cmd.getOptions()) {
      if ("remote".equals(option.getLongOpt())) {
        for (String url : option.getValues()) {
          if (doneRepos.contains(url)) {
            log.info("Repo {} has already been analyzed, skipping.", url);
            continue;
          }
          handleRemote(url);
        }
      }
    }

    executor.shutdown();
  }

  private Git getRemote(String url) throws IOException {
    File destination = Files.createTempDirectory("cpg-benchmark-").toFile();
    destination.deleteOnExit();
    log.info("Cloning repo {} into {}", url, destination);
    return cloneRepo(url, destination);
  }

  private Git cloneRepo(String url, File destination) {
    try {
      return Git.cloneRepository().setURI(url).setDirectory(destination).call();
    } catch (GitAPIException e) {
      System.out.println("Error cloning repository at " + url);
      System.out.println(e.getMessage());
      return null;
    }
  }

  private void handleRemote(String url) {
    try (Git git = getRemote(url)) {
      File root = git.getRepository().getWorkTree();
      if (root != null && root.isDirectory()) {
        File newRoot = Files.createTempDirectory("cpg-benchmark-grouped-").toFile();
        newRoot.deleteOnExit();
        Map<String, TranslationResult> previousResults = null;
        git.branchCreate().setName("cpg-benchmark");

        output(url);

        for (RevCommit commit : git.log().setMaxCount(100).call()) {
          git.checkout().setName(commit.getName()).call();
          output("\tHEAD: " + git.getRepository().resolve(Constants.HEAD).getName());

          // Conventional result
          long startTime = System.nanoTime();
          Map<String, TranslationResult> conventionalResults = handle(root, newRoot, null);
          output("\t\tConventional time: " + (System.nanoTime() - startTime) / 1_000_000 + " ms");

          // Clear persistent state
          TypeParser.reset();
          TypeManager.reset();

          // Incremental result
          startTime = System.nanoTime();
          previousResults = handle(root, newRoot, previousResults);
          output("\t\tIncremental time: " + (System.nanoTime() - startTime) / 1_000_000 + " ms");

          log.info("Checking if both graphs are equal");
          for (String module : conventionalResults.keySet()) {
            assert previousResults.containsKey(module);
            CorrectnessCheck.assertGraphsAreEqual(
                conventionalResults.get(module).getTranslationUnits(),
                previousResults.get(module).getTranslationUnits());
          }
        }

        output("");

        markDone(url);
        FileUtils.deleteRecursive(root);
        FileUtils.deleteRecursive(newRoot);
      } else {
        log.error("Repository {} could not be checked out, skipping", root);
      }
    } catch (IOException e) {
      log.error("Repository {} could not be downloaded, skipping", url);
    } catch (Exception e) {
      log.error("Error during analysis!", e);
    }
  }

  private Map<String, TranslationResult> handle(
      File root, File newRoot, Map<String, TranslationResult> previousResults) throws Exception {
    log.info("Collecting Java files");
    List<JavaFile> javaFiles = findJavaFiles(root);
    Map<String, List<JavaFile>> modules = groupFilesByModule(javaFiles);
    Map<String, TranslationResult> resultMap = new HashMap<>();

    for (String module : modules.keySet()) {
      List<JavaFile> moduleFiles = modules.get(module);
      String moduleName = module.replace(File.separator, ".");
      log.info("Module {}: {} Java files. Grouping into packages", module, moduleFiles.size());
      File modulePath = new File(newRoot, moduleName);
      moduleFiles = moveFilesToPackage(moduleFiles, modulePath);

      log.info("Creating CPG for module {}", moduleName);
      long startTime = System.nanoTime();
      List<JavaFile> finalModuleFiles = moduleFiles;
      Future<Pair<Boolean, TranslationResult>> future =
          executor.submit(
              () -> {
                try {
                  return new Pair<>(
                      true,
                      generateCPG(
                          newRoot,
                          finalModuleFiles,
                          previousResults == null ? null : previousResults.get(module)));
                } catch (Exception e) {
                  log.error("Error during analysis", e);
                }
                return new Pair<>(false, null);
              });
      boolean complete = false;
      try {
        Pair<Boolean, TranslationResult> futureResult = future.get(timeout, TimeUnit.SECONDS);
        complete = futureResult.getFirst();
        if (complete) {
          resultMap.put(module, futureResult.getSecond());
        }
      } catch (TimeoutException e) {
        future.cancel(true);
      }
      log.info("CPG generated. Took {} ms", (System.nanoTime() - startTime) / 1_000_000);
      if (!complete) {
        log.warn("Only partially saved due to timeout");
      }
    }

    return resultMap;
  }

  private TranslationResult generateCPG(
      File root, List<JavaFile> javaFiles, TranslationResult previousResult)
      throws ExecutionException, InterruptedException {
    File[] files =
        javaFiles.stream().map(JavaFile::getPath).collect(Collectors.toList()).toArray(File[]::new);
    TranslationConfiguration config =
        TranslationConfiguration.builder()
            .sourceLocations(files)
            .topLevel(root)
            .defaultPasses()
            .typeSystemActiveInFrontend(false)
            .useParallelFrontends(true)
            .debugParser(true)
            .failOnError(false)
            .build();

    TranslationManager analyzer = TranslationManager.builder().config(config).build();

    if (previousResult == null) {
      return analyzer.analyze().get();
    }

    return analyzer.analyze(previousResult);
  }

  private Map<String, List<JavaFile>> groupFilesByModule(List<JavaFile> javaFiles) {
    return javaFiles.stream().collect(Collectors.groupingBy(JavaFile::getModulePrefix));
  }

  private List<JavaFile> moveFilesToPackage(List<JavaFile> javaFiles, File destination)
      throws IOException {
    List<JavaFile> result = new ArrayList<>();
    Map<String, List<JavaFile>> packages =
        javaFiles.stream().collect(Collectors.groupingBy(JavaFile::getPackageName));
    for (String p : packages.keySet()) {
      File packageDir = new File(destination, p.replaceAll("\\.", File.separator));
      if (!packageDir.exists()) {
        Files.createDirectories(packageDir.toPath());
      }
      for (JavaFile javaFile : packages.get(p)) {
        File newPath = new File(packageDir, javaFile.getClassName() + ".java");
        if (newPath.exists()) {
          log.warn("Java class duplicate! {}", javaFile.getFullyQualifiedName());
        } else {
          Files.copy(javaFile.getPath().toPath(), newPath.toPath());
          result.add(new JavaFile(newPath, javaFile.getPackageName(), javaFile.getClassName()));
        }
      }
    }
    log.info(
        "{}/{} files remaining, grouped into {} packages: {}",
        result.size(),
        javaFiles.size(),
        packages.keySet().size(),
        packages.keySet());
    return result;
  }

  private List<JavaFile> findJavaFiles(File root) throws IOException {
    return Files.walk(root.toPath())
        .map(Path::toFile)
        .filter(f -> f.getName().endsWith(".java"))
        .map(JavaFile::parse)
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  private void markDone(String repo) throws IOException {
    BufferedWriter writer = new BufferedWriter(new FileWriter("done-repos", true));
    writer.append(repo);
    writer.newLine();
    writer.close();
  }

  private void output(String message) {
    if (outputPath != null) {
      try {
        Files.write(
            outputPath,
            List.of(message),
            StandardCharsets.UTF_8,
            Files.exists(outputPath) ? StandardOpenOption.APPEND : StandardOpenOption.CREATE);
      } catch (final IOException e) {
        log.error("Could not write to {}", outputPath);
      }
    }
    System.out.println(message);
  }

  public static void main(String[] args) throws Exception {
    Options options = new Options();
    options.addOption("r", "remote", true, "Remote repository");
    options.addOption("o", "output", true, "Report output file");
    options.addOption("t", "timeout", true, "Timeout per repository in seconds");
    CommandLineParser parser = new DefaultParser();
    CommandLine cmd = parser.parse(options, args);

    if (cmd.getOptions().length == 0) {
      HelpFormatter formatter = new HelpFormatter();
      formatter.printHelp("CPG benchmark", options);
      System.exit(1);
    }

    new CpgBenchmark(cmd);
  }
}
