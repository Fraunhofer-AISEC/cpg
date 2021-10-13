
define i32 @foo(i32* %ptr) nounwind uwtable readnone optsize ssp {
  %old = atomicrmw add i32* %ptr, i32 1 acquire

  %val_success = cmpxchg i32* %ptr, i32 5, i32 %old acq_rel monotonic ; yields  { i32, i1 }
  %value_loaded = extractvalue { i32, i1 } %val_success, 1

  ret i32 %old
}