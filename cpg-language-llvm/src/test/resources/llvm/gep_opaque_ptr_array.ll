; Indexes through an array of opaque pointers (`ptr`). The GEP's source element type is itself
; an opaque pointer, which carries no pointee type. This checks that the index is still treated
; as an array/pointer access (a Subscription), not misinterpreted as a struct field access.
;
; void *getElem(void **arr, long idx) {
;   return arr[idx];
; }

define ptr @getElem(ptr %arr, i64 %idx) {
  %p = getelementptr ptr, ptr %arr, i64 %idx
  %elem = load ptr, ptr %p
  ret ptr %elem
}
