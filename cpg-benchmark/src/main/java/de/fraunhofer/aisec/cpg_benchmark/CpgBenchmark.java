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
import java.util.function.BiFunction;
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
  private int timeout, numCommits;

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

    try {
      numCommits = Integer.parseInt(cmd.getOptionValue("commits"));
    } catch (NumberFormatException e) {
      log.error(
          "Number of commits to analyze ({}) can't be parsed!", cmd.getOptionValue("commits"));
      System.exit(1);
    }

    outputPath = cmd.hasOption("output") ? Paths.get(cmd.getOptionValue("output")) : null;

    File done = new File("done-repos");
    done.createNewFile();

    BufferedReader reader = new BufferedReader(new FileReader(done));
    Set<String> doneRepos = reader.lines().collect(Collectors.toSet());
    reader.close();

    for (Option option : cmd.getOptions()) {
      switch (option.getLongOpt()) {
        case "remote":
          for (String url : option.getValues()) {
            if (doneRepos.contains(url)) {
              log.info("Repo {} has already been analyzed, skipping.", url);
              continue;
            }
            try (Git git = getRemote(url)) {
              output(url);

              handleRepo(git, cmd.hasOption("verify-equivalence"));

              output("");
              markDone(url);
              FileUtils.deleteRecursive(git.getRepository().getWorkTree());
            } catch (Exception e) {
              log.error("Error during analysis", e);
            }
          }
          break;
        case "local":
          for (String path : option.getValues()) {
            try (Git git = Git.open(new File(path))) {
              output(path);
              handleRepo(git, cmd.hasOption("verify-equivalence"));
              output("");
            } catch (Exception e) {
              log.error("Error during analysis", e);
            }
          }
          break;
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

  private void handleRepo(Git git, boolean verifyEquivalence) throws Exception {
    File root = git.getRepository().getWorkTree();
    if (root != null && root.isDirectory()) {
      File newRoot = Files.createTempDirectory("cpg-benchmark-grouped-").toFile();
      newRoot.deleteOnExit();
      Map<String, TranslationResult> previousResults = null;
      git.branchCreate().setName("cpg-benchmark");

      List<RevCommit> commits = new ArrayList<>();
      git.log().setMaxCount(numCommits).call().forEach(commits::add);
      Collections.reverse(commits);

      for (RevCommit commit : commits) {
        git.checkout().setName(commit.getName()).call();
        output("\tHEAD: " + git.getRepository().resolve(Constants.HEAD).getName());

        // Conventional result
        long startTime = System.nanoTime();
        Map<String, TranslationResult> conventionalResults =
            handle(
                root,
                newRoot,
                (module, files) -> {
                  try {
                    return new Pair<>(true, generateConventionalCPG(newRoot, files));
                  } catch (Exception e) {
                    log.error("Error during analysis", e);
                  }
                  return new Pair<>(false, null);
                });
        output("\t\tConventional time: " + (System.nanoTime() - startTime) / 1_000_000 + " ms");

        // Clear persistent state
        TypeParser.reset();
        TypeManager.reset();

        // Incremental result
        startTime = System.nanoTime();
        Map<String, TranslationResult> finalPreviousResults = previousResults;
        previousResults =
            handle(
                root,
                newRoot,
                (module, files) -> {
                  try {
                    return new Pair<>(
                        true,
                        generateIncrementalCPG(
                            newRoot,
                            files,
                            finalPreviousResults == null
                                ? null
                                : finalPreviousResults.get(module)));
                  } catch (Exception e) {
                    log.error("Error during incremental analysis", e);
                  }
                  return new Pair<>(false, null);
                });
        output("\t\tIncremental time: " + (System.nanoTime() - startTime) / 1_000_000 + " ms");

        if (verifyEquivalence) {
          log.info("Checking if both graphs are equal");
          for (String module : conventionalResults.keySet()) {
            assert previousResults.containsKey(module);
            CorrectnessCheck.assertGraphsAreEqual(
                conventionalResults.get(module).getTranslationUnits(),
                previousResults.get(module).getTranslationUnits());
          }
        }
      }

      FileUtils.deleteRecursive(newRoot);
    } else {
      log.error("Repository {} could not be checked out, skipping", root);
    }
  }

  private Map<String, TranslationResult> handle(
      File root,
      File newRoot,
      BiFunction<String, List<JavaFile>, Pair<Boolean, TranslationResult>> futureFactory)
      throws Exception {
    log.info("Collecting Java files");
    List<JavaFile> javaFiles = findJavaFiles(root);
    Map<String, List<JavaFile>> modules = groupFilesByModule(javaFiles);
    Map<String, TranslationResult> resultMap = new HashMap<>();

    for (String module : modules.keySet()) {
      List<JavaFile> moduleFiles = modules.get(module);
      String moduleName = module.replace(File.separator, ".");
      log.info("Module {}: {} Java files. Grouping into packages", module, moduleFiles.size());
      File modulePath = new File(newRoot, moduleName);
      if (modulePath.exists()) {
        FileUtils.deleteRecursive(modulePath);
      }
      moduleFiles = moveFilesToPackage(moduleFiles, modulePath);

      log.info("Creating CPG for module {}", moduleName);
      long startTime = System.nanoTime();
      List<JavaFile> finalModuleFiles = moduleFiles;
      Future<Pair<Boolean, TranslationResult>> future =
          executor.submit(() -> futureFactory.apply(module, finalModuleFiles));

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

  private TranslationResult generateConventionalCPG(File root, List<JavaFile> javaFiles)
      throws ExecutionException, InterruptedException {
    File[] files =
        javaFiles.stream().map(JavaFile::getPath).collect(Collectors.toList()).toArray(File[]::new);
    TranslationConfiguration config =
        TranslationConfiguration.builder()
            .defaultLanguages()
            .sourceLocations(files)
            .topLevel(root)
            .defaultPasses()
            .typeSystemActiveInFrontend(true)
            .useParallelFrontends(false)
            .debugParser(true)
            .failOnError(true)
            .build();

    TranslationManager analyzer = TranslationManager.builder().config(config).build();

    return analyzer.analyze().get();
  }

  private TranslationResult generateIncrementalCPG(
      File root, List<JavaFile> javaFiles, TranslationResult previousResult)
      throws ExecutionException, InterruptedException {
    File[] files =
        javaFiles.stream().map(JavaFile::getPath).collect(Collectors.toList()).toArray(File[]::new);
    TranslationConfiguration config =
        TranslationConfiguration.builder()
            .defaultLanguages()
            .sourceLocations(files)
            .topLevel(root)
            .defaultPasses()
            .typeSystemActiveInFrontend(true)
            .useParallelFrontends(false)
            .debugParser(true)
            .failOnError(true)
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
    options.addOption("l", "local", true, "Local repository");
    options.addOption("r", "remote", true, "Remote repository");
    options.addOption("o", "output", true, "Report output file");
    options.addOption("t", "timeout", true, "Timeout per repository in seconds");
    options.addOption(
        "v",
        "verify-equivalence",
        false,
        "Check whether the conventional and incremental graphs are equal");
    options.addRequiredOption("n", "commits", true, "Number of commits to analyze");
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
