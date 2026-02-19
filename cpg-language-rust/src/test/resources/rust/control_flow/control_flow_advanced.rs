// Negative literal (used in patterns, e.g., match arms)
fn test_negative_literal() -> i32 {
    let x = 5;
    match x {
        -1 => -100,
        0 => 0,
        _ => 1,
    }
}

// Continue with label
fn test_continue_with_label() {
    'outer: for i in 0..10 {
        for j in 0..10 {
            if j == 5 {
                continue 'outer;
            }
        }
    }
}

// Continue without label
fn test_continue_simple() {
    for i in 0..10 {
        if i == 5 {
            continue;
        }
    }
}

// Break with value
fn test_break_with_value() -> i32 {
    let result = loop {
        break 42;
    };
    result
}

// If-else chain
fn test_if_else_chain(x: i32) -> &'static str {
    if x < 0 {
        "negative"
    } else if x == 0 {
        "zero"
    } else {
        "positive"
    }
}

// While let
fn test_while_let() {
    let mut stack: Vec<i32> = Vec::new();
    while let Some(top) = stack.pop() {
        let _ = top;
    }
}

