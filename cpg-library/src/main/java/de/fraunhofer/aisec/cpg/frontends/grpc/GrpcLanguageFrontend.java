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
package de.fraunhofer.aisec.cpg.frontends.grpc;

import static java.util.Collections.nCopies;

import de.fraunhofer.aisec.cpg.TranslationConfiguration;
import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend;
import de.fraunhofer.aisec.cpg.frontends.TranslationException;
import de.fraunhofer.aisec.cpg.frontends.grpc.messages.CpgResponse;
import de.fraunhofer.aisec.cpg.graph.*;
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration;
import de.fraunhofer.aisec.cpg.passes.scopes.ScopeManager;
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation;
import de.fraunhofer.aisec.cpg.sarif.Region;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.util.*;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/** Parser for the gRPC interface */
public class GrpcLanguageFrontend extends LanguageFrontend {

  public static final List<String> GRPC_EXTENSIONS = List.of(".pr");

  public GrpcLanguageFrontend(@NonNull TranslationConfiguration config, ScopeManager scopeManager) {
    super(config, scopeManager, ".");
  }

  @Override
  public TranslationUnitDeclaration parse(File file) throws TranslationException {
    try {
      BufferedReader reader = new BufferedReader(new FileReader(file));
      String host = reader.readLine();
      int port = Integer.parseInt(reader.readLine());
      String filename = reader.readLine();

      GrpcClient client = new GrpcClient(host, port);
      CpgResponse response = client.sendMessage(filename);
      System.out.println(
          "Got a response with "
              + response.getNodesList().size()
              + " nodes of the following type: ");
      response.getNodesList().stream().forEach(n -> System.out.println(n.getClass()));

      // 1. Create a list of Nodes with the same length as response.getNodesList()
      // 2. Everything in the list is set to null
      // 3. Create the TranslationUnitDeclaration:
      //    a. This will then create a TranslationUnitDeclaration and set its parameters
      //    b. It will then iterate over its parts and check if they have been initialized
      //    c. If they have, it will use these references. If not, it will call the methods

      List<Node> cpgNodes = new ArrayList<>();
      cpgNodes.addAll(nCopies(response.getNodesList().size(), null));
      Node node =
          NodeFactory.createNode(
              response.getRoot().getNodeIndex(), response.getNodesList(), cpgNodes);

      System.out.println("Got it: " + cpgNodes.size());

      if (node instanceof TranslationUnitDeclaration) {
        return (TranslationUnitDeclaration) node;
      } else {
        throw new TranslationException("Root node is not a TranslationUnitDeclaration");
      }
    } catch (IOException e) {
      throw new TranslationException(e.getMessage());
    } catch (NumberFormatException e) {
      throw new TranslationException("Misformed .pr file (port is not a number)");
    }
  }

  @Override
  public <T> String getCodeFromRawNode(T astNode) {
    // TODO
    return "getCodeFromRawNode() from proto called";
  }

  @Override
  @Nullable
  public <T> PhysicalLocation getLocationFromRawNode(T astNode) {
    // TODO
    Region region = new Region(0, 0, 0, 1); // +1 for SARIF compliance

    return new PhysicalLocation(URI.create("/home/user/dummy/location"), region);
  }

  @Override
  public <S, T> void setComment(S s, T ctx) {
    // TODO
  }
}
