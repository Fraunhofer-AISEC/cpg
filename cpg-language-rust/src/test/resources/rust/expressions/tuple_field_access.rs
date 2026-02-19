// Tuple index expressions
fn test_tuple_index_simple() {
    let t = (1, "hello", 3.14);
    let a = t.0;
    let b = t.1;
    let c = t.2;
    let _ = (a, b, c);
}
fn test_tuple_index_nested() {
    let t = ((1, 2), (3, 4));
    let inner = t.0;
    let val = inner.1;
    let _ = val;
}
