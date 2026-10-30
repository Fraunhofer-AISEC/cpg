; insertvalue into a struct field whose type is an opaque pointer. Mirrors
; gep_opaque_ptr_array.ll's concern for the GEP loop, but for the separate insertvalue loop in
; StatementHandler: baseType collapses to UnknownType for the ptr field, and indexing into it must
; not hit the "could not find structure type" failure mode.

define { i32, ptr } @foo(ptr %v) {
  %a = insertvalue { i32, ptr } undef, ptr %v, 1
  ret { i32, ptr } %a
}
