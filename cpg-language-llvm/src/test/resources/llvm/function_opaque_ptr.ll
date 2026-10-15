; Tests several instructions that involve opaque pointers:
;
;  - `define i32 @deref(ptr %p)` : a function with an opaque pointer parameter and a recoverable
;    (i32) return type. The return type is derived from the function's explicit value type
;    (LLVMGlobalGetValueType), not from the (now opaque) function pointer type.
;  - `load`                      : loads an i32 through an opaque pointer.
;  - `ptrtoint ... to i64`       : a cast instruction with an opaque pointer operand.
;  - `call ptr @malloc(i64 16)`  : a call instruction returning an opaque pointer. A bare `ptr`
;    result carries no pointee type, so its type resolves to an unknown type.
;
; int deref(int *p) { return *p; }
; long to_int(void *p) { return (long) p; }
; void *alloc() { return malloc(16); }

declare ptr @malloc(i64)

define i32 @deref(ptr %p) {
  %v = load i32, ptr %p
  ret i32 %v
}

define i64 @to_int(ptr %p) {
  %i = ptrtoint ptr %p to i64
  ret i64 %i
}

define ptr @alloc() {
  %p = call ptr @malloc(i64 16)
  ret ptr %p
}
