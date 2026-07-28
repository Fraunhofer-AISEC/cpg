fn main() {
    let a = Some(10);
    let b = Some(20);
    // A let-chain. The condition contains two `let`
    // expressions connected by `&&`.
    if let Some(x) = a && let Some(y) = b {
        println!("Case 2: x = {x}, y = {y}");
    }
}