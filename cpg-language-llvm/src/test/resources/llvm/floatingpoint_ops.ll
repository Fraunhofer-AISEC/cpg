; External declaration of the rand function
declare half @rand() nounwind

; Definition of main function
define half @main() {   ; half()*
  %x = call half @rand()
  %y = call half @rand()
  %a = fmul half %y, %x
  %b = fadd half %a, %x
  %c = fsub half %a, %b
  %d = fdiv half %a, %x
  %e = frem half %a, %x
  %f = fneg half %e

  ret half %b
}