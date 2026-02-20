// Complex closures with different signatures
fn test_complex_closure() {
    let add = |a: i32, b: i32| -> i32 { a + b };
    let no_params = || 42;
    let multi_line = |x: i32| {
        let y = x + 1;
        y * 2
    };
}
