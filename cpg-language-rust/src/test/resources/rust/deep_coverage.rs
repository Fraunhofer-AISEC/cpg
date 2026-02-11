// Deep coverage test fixture -- exercises uncovered branches across all handlers

// ============ Pattern matching with various pattern types ============

struct Point { x: i32, y: i32 }
enum Shape {
    Circle(f64),
    Rect(f64, f64),
    Named { width: f64, height: f64 },
}

fn test_match_struct_pattern(p: Point) -> i32 {
    // struct_pattern in extractBindings
    match p {
        Point { x, y } => x + y,
    }
}

fn test_match_struct_rename(p: Point) -> i32 {
    // field_pattern with pattern child: Point { x: a, y: b }
    match p {
        Point { x: a, y: b } => a + b,
    }
}

fn test_match_tuple_struct(s: Shape) -> f64 {
    // tuple_struct_pattern in extractBindings
    match s {
        Shape::Circle(r) => r,
        Shape::Rect(w, h) => w * h,
        Shape::Named { width, height } => width * height,
    }
}

fn test_match_or_pattern(x: i32) -> bool {
    // or_pattern in extractBindings + ExpressionHandler
    match x {
        1 | 2 | 3 => true,
        _ => false,
    }
}

fn test_match_ref_pattern(x: &i32) {
    // ref_pattern in extractBindings
    match x {
        &0 => {},
        val => {
            let _ = val;
        },
    }
}

fn test_match_guard(x: i32) -> &'static str {
    // match arm with if_clause guard
    match x {
        n if n < 0 => "negative",
        0 => "zero",
        n if n > 100 => "big",
        _ => "small",
    }
}

fn test_match_nested_tuple(pair: (i32, i32)) -> i32 {
    // tuple_pattern inside match
    match pair {
        (0, y) => y,
        (x, 0) => x,
        (x, y) => x + y,
    }
}

fn test_match_slice(data: &[i32]) -> i32 {
    // slice_pattern in extractBindings
    match data {
        [] => 0,
        [single] => *single,
        [first, .., last] => *first + *last,
    }
}

fn test_match_negative_literal(x: i32) -> &'static str {
    match x {
        -1 => "minus one",
        -100 => "minus hundred",
        0 => "zero",
        _ => "other",
    }
}

// ============ Let declarations -- various forms ============

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

// ============ If expressions -- all branches ============

fn test_if_let(opt: Option<i32>) -> i32 {
    if let Some(x) = opt {
        x * 2
    } else {
        0
    }
}

fn test_if_let_no_else(opt: Option<i32>) {
    if let Some(x) = opt {
        let _ = x;
    }
}

fn test_if_else_if(x: i32) -> &'static str {
    if x < 0 {
        "negative"
    } else if x == 0 {
        "zero"
    } else if x < 10 {
        "small"
    } else {
        "large"
    }
}

fn test_if_simple(x: bool) -> i32 {
    if x {
        1
    } else {
        0
    }
}

// ============ Assignments and compound assignments ============

fn test_assignments() {
    let mut x = 0;
    x = 42;
    let mut arr = [1, 2, 3];
    arr[0] = 10;
    let _ = (x, arr);
}

fn test_compound_assignments() {
    let mut x: i32 = 100;
    x += 1;
    x -= 2;
    x *= 3;
    x /= 4;
    x %= 5;
    x &= 0xFF;
    x |= 0x01;
    x ^= 0x10;
    x <<= 1;
    x >>= 1;
    let _ = x;
}

// ============ Struct expressions -- all field init types ============

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

// ============ Type system -- reference types with mut/lifetime ============

fn test_mutable_ref(x: &mut i32) {
    *x += 1;
}

fn test_lifetime_ref<'a>(x: &'a str) -> &'a str {
    x
}

fn test_double_ref(x: &&i32) -> i32 {
    **x
}

// ============ Traits with methods and associated types ============

