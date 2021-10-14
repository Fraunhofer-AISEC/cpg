; External declaration of the rand function
declare i32 @rand() nounwind
@a = global i32 8

define i32 @main() {   ; i32()*
  %a = call i32 @rand()
  %locA = load i32, i32* @a
  %b = add i32 %a, %locA

  ret i32 %b
}