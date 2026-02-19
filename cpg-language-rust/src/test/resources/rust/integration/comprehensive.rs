// === Literal Coverage ===

fn test_char_literals() {
    let a = 'a';
    let newline = '\n';
    let unicode = '\u{1F600}';
}

fn test_float_literals() {
    let pi = 3.14;
    let sci = 1.5e10;
    let neg_float = -2.5;
}

fn test_unit_expression() {
    let u = ();
}

fn test_parenthesized() {
    let x = (1 + 2) * 3;
}

// === Try Operator ===

fn test_try_operator() -> Result<i32, String> {
    let val = Ok(42)?;
    Ok(val)
}

// === Await ===

async fn test_await() -> i32 {
    let future = async { 42 };
    future.await
}

// === Self Parameter Variations ===

struct MyStruct {
    value: i32,
}

impl MyStruct {
    fn by_ref(&self) -> i32 {
        self.value
    }

    fn by_mut_ref(&mut self) {
        self.value = 0;
    }

    fn by_value(self) -> i32 {
        self.value
    }

    fn static_method() -> MyStruct {
        MyStruct { value: 0 }
    }
}

// === Trait with associated type and bounds ===

trait Processor {
    type Output: Clone + Send;

    fn process(&self) -> Self::Output;
    fn default_method(&self) -> i32 {
        42
    }
}

// === Where clause ===

fn complex_where<T, U>(x: T, y: U)
where
    T: Clone + Send,
    U: Into<String>,
{
    let _a = x;
}

// === Type alias ===

type Coordinate = (i32, i32);
type Result2<T> = Result<T, String>;

// === Use declarations ===

use std::collections::HashMap;
use std::io::*;
use std::io as stdio;

// === Const and static ===

const MAX_SIZE: usize = 1024;
static GLOBAL: i32 = 42;
static mut MUTABLE_GLOBAL: i32 = 0;

// === Macro definition ===

macro_rules! my_macro {
    ($x:expr) => {
        $x + 1
    };
}

// === Enum with variants ===

enum Message {
    Quit,
    Move { x: i32, y: i32 },
    Write(String),
    Color(i32, i32, i32),
}

// === Struct expression with spread ===

fn test_struct_spread() {
    let a = MyStruct { value: 1 };
    let b = MyStruct { value: 2, ..a };
}

// === Match with guard ===

fn test_match_guard(x: i32) -> &'static str {
    match x {
        n if n < 0 => "negative",
        0 => "zero",
        n if n > 100 => "large",
        _ => "other",
    }
}

// === Reference expressions ===

fn test_references() {
    let x = 5;
    let r = &x;
    let mr = &mut 42;
}

// === Unsafe block ===

fn test_unsafe() {
    unsafe {
        let x = 42;
    }
}

// === Async block ===

fn test_async_block() {
    let future = async {
        42
    };
}

// === Raw string literals ===

fn test_raw_strings() {
    let raw = r"hello\nworld";
    let raw_hash = r#"hello "world""#;
}

// === Range expressions ===

fn test_ranges() {
    let full = 0..10;
    let inclusive = 0..=10;
    let from = 5..;
    let to = ..5;
}

// === Compound assignments ===

fn test_compound_assignments() {
    let mut x = 10;
    x += 5;
    x -= 3;
    x *= 2;
    x /= 4;
    x %= 3;
    x &= 0xFF;
    x |= 0x0F;
    x ^= 0xAA;
    x <<= 2;
    x >>= 1;
}

// === For loop ===

fn test_for_loop() {
    for i in 0..10 {
        let _ = i;
    }
}

// === While and loop ===

fn test_loops() {
    let mut i = 0;
    while i < 10 {
        i += 1;
    }

    loop {
        break;
    }
}

// === If let ===

fn test_if_let() {
    let opt: Option<i32> = Some(42);
    if let Some(x) = opt {
        let _ = x;
    }
}

// === Closures with types ===

fn test_typed_closure() {
    let add = |a: i32, b: i32| -> i32 { a + b };
    let simple = |x| x + 1;
}

// === Scoped identifiers ===

fn test_scoped_ids() {
    let _x = std::i32::MAX;
}

// === Negative integer literals ===

fn test_negative_ints() {
    let x = -42;
    let y = -0xFF;
}

// === Module ===

mod inner_module {
    pub fn inner_fn() -> i32 {
        42
    }
}
