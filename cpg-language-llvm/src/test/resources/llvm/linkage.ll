; Global values and functions with different LLVM IR linkage types, used to test the mapping of
; linkage onto the canonical visibility model.

; external is the default linkage and is not written out. It is visible everywhere and, like C's
; external linkage, carries no access-control restriction, so it stays UNKNOWN with no modifier.
@externalGlobal = global i32 8
; internal linkage behaves like C's file-scope `static`.
@internalGlobal = internal global i32 8
; private linkage is confined to this module and not even exposed by name.
@privateGlobal = private global i32 8
; weak linkage does not restrict resolution in a way the canonical model captures, but the raw
; keyword is still recorded losslessly in the modifiers.
@weakGlobal = weak global i32 8
; common linkage likewise leaves the visibility UNKNOWN while recording its raw keyword.
@commonGlobal = common global i32 0

define i32 @externalFunc() {
  ret i32 0
}

define internal i32 @internalFunc() {
  ret i32 0
}

define private i32 @privateFunc() {
  ret i32 0
}

; A function with a non-internal, non-external linkage: visibility stays UNKNOWN, keyword recorded.
define weak i32 @weakFunc() {
  ret i32 0
}
