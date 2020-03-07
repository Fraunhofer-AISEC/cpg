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

package de.fraunhofer.aisec.cpg.sarif;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/** A SARIF compatible location referring to a location, i.e. file and region within the file. */
public class PhysicalLocation {

  @NonNull
  public static String locationLink(@Nullable PhysicalLocation location) {
    if (location != null) {
      return location.getArtifactLocation().getUri() + ":" + location.getRegion().getStartLine();
    }

    return "unknown";
  }

  public static class ArtifactLocation {

    @NonNull private String uri;

    public ArtifactLocation(@NonNull String uri) {
      this.uri = uri;
    }

    @NonNull
    public String getUri() {
      return this.uri;
    }
  }

  @NonNull private ArtifactLocation artifactLocation;

  @NonNull private Region region;

  public PhysicalLocation(@NonNull String uri, @NonNull Region region) {
    this.artifactLocation = new ArtifactLocation(uri);
    this.region = region;
  }

  public void setRegion(@NonNull Region region) {
    this.region = region;
  }

  @NonNull
  public Region getRegion() {
    return this.region;
  }

  @NonNull
  public ArtifactLocation getArtifactLocation() {
    return this.artifactLocation;
  }
}
