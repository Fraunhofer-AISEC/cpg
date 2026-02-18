// Closures
fn test_closure_as_arg() {
    let v = vec![1, 2, 3, 4, 5];
    let evens: Vec<&i32> = v.iter().filter(|x| **x % 2 == 0).collect();
    let sum: i32 = v.iter().map(|x| x * 2).sum();
    let _ = (evens, sum);
}
fn test_closure_with_move() {
    let s = String::from("hello");
    let greeting = move || s.len();
    let _ = greeting();
}
