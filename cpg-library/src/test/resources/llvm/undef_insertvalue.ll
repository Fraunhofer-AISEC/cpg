define { i32, i8 } @foo() {
    %a = insertvalue {i32, i8} undef, i32 100, 0
    ret { i32, i8 } %a ; Return %a, a struct of values 100 and undef
}
