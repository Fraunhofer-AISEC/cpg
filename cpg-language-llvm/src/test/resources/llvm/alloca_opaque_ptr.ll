; Same as alloca.ll, but written using LLVM's opaque pointer syntax (`ptr`) instead of the
; typed-pointer syntax (`i32*`). This makes sure the frontend resolves the allocated type of an
; `alloca` instruction (and the dereferenced type of a subsequent `store`/`load`) purely from the
; instruction's explicit type operands, since opaque pointers no longer carry any pointee type.
;
; int main() {
;   int *ptr = malloc(sizeof(int)); // -> alloca i32
;   *ptr = 3;                       // -> store i32 3, ptr %ptr
;   int val = *ptr;                 // -> load i32, ptr %ptr
;   return 0;
; }

define i32 @main() {
  %ptr = alloca i32
  store i32 3, ptr %ptr
  %val = load i32, ptr %ptr
  ret i32 0
}
