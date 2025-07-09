; External declaration of the rand function
declare i32 @rand() nounwind

; External declaration of the foo function
declare i32 @foo(i32) nounwind

; External declaration of the foo function
declare i32 @foo1(i1) nounwind

; declare i32 constant @x
@x = private constant i32 5

; Definition of main function
define i32 @main() {   ; i32()*
  %a = call i32 @foo(i32 add(i32 ptrtoint (i32* @x to i32), i32 5))
  %b = call i32 @foo(i32 sub(i32 ptrtoint (i32* @x to i32), i32 5))
  %c = call i32 @foo(i32 mul(i32 ptrtoint (i32* @x to i32), i32 5))
  %d = call i32 @foo(i32 shl(i32 ptrtoint (i32* @x to i32), i32 5))
  %e = call i32 @foo(i32 lshr(i32 ptrtoint (i32* @x to i32), i32 5))
  %f = call i32 @foo(i32 xor(i32 ptrtoint (i32* @x to i32), i32 5))
  %g = call i32 @foo1(i1 icmp eq (i32 ptrtoint (i32* @x to i32), i32 5))

  ret i32 %a
}

