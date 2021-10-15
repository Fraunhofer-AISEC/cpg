; External declaration of the rand function
declare i32 @rand() nounwind
@a = global i32 8
@x = constant i32 10

define i32 @main() {   ; i32()*
  %a = call i32 @rand()
  %locX = load i32, i32* @x
  %locA = load i32, i32* @a
  %b = add i32 %a, %locA

  ret i32 %b
}