trait Drawable {
    type Output;
    fn draw(&self) -> Self::Output;
    fn name(&self) -> &str;
}

trait Resizable: Drawable {
    fn resize(&mut self, factor: f64);
}

struct Circle {
    radius: f64,
}

impl Drawable for Circle {
    type Output = String;
    fn draw(&self) -> String {
        String::from("circle")
    }
    fn name(&self) -> &str {
        "circle"
    }
}

impl Resizable for Circle {
    fn resize(&mut self, factor: f64) {
        self.radius *= factor;
    }
}

// ============ Complex structs with generics and visibility ============

pub struct Container<T> {
    pub data: Vec<T>,
    count: usize,
}

impl<T> Container<T> {
    pub fn new() -> Self {
        Container { data: Vec::new(), count: 0 }
    }

    pub fn add(&mut self, item: T) {
        self.data.push(item);
        self.count += 1;
    }

    fn len(&self) -> usize {
        self.count
    }
}

// ============ Impl with generics and trait bounds ============

fn test_generic_with_bounds<T: Clone + Send>(x: T) -> T {
    x.clone()
}

fn test_multi_generic<A, B>(a: A, b: B) -> (A, B) {
    (a, b)
}

// ============ While/for/loop with labels ============

fn test_labeled_loops() {
    'outer: for i in 0..10 {
        'inner: for j in 0..10 {
            if i + j > 15 {
                break 'outer;
            }
            if j == 5 {
                continue 'inner;
            }
        }
    }
}

fn test_loop_with_break() -> i32 {
    let mut count = 0;
    loop {
        count += 1;
        if count >= 10 {
            break;
        }
    }
    count
}

fn test_while_condition() {
    let mut x = 0;
    while x < 10 {
        x += 1;
    }
}

// ============ Closures ============

fn test_closure_as_arg() {
    let v = vec![1, 2, 3, 4, 5];
    let evens: Vec<&i32> = v.iter().filter(|x| **x % 2 == 0).collect();
    let sum: i32 = v.iter().map(|x| x * 2).sum();
    let _ = (evens, sum);
}

fn test_closure_with_move() {
    let s = String::from("hello");
    let greeting = move || s.len();
    let _ = greeting();
}

// ============ Type cast ============

fn test_type_cast() {
    let x: i32 = 42;
    let y = x as f64;
    let z = y as i32;
    let p = &x as *const i32;
    let _ = (y, z, p);
}

// ============ Unsafe block ============

fn test_unsafe() {
    let mut x = 42;
    let r = &mut x as *mut i32;
    unsafe {
        *r = 100;
    }
}

// ============ Async block ============

fn test_async() {
    let fut = async {
        42
    };
    let fut2 = async move {
        100
    };
}

// ============ Tuple index expressions ============

fn test_tuple_index_simple() {
    let t = (1, "hello", 3.14);
    let a = t.0;
    let b = t.1;
    let c = t.2;
    let _ = (a, b, c);
}

fn test_tuple_index_nested() {
    let t = ((1, 2), (3, 4));
    let inner = t.0;
    let val = inner.1;
    let _ = val;
}

// ============ Field expression ============

fn test_field_access() {
    let p = Point { x: 10, y: 20 };
    let a = p.x;
    let b = p.y;
    let _ = a + b;
}

// ============ Range expressions -- all forms ============

fn test_ranges() {
    let full = 0..10;
    let inclusive = 0..=9;
    let from = 5..;
    let to = ..10;
    let to_inclusive = ..=9;
    let full_range = ..;
    let _ = (full, inclusive, from, to, to_inclusive, full_range);
}

// ============ Macro invocations at decl level ============

macro_rules! make_fn {
    ($name:ident) => {
        fn $name() -> i32 { 0 }
    };
}

make_fn!(generated_func);

// ============ Complex function parameters ============

fn test_many_params(a: i32, b: f64, c: &str, d: bool) -> i32 {
    if d { a } else { b as i32 }
}

