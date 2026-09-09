fn expensive_computation() -> String {
    println!("Computing...");
    "Hello".to_string()
}

fn main() {
    let mut s = String::new();

    // Evaluate the right-hand side, assign it to `_`,
    // and immediately drop it.
    _ = expensive_computation();

    // You can also ignore the result of an assignment expression.
    _ = s.push_str("Rust");

    println!("{s}");
}