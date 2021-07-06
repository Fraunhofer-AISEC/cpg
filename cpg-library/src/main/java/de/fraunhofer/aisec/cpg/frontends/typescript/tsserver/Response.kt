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

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "command")
sealed class Response<T>(
    var command: String,
    var success: Boolean,
    var message: String? = null,
    var body: T? = null,
    var metadata: Any? = null,
    @JsonProperty("request_seq") var requestSeq: Number = 0,
    seq: Number = 0
) : Message(seq, "request")

@JsonTypeName("unknown")
class UnknownResponse(success: Boolean) : Response<Any>("unknown", success)

@JsonTypeName("open") class OpenResponse(success: Boolean) : Response<Any>("open", success)

class Location(var line: Number, var offset: Number)

class TextSpan(var start: Location, var end: Location)

class NavigationTree(
    var text: String,
    var kind: String,
    var kindModifiers: String,
    var spans: List<TextSpan>,
    var nameSpan: TextSpan?,
    var childItems: List<NavigationTree>?
)

@JsonTypeName("navtree")
class NavTreeResponse(success: Boolean) : Response<NavigationTree>("navtree", success)
