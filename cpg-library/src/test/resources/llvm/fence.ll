@a = global i32 8

define i32 @main() {   ; i32()*
  fence acquire
  %locA = load i32, i32* @a

  fence syncscope("scope") seq_cst
  %locA2 = load i32, i32* @a

  ret i32 1
}