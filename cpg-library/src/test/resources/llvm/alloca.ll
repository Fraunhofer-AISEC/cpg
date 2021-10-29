define i32 @main() {   ; i32()*
    %ptr = alloca i32                               ; yields i32*:ptr
    store i32 3, i32* %ptr                          ; yields void
    %val = load i32, i32* %ptr                      ; yields i32:val = i32 3
    ret i32 0
}