fn test_pattern_param((x, y): (i32, i32)) -> i32 {
    x + y
}

// ============ Float literals with suffixes ============

fn test_float_literals() {
    let a = 3.14;
    let b = 2.0f32;
    let c = 1.0f64;
    let d = 42.0;
    let _ = (a, b, c, d);
}

// ============ Integer literals -- more variants ============

fn test_more_integers() {
    let hex = 0xFF;
    let oct = 0o77;
    let bin = 0b1010;
    let with_suffix_i8: i8 = 42i8;
    let with_suffix_u32 = 100u32;
    let with_suffix_i64 = 999i64;
    let with_underscores = 1_000_000;
    let _ = (hex, oct, bin, with_suffix_i8, with_suffix_u32, with_suffix_i64, with_underscores);
}

// ============ Await expression ============

async fn fetch_data() -> i32 {
    42
}

async fn test_await() -> i32 {
    let result = fetch_data().await;
    result
}

// ============ Try expression ============

fn test_try_operator() -> Result<i32, String> {
    let x: Result<i32, String> = Ok(42);
    let val = x?;
    Ok(val + 1)
}

// ============ Unary expressions ============

fn test_unary_ops() {
    let x = 5;
    let neg = -x;
    let not = !true;
    let deref_ptr: &i32 = &x;
    let deref_val = *deref_ptr;
    let _ = (neg, not, deref_val);
}

// ============ Scoped identifiers ============

fn test_scoped_ids() {
    let _ = std::i32::MAX;
    let _ = String::new();
    let _ = Vec::<i32>::new();
}

// ============ Module with content ============

mod inner {
    pub fn helper() -> i32 {
        42
    }

    pub struct InnerStruct {
        pub value: i32,
    }
}

fn test_module_access() {
    let x = inner::helper();
    let s = inner::InnerStruct { value: x };
    let _ = s.value;
}

// ============ Extern block with functions ============

extern "C" {
    fn abs(input: i32) -> i32;
    fn strlen(s: *const u8) -> usize;
}

// ============ Use declarations ============

use std::collections::HashMap;

fn test_use() {
    let _map: HashMap<String, i32> = HashMap::new();
}

// ============ Const and static ============

const MAX_SIZE: usize = 1024;
static GREETING: &str = "hello";

fn test_const_static() {
    let _ = MAX_SIZE;
    let _ = GREETING;
}

// ============ Type alias ============

type Pair = (i32, i32);

fn test_type_alias() -> Pair {
    (1, 2)
}

// ============ Where clause ============

fn test_where_clause<T, U>(t: T, u: U) -> String
where
    T: std::fmt::Display,
    U: std::fmt::Debug,
{
    format!("{}", t)
}

// ============ Impl trait in return position ============

fn test_impl_trait() -> impl std::fmt::Display {
    42
}

// ============ Enum with all variant types ============

enum Message {
    Quit,
    Move { x: i32, y: i32 },
    Write(String),
    ChangeColor(i32, i32, i32),
}

fn test_enum_match(msg: Message) -> i32 {
    match msg {
        Message::Quit => 0,
        Message::Move { x, y } => x + y,
        Message::Write(text) => text.len() as i32,
        Message::ChangeColor(r, g, b) => r + g + b,
    }
}

// ============ Pointer types in function signatures ============

fn test_raw_pointers(p: *const i32, q: *mut i32) -> bool {
    p.is_null() || q.is_null()
}

// ============ Dynamic type (dyn Trait) ============

fn test_dyn_trait(d: &dyn std::fmt::Display) -> String {
    format!("{}", d)
}

// ============ Never type ============

fn test_never() -> ! {
    loop {}
}

// ============ Nested function ============

fn test_nested_fn() -> i32 {
    fn inner_add(a: i32, b: i32) -> i32 {
        a + b
    }
    inner_add(3, 4)
}
