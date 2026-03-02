// Field expression
struct Point { x: i32, y: i32 }
fn test_field_access() {
    let p = Point { x: 10, y: 20 };
    let a = p.x;
    let b = p.y;
    let _ = a + b;
}
