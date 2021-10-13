
define i32 @foo(i32* %ptr) nounwind uwtable readnone optsize ssp {
  %old = atomicrmw add i32* %ptr, i32 1 acquire
  ret i32 %old
}