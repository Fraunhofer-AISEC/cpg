fn classify(x: i32) -> i32 {
    if x == 0 {
        return 42;
    }
    x
}

fn early_return(x: i32) -> i32 {
    let val1 = if x > 0 { return 1; } else { 0 };
    val1
}
