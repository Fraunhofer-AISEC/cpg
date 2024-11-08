; External declaration of the rand function
declare i32 @rand() nounwind

; Definition of main function
define i32 @main() {   ; i32()*
  %x = call i32 @rand()
  %y = call i32 @rand()
  %a = mul i32 %y, 32768
  %b = add i32 %a, 5
  %c = sub i32 %a, %b
  %d = sdiv i32 %a, %x
  %e = srem i32 %a, %x
  %f = xor i32 %a, %x
  %g = udiv i32 %a, %x
  %h = urem i32 %a, %x
  %i = shl i32 %a, %x
  %j = lshr i32 %a, %x
  %k = ashr i32 %a, %x

  ret i32 %b
}
