// Struct expressions - all field init types
struct Point { x: i32, y: i32 }
fn test_struct_field_init() {
    let p = Point { x: 10, y: 20 };
    let _ = p;
}
fn test_struct_shorthand() {
    let x = 10;
    let y = 20;
    let p = Point { x, y };
    let _ = p;
}
fn test_struct_spread() {
    let p1 = Point { x: 1, y: 2 };
    let p2 = Point { x: 99, ..p1 };
    let _ = p2;
}
