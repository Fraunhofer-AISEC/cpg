// ============================================================
// SECTION 5: Additional edge cases for coverage
// ============================================================

// --- 5a: closure with typed parameters and block body ---
fn test_closure_typed_block() {
    let add = |a: i32, b: i32| -> i32 {
        a + b
    };
    let _ = add(1, 2);
}

// --- 5b: closure with return type but expression body ---
fn test_closure_return_type() {
    let double = |x: i32| -> i32 { x * 2 };
    let _ = double(5);
}

// --- 5c: parenthesized expression ---
fn test_parenthesized() {
    let x = (1 + 2) * 3;
    let y = ((x));
    let _ = y;
}

// --- 5d: unit expression ---
fn test_unit_expr() {
    let _unit = ();
}

// --- 5e: self reference in method ---
struct Counter {
    count: i32,
}

impl Counter {
    fn new() -> Counter {
        Counter { count: 0 }
    }

    fn increment(&mut self) {
        self.count += 1;
    }

    fn get(&self) -> i32 {
        self.count
    }
}

// --- 5f: if-else with complex condition and nested ifs ---
fn test_nested_if(x: i32, y: i32) -> i32 {
    if x > 0 {
        if y > 0 {
            x + y
        } else {
            x - y
        }
    } else if x == 0 {
        y
    } else {
        -x - y
    }
}

// --- 5g: match with wildcard and literal patterns ---
fn test_match_wildcard() -> &'static str {
    let x = 42;
    match x {
        0 => "zero",
        1 => "one",
        _ => "other",
    }
}

// --- 5h: while let with Option ---
fn test_while_let_option() {
    let mut opt = Some(5);
    while let Some(val) = opt {
        if val == 0 {
            opt = None;
        } else {
            opt = Some(val - 1);
        }
    }
}

// --- 5i: labeled while loop ---
fn test_labeled_while() {
    let mut i = 0;
    'outer: while i < 10 {
        i += 1;
        let mut j = 0;
        while j < 10 {
            j += 1;
            if i * j > 50 {
                break 'outer;
            }
        }
    }
}

// --- 5j: labeled for loop ---
fn test_labeled_for() {
    'outer: for i in 0..10 {
        for j in 0..10 {
            if i + j > 15 {
                break 'outer;
            }
            if j == 3 {
                continue;
            }
        }
    }
}

// --- 5k: raw string literal ---
fn test_raw_strings() {
    let s1 = r"raw string no hashes";
    let s2 = r#"raw string with hashes"#;
    let s3 = r##"raw with double hashes"##;
    let _ = (s1, s2, s3);
}

// --- 5l: byte string and C string ---
fn test_special_strings() {
    let bs = b"byte string";
    let cs = c"c string";
    let _ = (bs, cs);
}

// --- 5m: char literals ---
fn test_char_literals() {
    let a = 'a';
    let b = 'Z';
    let newline = '\n';
    let _ = (a, b, newline);
}

// --- 5n: boolean literals ---
fn test_bool_literals() {
    let t = true;
    let f = false;
    let _ = (t, f);
}

// --- 5o: scoped identifier ---
fn test_scoped_ids() {
    let _ = std::i32::MAX;
    let _ = String::new();
}

// --- 5p: generic function with where clause ---
fn test_where_clause<T>(x: T) -> String
where
    T: std::fmt::Display + Clone,
{
    format!("{}", x)
}

// --- 5q: struct with generic and where clause ---
struct GenericContainer<T>
where
    T: Clone,
{
    data: Vec<T>,
}

// --- 5r: impl with trait for existing struct ---
trait Summary {
    fn summarize(&self) -> String;
}

impl Summary for Counter {
    fn summarize(&self) -> String {
        String::from("counter")
    }
}

// --- 5s: empty match arm value (should produce empty statement) ---
fn test_empty_match_arm(x: i32) {
    match x {
        0 => {},
        1 => {},
        _ => {},
    }
}

// --- 5t: nested tuple patterns in match ---
fn test_nested_tuple_match(pair: (i32, (bool, i32))) -> i32 {
    match pair {
        (0, (true, y)) => y,
        (x, (false, _)) => x,
        (x, (_, y)) => x + y,
    }
}

// --- 5u: or_pattern with single alternative (size == 1 path, line 101-102) ---
fn test_single_or_pattern(x: i32) -> bool {
    match x {
        1 => true,
        _ => false,
    }
}

// --- 5v: method call on struct ---
fn test_method_calls() {
    let mut c = Counter::new();
    c.increment();
    c.increment();
    let val = c.get();
    let _ = val;
}

