// If expressions - all branches
fn test_if_let(opt: Option<i32>) -> i32 {
    if let Some(x) = opt {
        x * 2
    } else {
        0
    }
}
fn test_if_let_no_else(opt: Option<i32>) {
    if let Some(x) = opt {
        let _ = x;
    }
}
fn test_if_else_if(x: i32) -> &'static str {
    if x < 0 {
        "negative"
    } else if x == 0 {
        "zero"
    } else if x < 10 {
        "small"
    } else {
        "large"
    }
}
fn test_if_simple(x: bool) -> i32 {
    if x {
        1
    } else {
        0
    }
}
