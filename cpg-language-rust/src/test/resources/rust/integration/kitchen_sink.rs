// Kitchen sink: exercises all supported Rust features for CPG
mod utils {
    fn helper() -> i32 {
        42
    }
}

// Structs and enums
struct Point {
    x: i32,
    y: i32,
}

enum Shape {
    Circle,
    Rectangle,
}

// Traits with default methods, associated types, and generics
trait Drawable {
    type Color;
    fn draw(&self);
    fn description(&self) -> i32 {
        let default_val = 0;
        default_val
    }
}

impl Drawable for Point {
    type Color = i32;
    fn draw(&self) {
        let msg = 1;
    }
}

// Generic function with trait bounds and where clause
fn process<'a, T: Clone>(data: &'a T) -> i32 where T: Drawable {
    data.draw();
    0
}

// Async function
async fn fetch_data() -> i32 {
    let result = 42;
    result
}

// Control flow: if let, while let, match with guards, loop labels
fn control_flow() {
    let opt: i32 = 1;

    if opt > 0 {
        let x = opt;
    } else {
        let y = 0;
    }

    let mut counter = 0;
    'outer: loop {
        counter = counter + 1;
        if counter > 5 {
            break 'outer;
        }
    }

    match counter {
        0 => {
            let z = 0;
        }
        n if n > 3 => {
            let big = true;
        }
        _ => {
            let other = false;
        }
    }
}

// Ownership and borrowing
fn ownership(data: &mut i32) {
    let val = *data;
    let r = &val;
}

// Macro usage
fn use_macros() {
    println!("hello");
}

// Type alias
type Coordinate = i32;

// Derive attribute
#[derive(Clone)]
struct Config {
    value: i32,
}

// For-in loop
fn for_loop_demo() {
    let items = [1, 2, 3];
    for x in items {
        let y = x;
    }
    for i in 0..10 {
        let z = i;
    }
}

// References and closures
fn closures_and_refs() {
    let x = 5;
    let r = &x;
    let v = *r;
    let add = |a: i32, b: i32| a + b;
    let sum = add(1, 2);
}

// Struct expressions
fn struct_demo() {
    let p = Point { x: 1, y: 2 };
    let s = p.x + p.y;
}

// Constants and statics
const PI: i32 = 3;
static COUNT: i32 = 0;

// Indexing and casting
fn index_and_cast() {
    let arr = [1, 2, 3];
    let first = arr[0];
    let x = 42;
    let y = x as i64;
}
