
define i32 @main() {   ; i32()*
    %y = alloca i32
    store i32 3, i32* %y
    %x = load i32, i32* %y
    ; "asm goto" without output constraints.
    callbr void asm "", "r,!i"(i32 %x)
            to label %fallthrough [label %indirect]

fallthrough: ; This is the fallthrough target
  %b = add i32 %x, 5
  ret i32 %b

indirect:
  ret i32 %x
}