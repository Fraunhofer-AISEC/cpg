package de.fraunhofer.aisec.cpg.graph.type;

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
