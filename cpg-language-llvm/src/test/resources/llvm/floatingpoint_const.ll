; declare i32 constant @x
@x = private constant half 1.25

; Definition of main function
define half @main() {   ; half()*
  %a = fadd half 1.25, 1.0

  ret half %a
}
