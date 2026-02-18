// Consolidated declarations fixtures
#![allow(dead_code)]

use std::collections;

// Const and static
const MAX_SIZE: i32 = 100;
static GLOBAL_COUNTER: i32 = 0;

fn use_constants() {
    let x = MAX_SIZE;
    let _ = (x, GLOBAL_COUNTER);
}

// Union declaration
union MyUnion {
    i: i32,
    f: f32,
}

// Extern block
extern "C" {
    fn printf(format: *const u8) -> i32;
    fn c_abs(x: i32) -> i32;
    fn c_strlen(s: *const u8) -> usize;
}

// Extern crate
extern crate alloc;

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

// Tuple struct declarations
struct Pair(i32, i32);
struct Triple(f64, f64, f64);
struct Wrapper(String);

fn test_tuple_structs() {
    let p = Pair(1, 2);
    let t = Triple(1.0, 2.0, 3.0);
    let w = Wrapper(String::from("hello"));
    let _ = (p, t, w);
}

// Trait with type parameters
trait Converter<T, U> {
    fn convert(&self, input: T) -> U;
}

// Mut parameter pattern
fn test_mut_param(mut x: i32) -> i32 {
    x += 10;
    x
}

// Attribute propagation in trait body
trait Annotated {
    #[must_use]
    fn compute(&self) -> i32;
    fn describe(&self) -> &str;
}

// Attribute propagation in impl body
struct MyType {
    value: i32,
}

impl MyType {
    #[inline]
    fn get_value(&self) -> i32 {
        self.value
    }

    fn set_value(&mut self, v: i32) {
        self.value = v;
    }
}

// Attribute propagation in module body
mod annotated_mod {
    #[allow(dead_code)]
    pub fn helper() -> i32 {
        42
    }

    pub fn other() -> i32 {
        0
    }
}

// Semicolons at top level can produce empty_statement
;

// Macro invocation at declaration level
macro_rules! define_const {
    ($name:ident, $val:expr) => {
        const $name: i32 = $val;
    };
}

define_const!(MY_CONST, 99);
