// While/for/loop with labels
fn test_labeled_loops() {
    'outer: for i in 0..10 {
        'inner: for j in 0..10 {
            if i + j > 15 {
                break 'outer;
            }
            if j == 5 {
                continue 'inner;
            }
        }
    }
}
fn test_loop_with_break() -> i32 {
    let mut count = 0;
    loop {
        count += 1;
        if count >= 10 {
            break;
        }
    }
    count
}
fn test_while_condition() {
    let mut x = 0;
    while x < 10 {
        x += 1;
    }
}
