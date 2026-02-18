// Branch coverage - StatementHandler uncovered branches

// let with mut_pattern
fn test_let_mut_pattern() {
    let mut x = 10;
    x += 1;
    let _ = x;
}

// ref_pattern in extractBindings
fn test_ref_pattern_binding() {
    let val = 42;
    match val {
        ref r => {
            let _ = r;
        }
    }
}

// mut_pattern in extractBindings
fn test_mut_pattern_binding() {
    let val = 42;
    match val {
        mut m => {
            m += 1;
            let _ = m;
        }
    }
}

// while let (while_expression with let_condition)
fn test_while_let() {
    let mut stack: Vec<i32> = vec![1, 2, 3];
    while let Some(top) = stack.pop() {
        let _ = top;
    }
}

