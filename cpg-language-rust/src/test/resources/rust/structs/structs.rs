// Consolidated struct resources

struct MyStruct {
    field1: i32,
    field2: bool,
}

impl MyStruct {
    fn my_method(self, a: i32) {
    }
}

enum MyEnum {
    Variant1,
    Variant2,
}

struct Point {
    x: i32,
    y: i32,
}

impl Point {
    fn new(x: i32, y: i32) -> Point {
        Point { x: x, y: y }
    }

    fn sum(&self) -> i32 {
        self.x + self.y
    }
}

fn test_struct_expr() {
    let p = Point { x: 1, y: 2 };
    let s = p.sum();
}

// Struct with shorthand initializer
struct Config {
    width: i32,
    height: i32,
    debug: bool,
}

fn test_shorthand_init() {
    let width = 800;
    let height = 600;
    let debug = true;
    let config = Config { width, height, debug };
}

// Struct expressions - all field init types
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

