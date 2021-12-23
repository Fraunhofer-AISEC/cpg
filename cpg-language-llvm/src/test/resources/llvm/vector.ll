define i32 @main() {   ; i32()*
  ; For some reason, we cannot just assign the vector to %x or %y but have to make this effort...
  %x = shufflevector <4 x i32> <i32 10, i32 9, i32 6, i32 -100>, <4 x i32> undef, <4 x i32> <i32 0, i32 1, i32 2, i32 3>
  %y = shufflevector <4 x i32> <i32 15, i32 34, i32 99, i32 1000>, <4 x i32> undef, <4 x i32> <i32 0, i32 1, i32 2, i32 3>

  %z = extractelement <4 x i32> %x, i32 0

  %yMod = insertelement <4 x i32> %y, i32 8, i32 3

  %shuffled = shufflevector <4 x i32> %x, <4 x i32> %yMod, <3 x i32> <i32 1, i32 6, i32 7>

  ret i32 %z
}
