#![allow(unused_variables)]

fn if_let() {
    let opt = Some(5);
    if let Some(x) = opt {
        let y = x;
    }
}

fn while_let() {
    let mut iter = [1, 2, 3].iter();
    while let Some(x) = iter.next() {
        let y = x;
    }
}

fn test_negative_literal() -> i32 {
    let x = 5;
    match x {
        -1 => -100,
        0 => 0,
        _ => 1,
    }
}

fn test_continue_with_label() {
    'outer: for _i in 0..10 {
        for j in 0..10 {
            if j == 5 {
                continue 'outer;
            }
        }
    }
}

fn test_continue_simple() {
    for i in 0..10 {
        if i == 5 {
            continue;
        }
    }
}

fn test_break_with_value() -> i32 {
    let result = loop {
        break 42;
    };
    result
}

fn test_if_else_chain(x: i32) -> &'static str {
    if x < 0 {
        "negative"
    } else if x == 0 {
        "zero"
    } else {
        "positive"
    }
}

fn test_while_let() {
    let mut stack: Vec<i32> = Vec::new();
    while let Some(top) = stack.pop() {
        let _ = top;
    }
}

fn test_let_mut_pattern() {
    let mut x = 10;
    x += 1;
    let _ = x;
}

fn test_ref_pattern_binding() {
    let val = 42;
    match val {
        ref r => {
            let _ = r;
        }
    }
}

fn test_mut_pattern_binding() {
    let val = 42;
    match val {
        mut m => {
            m += 1;
            let _ = m;
        }
    }
}

fn test_closure_typed_block() {
    let add = |a: i32, b: i32| -> i32 {
        a + b
    };
    let _ = add(1, 2);
}

fn test_closure_return_type() {
    let double = |x: i32| -> i32 { x * 2 };
    let _ = double(5);
}

fn test_parenthesized() {
    let x = (1 + 2) * 3;
    let y = ((x));
    let _ = y;
}

fn test_unit_expr() {
    let _unit = ();
}

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

    fn double(&self) -> i32 {
        self.count * 2
    }

    fn reset(&mut self) {
        self.count = 0;
    }
}

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

fn test_match_wildcard() -> &'static str {
    let x = 42;
    match x {
        0 => "zero",
        1 => "one",
        _ => "other",
    }
}

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

fn test_raw_strings() {
    let s1 = r"raw string no hashes";
    let s2 = r#"raw string with hashes"#;
    let s3 = r##"raw with double hashes"##;
    let _ = (s1, s2, s3);
}

fn test_special_strings() {
    let bs = b"byte string";
    let cs = c"c string";
    let _ = (bs, cs);
}

fn test_char_literals() {
    let a = 'a';
    let b = 'Z';
    let newline = '\n';
    let _ = (a, b, newline);
}

fn test_bool_literals() {
    let t = true;
    let f = false;
    let _ = (t, f);
}

fn test_scoped_ids() {
    let _ = std::i32::MAX;
    let _ = String::new();
}

fn test_where_clause<T>(x: T) -> String
where
    T: std::fmt::Display + Clone,
{
    format!("{}", x)
}

struct GenericContainer<T>
where
    T: Clone,
{
    data: Vec<T>,
}

trait Summary {
    fn summarize(&self) -> String;
}

impl Summary for Counter {
    fn summarize(&self) -> String {
        String::from("counter")
    }
}

fn test_empty_match_arm(x: i32) {
    match x {
        0 => {},
        1 => {},
        _ => {},
    }
}

fn test_nested_tuple_match(pair: (i32, (bool, i32))) -> i32 {
    match pair {
        (0, (true, y)) => y,
        (x, (false, _)) => x,
        (x, (_, y)) => x + y,
    }
}

fn test_single_or_pattern(x: i32) -> bool {
    match x {
        1 => true,
        _ => false,
    }
}

fn test_method_calls() {
    let mut c = Counter::new();
    c.increment();
    c.increment();
    let val = c.get();
    let _ = val;
}

fn test_impl_return() -> impl std::fmt::Display {
    42
}

fn test_dyn_trait(d: &dyn std::fmt::Display) {
    let _ = d;
}

fn test_never_type() -> ! {
    loop {}
}

