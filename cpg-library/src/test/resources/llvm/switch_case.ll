; External declaration of the rand function
declare i32 @rand() nounwind

; Definition of main function
define i32 @main() {   ; i32()*
  %x = call i32 @rand()
  %y = call i32 @rand()
  %z = mul i32 %y, 32768
  %a = xor i32 %z, %x

  switch i32 %a, label %otherwise [ i32 0, label %onzero
                                    i32 1, label %onone ]

onzero:
  %b = add i32 %a, 5
  ret i32 %b

onone:
  %c = sub i32 %a, 1
  ret i32 %c

otherwise:
  ret i32 %a
}

