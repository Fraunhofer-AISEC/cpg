fn test_block_expr() -> i32 {
    let x = {
        let temp = 10;
        temp + 1
    };
    x
}

fn test_if_expr_value() -> i32 {
    let y = if true { 1 } else { 2 };
    y
}
