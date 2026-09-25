; Global values and functions with different LLVM IR linkage types, used to test the mapping of
; linkage onto the canonical visibility model.

; external is the default linkage and is not written out.
@publicGlobal = global i32 8
; internal linkage behaves like C's file-scope `static`.
@internalGlobal = internal global i32 8
; private linkage is confined to this module and not even exposed by name.
@privateGlobal = private global i32 8
; weak linkage does not restrict resolution in a way the canonical model captures.
@weakGlobal = weak global i32 8

define i32 @publicFunc() {
  ret i32 0
}

define internal i32 @internalFunc() {
  ret i32 0
}

define private i32 @privateFunc() {
  ret i32 0
}
