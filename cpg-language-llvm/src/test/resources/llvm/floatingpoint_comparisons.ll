; External declaration of the rand function
declare half @rand() nounwind

; Definition of main function
define half @main() {   ; i32()*
  %x = call half @rand()
  %y = call half @rand()

  %a = fcmp oeq half %x, %y
  %b = fcmp one half %x, %y
  %c = fcmp ogt half %x, %y
  %d = fcmp olt half %x, %y
  %e = fcmp oge half %x, %y
  %f = fcmp ole half %x, %y
  %g = fcmp ugt half %x, %y
  %h = fcmp ult half %x, %y
  %i = fcmp uge half %x, %y
  %j = fcmp ule half %x, %y
  %k = fcmp ueq half %x, %y
  %l = fcmp une half %x, %y
  %m = fcmp ord half %x, %y
  %n = fcmp uno half %x, %y

  ret half %x
}

