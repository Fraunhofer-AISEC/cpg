define i32 @main() {   ; i32()*
  %vec = insertelement <4 x i32> <i32 poison, i32 0, i32 0, i32 0>, i32 5, i32 0
  ret i32 0
}