// --- 5w: impl trait return type ---
fn test_impl_return() -> impl std::fmt::Display {
    42
}

// --- 5x: dynamic trait type ---
fn test_dyn_trait(d: &dyn std::fmt::Display) {
    let _ = d;
}

// --- 5y: never type ---
fn test_never_type() -> ! {
    loop {}
}

// --- 5z: nested function ---
fn test_nested_fn() -> i32 {
    fn helper(a: i32, b: i32) -> i32 {
        a * b
    }
    helper(3, 7)
}

// --- 5aa: pointer types ---
fn test_pointer_types(p: *const i32, q: *mut i32) {
    let _ = (p, q);
}

// --- 5ab: array type in function signature ---
fn test_array_type(arr: [i32; 3]) -> i32 {
    arr[0] + arr[1] + arr[2]
}

// --- 5ac: tuple type in function signature ---
fn test_tuple_type(t: (i32, f64, bool)) -> i32 {
    t.0
}

// --- 5ad: reference type with lifetime in struct ---
struct RefHolder<'a> {
    data: &'a str,
    mutable_data: &'a mut i32,
}

// --- 5ae: const and static with various types ---
const PI: f64 = 3.14159;
static mut COUNTER: i32 = 0;
static NAMES: &str = "test";

// --- 5af: type alias ---
type IntPair = (i32, i32);
type Result2<T> = Result<T, String>;

// --- 5ag: use declarations ---
use std::collections::HashMap;
use std::io::Read;

// --- 5ah: extern crate ---
extern crate std;

// --- 5ai: inner attribute ---
#![allow(unused_variables)]

// --- 5aj: union ---
union IntOrFloat {
    i: i32,
    f: f32,
}

fn test_union() {
    let u = IntOrFloat { i: 42 };
    let _ = unsafe { u.i };
}

// --- 5ak: associated type in trait ---
trait Container2 {
    type Item;
    fn first(&self) -> Option<&Self::Item>;
}

// --- 5al: abstract type (impl Trait) in parameters ---
fn test_abstract_param(x: impl std::fmt::Display) -> String {
    format!("{}", x)
}

// --- 5am: enum with all variant types ---
enum Value {
    Unit,
    Tuple(i32, String),
    Struct { name: String, age: i32 },
}

fn test_enum_patterns(v: Value) -> i32 {
    match v {
        Value::Unit => 0,
        Value::Tuple(n, _) => n,
        Value::Struct { name: _, age } => age,
    }
}

// --- 5an: function type parameter ---
fn test_fn_type_with_return(f: fn(i32) -> i32) -> i32 {
    f(42)
}

// --- 5ao: async function ---
async fn async_helper() -> i32 {
    42
}

async fn test_async_await() -> i32 {
    let result = async_helper().await;
    result
}

// --- 5ap: try expression ---
fn test_try_expr() -> Result<i32, String> {
    let x: Result<i32, String> = Ok(10);
    let val = x?;
    Ok(val * 2)
}

// --- 5aq: index expression ---
fn test_index_expr() -> i32 {
    let arr = [10, 20, 30, 40];
    let first = arr[0];
    let last = arr[3];
    first + last
}

// --- 5ar: range expressions -- all forms ---
fn test_all_ranges() {
    let full = 0..10;
    let inclusive = 0..=9;
    let from = 5..;
    let to = ..10;
    let to_inclusive = ..=9;
    let unbounded = ..;
    let _ = (full, inclusive, from, to, to_inclusive, unbounded);
}

// --- 5as: cast expressions ---
fn test_casts() {
    let x = 42i32;
    let y = x as f64;
    let z = y as i32;
    let _ = (y, z);
}

// --- 5at: unsafe block ---
fn test_unsafe_block() {
    let mut x = 42;
    let p = &mut x as *mut i32;
    unsafe {
        *p = 99;
    }
}

// --- 5au: async block ---
fn test_async_block() {
    let fut = async {
        42
    };
}

// --- 5av: struct expression with all field types ---
struct FullStruct {
    a: i32,
    b: String,
}

fn test_struct_full() {
    let a = 10;
    // field init
    let s1 = FullStruct { a: 1, b: String::from("x") };
    // shorthand
    let s2 = FullStruct { a, b: String::from("y") };
    // spread
    let s3 = FullStruct { a: 99, ..s1 };
    let _ = (s2, s3);
}

// --- 5aw: macro invocation ---
fn test_macros() {
    println!("hello {}", 42);
    let v = vec![1, 2, 3];
    let _ = v;
}

