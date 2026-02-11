// Union declaration
union MyUnion {
    i: i32,
    f: f32,
}

// Extern block
extern "C" {
    fn printf(format: *const u8) -> i32;
}

// Extern crate
extern crate alloc;

// Inner attribute
#![allow(dead_code)]

// Empty statement in function
fn test_empty_statement() {
    ;
}

// Struct with impl for self parameter testing
struct Point {
    x: i32,
    y: i32,
}

impl Point {
    fn new(x: i32, y: i32) -> Point {
        Point { x, y }
    }

    fn distance(&self) -> f64 {
        0.0
    }

    fn reset(&mut self) {
        self.x = 0;
        self.y = 0;
    }

    fn consume(self) -> i32 {
        self.x + self.y
    }
}

// Trait with defaults and associated types
trait Shape {
    type Area;

    fn area(&self) -> Self::Area;

    fn name(&self) -> &str {
        "shape"
    }
}

// Generic function with where clause and multiple bounds
fn process<T, U>(input: T) -> U
where
    T: Clone + Send,
    U: Default,
{
    U::default()
}
