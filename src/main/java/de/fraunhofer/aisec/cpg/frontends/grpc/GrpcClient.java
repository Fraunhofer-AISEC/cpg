/*
 * Copyright (c) 2019, Fraunhofer AISEC. All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
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

import de.fraunhofer.aisec.cpg.frontends.TranslationException;
import de.fraunhofer.aisec.cpg.frontends.grpc.messages.CpgRequest;
import de.fraunhofer.aisec.cpg.frontends.grpc.messages.CpgResponse;
import de.fraunhofer.aisec.cpg.frontends.grpc.messages.TransferCpgGrpc;
import io.grpc.Channel;
import io.grpc.ManagedChannelBuilder;

public class GrpcClient {
  private final Channel channel;
  private final TransferCpgGrpc.TransferCpgBlockingStub blockingStub;

  public GrpcClient(String host, int port) {
    channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
    blockingStub = TransferCpgGrpc.newBlockingStub(channel);
  }

  public CpgResponse sendMessage(String filename) throws TranslationException {
    CpgRequest request = CpgRequest.newBuilder().setFilename(filename).build();
    try {
      CpgResponse response = blockingStub.transferCpg(request);
      return response;
    } catch (Exception e) {
      throw new TranslationException("Error in GrpcClient: " + e.getMessage());
    }
  }
}
