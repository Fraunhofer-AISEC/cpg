; Same as global_local_var.ll, but written using LLVM's opaque pointer syntax (`ptr`). This makes
; sure the frontend resolves a global variable's type from the global's explicit value type (via
; LLVMGlobalGetValueType), since opaque pointers no longer carry any pointee type themselves.
;
; int rand(void);
; int a = 8;
; const int x = 10;
;
; int main() {
;   int a = rand();
;   int locX = x;
;   int locA = a;
;   int b = a + locA;
;   return b;
; }

declare i32 @rand() nounwind
@a = global i32 8
@x = constant i32 10

define i32 @main() {
  %a = call i32 @rand()
  %locX = load i32, ptr @x
  %locA = load i32, ptr @a
  %b = add i32 %a, %locA

  ret i32 %b
}
