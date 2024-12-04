; External declaration of the rand function
declare i32 @rand() nounwind

; Definition of main function
define i32 @main() {   ; i32()*
  %x = call i32 @rand()

  %a = icmp eq i32 %x, 10
  %b = icmp ne i32 %x, 10
  %c = icmp ugt i32 %x, 10
  %d = icmp ult i32 %x, 10
  %e = icmp uge i32 %x, 10
  %f = icmp ule i32 %x, 10
  %g = icmp sgt i32 %x, 10
  %h = icmp slt i32 %x, 10
  %i = icmp sge i32 %x, 10
  %j = icmp sle i32 %x, 10

  ret i32 %x
}