fn test_nested_fn() -> i32 {
    fn helper(a: i32, b: i32) -> i32 {
        a * b
    }
    helper(3, 7)
}

fn test_pointer_types(p: *const i32, q: *mut i32) {
    let _ = (p, q);
}

fn test_array_type(arr: [i32; 3]) -> i32 {
    arr[0] + arr[1] + arr[2]
}

fn test_tuple_type(t: (i32, f64, bool)) -> i32 {
    t.0
}

struct RefHolder<'a> {
    data: &'a str,
    mutable_data: &'a mut i32,
}

const PI: f64 = 3.14159;
static mut COUNTER: i32 = 0;
static NAMES: &str = "test";

type IntPair = (i32, i32);
type Result2<T> = Result<T, String>;

use std::collections::HashMap;
use std::io::Read;

extern crate std;

union IntOrFloat {
    i: i32,
    f: f32,
}

fn test_union() {
    let u = IntOrFloat { i: 42 };
    let _ = unsafe { u.i };
}

trait Container2 {
    type Item;
    fn first(&self) -> Option<&Self::Item>;
}

fn test_abstract_param(x: impl std::fmt::Display) -> String {
    format!("{}", x)
}

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

fn test_fn_type_with_return(f: fn(i32) -> i32) -> i32 {
    f(42)
}

async fn async_helper() -> i32 {
    42
}

async fn test_async_await() -> i32 {
    let result = async_helper().await;
    result
}

fn test_try_expr() -> Result<i32, String> {
    let x: Result<i32, String> = Ok(10);
    let val = x?;
    Ok(val * 2)
}

fn test_index_expr() -> i32 {
    let arr = [10, 20, 30, 40];
    let first = arr[0];
    let last = arr[3];
    first + last
}

fn test_all_ranges() {
    let full = 0..10;
    let inclusive = 0..=9;
    let from = 5..;
    let to = ..10;
    let to_inclusive = ..=9;
    let unbounded = ..;
    let _ = (full, inclusive, from, to, to_inclusive, unbounded);
}

fn test_casts() {
    let x = 42i32;
    let y = x as f64;
    let z = y as i32;
    let _ = (y, z);
}

fn test_unsafe_block() {
    let mut x = 42;
    let p = &mut x as *mut i32;
    unsafe {
        *p = 99;
    }
}

fn test_async_block() {
    let _fut = async { 42 };
}

struct FullStruct {
    a: i32,
    b: String,
}

fn test_struct_full() {
    let a = 10;
    let s1 = FullStruct { a: 1, b: String::from("x") };
    let s2 = FullStruct { a, b: String::from("y") };
    let s3 = FullStruct { a: 99, ..s1 };
    let _ = (s2, s3);
}

fn test_macros() {
    println!("hello {}", 42);
    let v = vec![1, 2, 3];
    let _ = v;
}

macro_rules! define_const {
    ($name:ident, $val:expr) => {
        const $name: i32 = $val;
    };
}

define_const!(ANOTHER_CONST, 77);

fn test_closure_return_expr() -> i32 {
    let f = |x: i32| -> i32 { return x };
    f(42)
}

fn test_let_while_value() {
    let _r = while false {};
}

fn test_let_for_value() {
    let _r = for _i in 0..0 {};
}

fn test_closure_with_return_body() -> i32 {
    let f = |x: i32| return x;
    f(42)
}

fn test_match_with_guard(x: i32) -> &'static str {
    match x {
        n if n < 0 => "negative",
        0 => "zero",
        n if n > 100 => "large",
        _ => "normal",
    }
}

fn test_single_alternative_pattern(x: i32) -> bool {
    match x {
        1 | 2 | 3 => true,
        _ => false,
    }
}

fn test_negative_int_literal() -> i32 {
    let a = -42;
    let b = -1;
    let c = -100;
    a + b + c
}

fn test_float_exponent() -> f64 {
    let a = 1.0e10;
    let b = 2.5E-3;
    let c = 3.14;
    a + b + c
}

trait MyTrait {
    type Output;
    fn produce(&self) -> Self::Output;
}

struct MyImpl;

impl MyTrait for MyImpl {
    type Output = i32;
    fn produce(&self) -> i32 {
        42
    }
}

fn test_qualified_path() -> i32 {
    let m = MyImpl;
    <MyImpl as MyTrait>::produce(&m)
}

