define { i32, i8 } @foo() {
    %a = insertvalue {i32, i8} undef, i32 100, 0
    %b = insertvalue {i32, i8} %a, i8 7, 1
    ret { i32, i8 } %b
}
