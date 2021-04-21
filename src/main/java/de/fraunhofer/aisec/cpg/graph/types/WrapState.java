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
package de.fraunhofer.aisec.cpg.graph.types;

/** Stores State for rewrap when typeinformation has been unwrapped */
public class WrapState {

  int depth;
  boolean reference;
  PointerType.PointerOrigin pointerOrigin;
  ReferenceType referenceType;

  public WrapState() {
    this.depth = 0;
    this.reference = false;
    this.pointerOrigin = PointerType.PointerOrigin.ARRAY;
    this.referenceType = null;
  }

  public int getDepth() {
    return depth;
  }

  public void setDepth(int depth) {
    this.depth = depth;
  }

  public boolean isReference() {
    return reference;
  }

  public void setReference(boolean reference) {
    this.reference = reference;
  }

  public PointerType.PointerOrigin getPointerOrigin() {
    return pointerOrigin;
  }

  public void setPointerOrigin(PointerType.PointerOrigin pointerOrigin) {
    this.pointerOrigin = pointerOrigin;
  }

  public ReferenceType getReferenceType() {
    return referenceType;
  }

  public void setReferenceType(ReferenceType referenceType) {
    this.referenceType = referenceType;
  }
}
