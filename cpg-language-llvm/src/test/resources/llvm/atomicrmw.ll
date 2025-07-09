
define i32 @foo(i32* %ptr) nounwind uwtable readnone optsize ssp {
  %old1 = atomicrmw add i32* %ptr, i32 1 acquire
  %old2 = atomicrmw sub i32* %ptr, i32 1 acquire
  %old3 = atomicrmw and i32* %ptr, i32 1 acquire
  %old4 = atomicrmw or i32* %ptr, i32 1 acquire
  %old5 = atomicrmw xor i32* %ptr, i32 1 acquire
  atomicrmw nand i32* %ptr, i32 1 acquire
  %old7 = atomicrmw min i32* %ptr, i32 1 acquire
  %old8 = atomicrmw max i32* %ptr, i32 1 acquire
  %old9 = atomicrmw umin i32* %ptr, i32 1 acquire
  %old10 = atomicrmw umax i32* %ptr, i32 1 acquire

  %val_success = cmpxchg i32* %ptr, i32 5, i32 %old1 acq_rel monotonic ; yields  { i32, i1 }
  %value_loaded = extractvalue { i32, i1 } %val_success, 1

  ret i32 %old1
}