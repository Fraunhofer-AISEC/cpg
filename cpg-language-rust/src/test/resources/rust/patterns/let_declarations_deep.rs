// Let declarations - various forms
struct Point { x: i32, y: i32 }
fn test_let_mut() {
    let mut x = 5;
    x += 1;
    let _ = x;
}
fn test_let_type_annotation() {
    let x: i32 = 42;
    let y: f64 = 3.14;
    let z: bool = true;
    let s: &str = "hello";
    let _ = (x, y, z, s);
}
fn test_let_no_value() {
    let x: i32;
    x = 10;
    let _ = x;
}
fn test_let_mut_specifier() {
    let mut counter: i32 = 0;
    counter += 1;
    let _ = counter;
}
fn test_let_destructure_struct() {
    let p = Point { x: 1, y: 2 };
    let Point { x, y } = p;
    let _ = x + y;
}