fn test_turbofish_annotation() {
    let _v: Vec<i32> = Vec::<i32>::new();
    let _s = String::new();
}

fn test_loop_with_break_value() -> i32 {
    let result = loop {
        break 42;
    };
    result
}

fn test_labeled_loop_break() -> i32 {
    let result = 'calc: loop {
        let x = 10;
        if x > 5 {
            break 'calc x * 2;
        }
    };
    result
}

fn test_if_let() -> i32 {
    let opt: Option<i32> = Some(42);
    if let Some(val) = opt {
        val
    } else {
        0
    }
}

fn test_if_let_chain() -> i32 {
    let a: Option<i32> = Some(10);
    let b: Option<i32> = Some(20);
    if let Some(x) = a {
        if let Some(y) = b {
            x + y
        } else {
            x
        }
    } else {
        0
    }
}

struct Point3D {
    x: i32,
    y: i32,
    z: i32,
}

fn test_struct_pattern_match(p: Point3D) -> i32 {
    match p {
        Point3D { x, y: 0, z } => x + z,
        Point3D { x: 0, y, .. } => y,
        Point3D { x, y, z } => x + y + z,
    }
}

fn test_slice_pattern(arr: &[i32]) -> i32 {
    match arr {
        [] => 0,
        [x] => *x,
        [first, .., last] => *first + *last,
    }
}

fn test_field_pattern_binding() -> i32 {
    let p = Point3D { x: 1, y: 2, z: 3 };
    match p {
        Point3D { x: a, y: b, z: c } => a + b + c,
    }
}

fn test_self_method_calls() {
    let mut c = Counter::new();
    c.increment();
    let doubled = c.double();
    c.reset();
    let _ = doubled;
}

fn test_ref_and_mut_ref() {
    let mut x = 42;
    let r = &x;
    let mr = &mut x;
    *mr = 100;
    let _ = r;
}

fn test_constrained_generic<T: Clone + std::fmt::Debug>(x: T) -> T {
    x.clone()
}

pub struct PubFields {
    pub visible: i32,
    hidden: i32,
}

fn test_enum_construction() {
    let _unit = Value::Unit;
    let _tuple = Value::Tuple(42, String::from("hello"));
    let _strct = Value::Struct { name: String::from("test"), age: 25 };
}

fn test_tuple_destructure() {
    let (a, b, c) = (1, 2, 3);
    let _ = a + b + c;
}

fn test_match_empty_arms(x: i32) {
    match x {
        0 => (),
        1 => (),
        _ => (),
    }
}

fn test_let_with_mut_destructure() {
    let opt: Option<i32> = Some(42);
    if let Some(mut val) = opt {
        val += 1;
        let _ = val;
    }
}

fn test_fn_with_mut_param(mut count: i32) -> i32 {
    count += 1;
    count
}

fn test_expression_statement_variants() {
    let mut x = 0;
    x = 5;
    x += 3;
    drop(x);
}

fn test_complex_enum_match(v: Value) -> String {
    match v {
        Value::Unit => String::from("unit"),
        Value::Tuple(n, ref s) => {
            let _ = n;
            s.clone()
        }
        Value::Struct { ref name, age } => {
            let _ = age;
            name.clone()
        }
    }
}

fn test_let_without_type() -> i32 {
    let x;
    x = 42;
    let y = 10;
    x + y
}

fn test_let_typed_no_value() -> i32 {
    let x: i32;
    x = 99;
    x
}

fn test_or_pattern_complex(x: i32) -> bool {
    match x {
        1 | 2 | 3 => true,
        4 | 5 => true,
        _ => false,
    }
}

fn test_nested_match(x: i32) -> i32 {
    match x {
        0 => 0,
        _ => match x % 2 {
            0 => x * 2,
            _ => x + 1,
        },
    }
}

fn test_block_as_value() -> i32 {
    let v = {
        let a = 1;
        let b = 2;
        a + b
    };
    v
}

fn test_fn_returning_closure() -> impl Fn(i32) -> i32 {
    |x| x + 1
}

fn test_async_expr() {
    let _fut = async { 1 };
}

fn test_multiple_returns(x: i32) -> i32 {
    if x < 0 {
        return -1;
    }
    if x == 0 {
        return 0;
    }
    1
}
