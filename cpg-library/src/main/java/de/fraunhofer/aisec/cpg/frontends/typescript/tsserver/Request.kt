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
package de.fraunhofer.aisec.cpg.frontends.typescript.tsserver

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "command")
sealed class Request(var command: String, seq: Number = 0) : Message(seq, "request")

sealed class FileRequest(command: String, var arguments: FileRequestArgs) : Request(command)

@JsonTypeName("open") class OpenRequest(var arguments: OpenRequestArgs) : Request("open")

@JsonTypeName("navtree")
class NavTreeRequest(arguments: FileRequestArgs) : FileRequest("navtree", arguments)

@JsonTypeName("encodedSemanticClassifications-full")
class EncodedSemanticClassificationsRequest(arguments: FileRequestArgs) :
    FileRequest("encodedSemanticClassifications-full", arguments)

open class FileRequestArgs(var file: String, var projectFileName: String? = null)

class OpenRequestArgs(
    var fileContent: String? = null,
    var scriptKindName: String? = null,
    var projectRootPath: String? = null,
    file: String,
    projectFileName: String?
) : FileRequestArgs(file, projectFileName)
