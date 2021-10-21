; External declaration of the rand function
declare i32 @rand() nounwind

; Definition of main function
define i32 @main() {   ; i32()*
  %x = call i32 @rand()
  %cond = icmp eq i32 %x, 10
  br i1 %cond, label %IfEqual, label %IfUnequal

IfEqual:
  %condUnsigned = icmp ugt i32 %x, -3
  br i1 %cond, label %Target2, label %IfUnequal

Target2:
  ret i32 1

IfUnequal:
  %y = mul i32 %x, 32768
  br label %randomTarget

onzero: ; This is never executed...
  %b = add i32 %x, 5
  ret i32 %b

randomTarget:
  ret i32 %y
}

