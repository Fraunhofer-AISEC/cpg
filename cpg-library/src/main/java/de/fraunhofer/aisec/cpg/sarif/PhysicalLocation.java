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
package de.fraunhofer.aisec.cpg.sarif;

import java.net.URI;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/** A SARIF compatible location referring to a location, i.e. file and region within the file. */
public class PhysicalLocation {

  @NonNull
  public static String locationLink(@Nullable PhysicalLocation location) {
    if (location != null) {
      return location.getArtifactLocation().getUri().getPath()
          + ":"
          + location.getRegion().getStartLine()
          + ":"
          + location.getRegion().getStartColumn();
    }

    return "unknown";
  }

  public static class ArtifactLocation {

    @NonNull private final URI uri;

    public ArtifactLocation(@NonNull URI uri) {
      this.uri = uri;
    }

    @NonNull
    public URI getUri() {
      return this.uri;
    }

    @Override
    public String toString() {
      return uri.getPath().substring(uri.getPath().lastIndexOf('/') + 1);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof ArtifactLocation)) return false;
      ArtifactLocation that = (ArtifactLocation) o;
      return Objects.equals(uri, that.uri);
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(uri);
    }
  }

  @NonNull private final ArtifactLocation artifactLocation;

  @NonNull private Region region;

  public PhysicalLocation(URI uri, @NonNull Region region) {
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

  @Override
  public String toString() {
    return artifactLocation.toString() + "(" + region.toString() + ")";
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof PhysicalLocation)) return false;
    PhysicalLocation that = (PhysicalLocation) o;
    return Objects.equals(artifactLocation, that.artifactLocation)
        && Objects.equals(region, that.region);
  }

  @Override
  public int hashCode() {
    return Objects.hash(artifactLocation, region);
  }
}
