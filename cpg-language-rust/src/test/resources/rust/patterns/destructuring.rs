// Destructuring fixtures
fn test_destructuring() {
    let (a, b) = (1, 2);
    let sum = a + b;
    let _ = sum;
}

fn test_destructuring_nested() {
    let ((x, y), z) = ((1, 2), 3);
    let _ = (x, y, z);
}

fn test_simple_let() {
    let x = 42;
}
