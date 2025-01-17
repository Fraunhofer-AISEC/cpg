define i32 @main() {   ; i32()*
    %ptr = alloca i32
    store i32 undef, i32* %ptr
    %w = load i32, i32* %ptr
    %x = freeze i32 %w
    ret i32 %x
}