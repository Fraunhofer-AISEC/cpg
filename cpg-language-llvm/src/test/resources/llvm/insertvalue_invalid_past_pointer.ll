; This file is intentionally invalid LLVM IR, used to confirm that LLVM itself rejects indexing
; past a pointer-typed field within a single insertvalue instruction, for the same reason as
; gep_invalid_past_pointer.ll.
;
; LLVM's parser error for this file is "invalid indices for insertvalue".

%struct.Node = type { i32, %struct.Node* }

define { i32, %struct.Node* } @f({ i32, %struct.Node* } %agg, i32 %v) {
  %a = insertvalue { i32, %struct.Node* } %agg, i32 %v, 1, 0
  ret { i32, %struct.Node* } %a
}
