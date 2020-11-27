package de.fraunhofer.aisec.cpg;

import de.fraunhofer.aisec.cpg.graph.Node;
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker;
import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ExecuteCPG {

  public static void main(String[] args) {

    File fileLambda = new File("/home/kweiss/cpgsamples/lambda.cpp");
    File fileRef =
        new File(
            "/home/kweiss/cpg/src/main/java/de/fraunhofer/aisec/cpg/frontends/java/ClassReferences.java");
    File staticContextThis =
        new File(
            "/home/kweiss/cpg/src/main/java/de/fraunhofer/aisec/cpg/frontends/java/StaticContextThis.java");
    File differentInstancesAccessed =
        new File(
            "/home/kweiss/cpg/src/main/java/de/fraunhofer/aisec/cpg/frontends/java/DifferentInstancesAccessed.java");

    File instanceShenanagans =
        new File(
            "/home/kweiss/cpg/src/main/java/de/fraunhofer/aisec/cpg/frontends/java/InstanceShenanagans.java");
    File someObject =
        new File(
            "/home/kweiss/cpg/src/main/java/de/fraunhofer/aisec/cpg/frontends/java/SomeObject.java");

    File memberPointer = new File("/home/kweiss/cpg/src/test/resources/memberPointer.cpp");

    File dennisNullPointerSample = new File("/home/kweiss/cpg/src/test/resources/nullpointer.cpp");

    File inheritanceCPP = new File("/home/kweiss/cpg/src/test/resources/variables/inheritance.cpp");

    File inheritanceJava =
        new File("/home/kweiss/cpg/src/test/resources/variables/Inheritance.java");
    File[] files = new File[] {new File("/home/kweiss/BasicSlice.java")};
    assertNotNull(files);

    var config =
        TranslationConfiguration.builder()
            .sourceLocations(files)
            .defaultPasses()
            .debugParser(true)
            .build();
    var analyzer = TranslationManager.builder().config(config).build();
    try {
      var result = analyzer.analyze().get();
      var tu = result.getTranslationUnits().get(0);
      List<Node> nodes =
          findByPredicate(
              SubgraphWalker.flattenAST(tu),
              n ->
                  n != null
                      && n.getLocation() != null
                      && n.getLocation().getRegion().getStartLine() == 34);
      System.out.println(
          "                                                                           ");
    } catch (NullPointerException np) {
      throw np;
    } catch (Exception ie) {
      System.out.println("Execution was interrupted");
      ie.printStackTrace();
    }
  }

  public static <S extends Node> List<S> findByPredicate(
      Collection<S> nodes, Predicate<S> predicate) {
    return nodes.stream().filter(predicate).collect(Collectors.toList());
  }

  /** <em>Asserts</em> that {@code actual} is not {@code null}. */
  public static void assertNotNull(File... files) {
    for (File file : files) {
      if (file == null) {
        throw new RuntimeException("Some provided file was null");
      }
    }
  }
}
