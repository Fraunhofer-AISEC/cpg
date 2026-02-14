// Branch coverage test fixture -- targets specifically uncovered branches
// across ExpressionHandler, StatementHandler, DeclarationHandler, TypeHandler

// ============================================================
// SECTION 1: ExpressionHandler uncovered branches
// ============================================================

// --- 1a: return_expression dispatched from ExpressionHandler (line 51) ---
// When return appears as an expression (not in expression_statement)
fn expr_return_in_closure() -> i32 {
    let f = |x: i32| -> i32 { return x + 1 };
    f(5)
}

// --- 1b: while_expression dispatched from ExpressionHandler (line 52) ---
// while used as trailing expression value in a block
fn expr_while_in_block() {
    let _result = {
        let mut i = 0;
        while i < 3 {
            i += 1;
        }
    };
}

// --- 1c: for_expression dispatched from ExpressionHandler (line 54) ---
// for used as trailing expression value in a block
fn expr_for_in_block() {
    let _result = {
        let mut sum = 0;
        for i in 0..5 {
            sum += i;
        }
    };
}

// --- 1d: tuple_index_expression (line 81, currently 0% covered) ---
fn test_tuple_index_expr() -> i32 {
    let t = (10, 20, 30);
    let a = t.0;
    let b = t.1;
    let c = t.2;
    a + b + c
}

fn test_tuple_index_nested() -> i32 {
    let t = ((1, 2), (3, 4));
    let first = t.0;
    let val = first.1;
    val
}

// --- 1e: negative float literal (line 801-804, handleNegativeLiteral float branch) ---
fn test_negative_float_literal() -> f64 {
    let a = -3.14;
    let b = -0.5;
    let c = -100.0;
    a + b + c
}

// --- 1f: integer literal suffixes not yet tested ---
fn test_integer_suffixes_extended() {
    let a = 42u16;
    let b = 42u64;
    let c = 42u128;
    let d = 42usize;
    let e = 42i16;
    let f = 42i128;
    let g = 42isize;
    let _ = (a, b, c, d, e, f, g);
}

// --- 1g: 0X uppercase hex, 0O uppercase octal, 0B uppercase binary ---
fn test_uppercase_literal_prefixes() {
    let hex_upper = 0xFF;
    let oct = 0o77;
    let bin = 0b1010;
    let _ = (hex_upper, oct, bin);
}

// ============================================================
// SECTION 2: StatementHandler uncovered branches
// ============================================================

// --- 2a: let with mut_pattern (handleLetDeclaration line 126-137) ---
fn test_let_mut_pattern() {
    let mut x = 10;
    x += 1;
    let _ = x;
}

// --- 2b: ref_pattern in extractBindings (line 588-590) ---
fn test_ref_pattern_binding() {
    let val = 42;
    match val {
        ref r => {
            let _ = r;
        }
    }
}

// --- 2c: mut_pattern in extractBindings (line 594-595) ---
fn test_mut_pattern_binding() {
    let val = 42;
    match val {
        mut m => {
            m += 1;
            let _ = m;
        }
    }
}

// --- 2d: while let (while_expression with let_condition) ---
fn test_while_let() {
    let mut stack: Vec<i32> = vec![1, 2, 3];
    while let Some(top) = stack.pop() {
        let _ = top;
    }
}

// ============================================================
// SECTION 3: DeclarationHandler uncovered branches
// ============================================================

// --- 3a: tuple struct (ordered_field_declaration_list, line 353-373) ---
struct Pair(i32, i32);
struct Triple(f64, f64, f64);
struct Wrapper(String);

fn test_tuple_structs() {
    let p = Pair(1, 2);
    let t = Triple(1.0, 2.0, 3.0);
    let w = Wrapper(String::from("hello"));
}

// --- 3b: trait with type parameters (line 394-400) ---
trait Converter<T, U> {
    fn convert(&self, input: T) -> U;
}

// --- 3c: mut parameter pattern (line 251-257) ---
fn test_mut_param(mut x: i32) -> i32 {
    x += 10;
    x
}

// --- 3d: attribute propagation in trait body (line 414-415) ---
trait Annotated {
    #[must_use]
    fn compute(&self) -> i32;
    fn describe(&self) -> &str;
}

// --- 3e: attribute propagation in impl body (line 539-540) ---
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

// --- 3f: attribute propagation in module body (line 571-572) ---
mod annotated_mod {
    #[allow(dead_code)]
    pub fn helper() -> i32 { 42 }

    pub fn other() -> i32 { 0 }
}

// --- 3g: extern "C" block with string literal ABI (line 735-736) ---
extern "C" {
    fn c_abs(x: i32) -> i32;
    fn c_strlen(s: *const u8) -> usize;
}

// --- 3h: empty_statement in handleNode (line 63) ---
// Semicolons at top level can produce empty_statement
;

// --- 3i: macro_invocation at declaration level (line 64) ---
// (macro_invocation as a top-level item)
// Already partially covered by make_fn! macro, but let's add another
macro_rules! define_const {
    ($name:ident, $val:expr) => {
        const $name: i32 = $val;
    };
}

define_const!(MY_CONST, 99);

// ============================================================
// SECTION 4: TypeHandler uncovered branches
// ============================================================

// --- 4a: bounded_type (line 59-61) ---
fn test_bounded_type(x: &(dyn std::fmt::Display + Send)) {
    let _ = x;
}

// --- 4b: qualified_type (line 63) - <Type as Trait>::Item ---
// Difficult to trigger directly, use a workaround
fn test_qualified_type_proxy() {
    // Use a fully qualified path expression instead
    let _v: Vec<i32> = Vec::new();
}

// --- 4c: generic_type_with_turbofish (line 57) ---
fn test_turbofish_type() {
    let v = Vec::<i32>::new();
    let _ = v;
}

// --- 4d: function type with no return (FunctionType without return_type, line 144) ---
fn test_fn_type_no_return(f: fn(i32, i32)) {
    f(1, 2);
}

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

// ============================================================
// SECTION 6: Targeted constructs for remaining uncovered branches
// ============================================================

// --- 6a: Expressions dispatched through ExpressionHandler ---
// These need to appear as expression children (not statement children)
// to be dispatched via expressionHandler.handle() rather than statementHandler.handle()

// Closure with non-block body that is a return expression
fn test_closure_return_expr() -> i32 {
    let f = |x: i32| -> i32 { return x };
    f(42)
}

// Direct let = while expression (value is while_expression node)
fn test_let_while_value() {
    let _r = while false {};
}

// Direct let = for expression (value is for_expression node)
fn test_let_for_value() {
    let _r = for _i in 0..0 {};
}

// Closure whose body is a direct return_expression (not in a block)
// |x| return x -- tree-sitter should parse body as return_expression
fn test_closure_with_return_body() -> i32 {
    let f = |x: i32| return x;
    f(42)
}

// --- 6b: match guard using "if" inside match arm ---
// This should trigger either match_guard or the embedded "if" guard path
fn test_match_with_guard(x: i32) -> &'static str {
    match x {
        n if n < 0 => "negative",
        0 => "zero",
        n if n > 100 => "large",
        _ => "normal",
    }
}

// --- 6c: or_pattern with single alternative (L101-102) ---
fn test_single_alternative_pattern(x: i32) -> bool {
    match x {
        1 | 2 | 3 => true,
        _ => false,
    }
}

// --- 6d: Macro invocation at top-level declaration level (DeclarationHandler L64) ---
// The define_const! macro invocation above already targets this, but let's add another
define_const!(ANOTHER_CONST, 77);

// --- 6e: negative integer literal (not float) ---
fn test_negative_int_literal() -> i32 {
    let a = -42;
    let b = -1;
    let c = -100;
    a + b + c
}

// --- 6f: float literal with exponent ---
fn test_float_exponent() -> f64 {
    let a = 1.0e10;
    let b = 2.5E-3;
    let c = 3.14;
    a + b + c
}

// --- 6g: qualified type - <Type as Trait>::Method ---
trait MyTrait {
    type Output;
    fn produce(&self) -> Self::Output;
}

struct MyImpl;
impl MyTrait for MyImpl {
    type Output = i32;
    fn produce(&self) -> i32 { 42 }
}

fn test_qualified_path() -> i32 {
    let m = MyImpl;
    <MyImpl as MyTrait>::produce(&m)
}

// --- 6h: generic_type_with_turbofish used as a type annotation ---
fn test_turbofish_annotation() {
    let _v: Vec<i32> = Vec::<i32>::new();
    let _s = String::new();
}

// --- 6i: loop with break value ---
fn test_loop_with_break_value() -> i32 {
    let result = loop {
        break 42;
    };
    result
}

// --- 6j: labeled loop with break value ---
fn test_labeled_loop_break() -> i32 {
    let result = 'calc: loop {
        let x = 10;
        if x > 5 {
            break 'calc x * 2;
        }
    };
    result
}

// --- 6k: if let expression ---
fn test_if_let() -> i32 {
    let opt: Option<i32> = Some(42);
    if let Some(val) = opt {
        val
    } else {
        0
    }
}

// --- 6l: if let chain ---
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

// --- 6m: struct pattern in match ---
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

// --- 6n: slice pattern in match ---
fn test_slice_pattern(arr: &[i32]) -> i32 {
    match arr {
        [] => 0,
        [x] => *x,
        [first, .., last] => *first + *last,
    }
}

// --- 6o: field_pattern with binding ---
fn test_field_pattern_binding() -> i32 {
    let p = Point3D { x: 1, y: 2, z: 3 };
    match p {
        Point3D { x: a, y: b, z: c } => a + b + c,
    }
}

// --- 6p: continue with label ---
fn test_continue_with_label() {
    'outer: for i in 0..10 {
        for j in 0..10 {
            if j == 5 {
                continue 'outer;
            }
        }
    }
}

// --- 6q: method call with self ---
impl Counter {
    fn double(&self) -> i32 {
        self.count * 2
    }

    fn reset(&mut self) {
        self.count = 0;
    }
}

fn test_self_method_calls() {
    let mut c = Counter::new();
    c.increment();
    let doubled = c.double();
    c.reset();
    let _ = doubled;
}

// --- 6r: reference expression with mutable ---
fn test_ref_and_mut_ref() {
    let mut x = 42;
    let r = &x;
    let mr = &mut x;
    *mr = 100;
    let _ = r;
}

// --- 6s: constrained type parameter ---
fn test_constrained_generic<T: Clone + std::fmt::Debug>(x: T) -> T {
    x.clone()
}

// --- 6t: visibility modifiers on struct fields ---
pub struct PubFields {
    pub visible: i32,
    hidden: i32,
}

// --- 6u: enum with all variant types exercised ---
fn test_enum_construction() {
    let _unit = Value::Unit;
    let _tuple = Value::Tuple(42, String::from("hello"));
    let _strct = Value::Struct { name: String::from("test"), age: 25 };
}

// --- 6v: tuple destructuring in let ---
fn test_tuple_destructure() {
    let (a, b, c) = (1, 2, 3);
    let _ = a + b + c;
}

// --- 6w: Empty match arm (no value) ---
fn test_match_empty_arms(x: i32) {
    match x {
        0 => (),
        1 => (),
        _ => (),
    }
}

// --- 6y: mut_pattern in let declaration (StatementHandler L126-137) ---
// 'let mut_pattern = value' uses mut_pattern when the pattern is more complex
fn test_let_with_mut_destructure() {
    let opt: Option<i32> = Some(42);
    // This should produce a let with tuple_struct_pattern containing mut_pattern
    if let Some(mut val) = opt {
        val += 1;
        let _ = val;
    }
}

// --- 6z: mut parameter (DeclarationHandler L251-263) ---
fn test_fn_with_mut_param(mut count: i32) -> i32 {
    count += 1;
    count
}

// --- 6aa: Construct that tests handleExpressionStatement else branch ---
// expression_statement with something that isn't if/block/return/while/loop/for
fn test_expression_statement_variants() {
    let mut x = 0;
    // Assignment expression_statement
    x = 5;
    // Compound assignment expression_statement
    x += 3;
    // Call expression_statement
    drop(x);
}

// --- 6x: Complex enum pattern matching with struct and tuple variants ---
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

// --- 6bb: let without type annotation (covers StatementHandler L167 false branch) ---
fn test_let_without_type() -> i32 {
    let x;
    x = 42;
    let y = 10;
    x + y
}

// --- 6cc: let with type but no value (covers L173 true path with type present) ---
fn test_let_typed_no_value() -> i32 {
    let x: i32;
    x = 99;
    x
}

// --- 6dd: empty statement in function body ---
fn test_empty_statements() {
    ;
    ;
    let _ = 1;
}

// --- 6ee: closure with simple identifier params (no type annotations) ---
fn test_closure_simple_params() -> i32 {
    let add = |a, b| a + b;
    let result: i32 = add(3, 4);
    result
}

// --- 6ff: match with complex or_pattern (multiple alternatives) ---
fn test_or_pattern_complex() -> &'static str {
    let x = 5;
    match x {
        1 | 2 | 3 | 4 => "small",
        5 | 6 | 7 | 8 => "medium",
        _ => "large",
    }
}

// --- 6gg: nested match expressions ---
fn test_nested_match() -> i32 {
    let x = Some(42);
    let y = Some("hello");
    match x {
        Some(n) => match y {
            Some(_) => n,
            None => 0,
        },
        None => -1,
    }
}

// --- 6hh: function taking and returning closures ---
fn test_fn_returning_closure() -> Box<dyn Fn(i32) -> i32> {
    Box::new(|x| x * 2)
}

// --- 6ii: multiple return paths ---
fn test_multiple_returns(x: i32) -> i32 {
    if x < 0 {
        return -x;
    }
    if x == 0 {
        return 1;
    }
    x * x
}

// --- 6jj: expression blocks as values ---
fn test_block_as_value() -> i32 {
    let x = {
        let a = 10;
        let b = 20;
        a + b
    };
    let y = {
        42
    };
    x + y
}

// --- 6kk: Generic struct with impl ---
struct Wrapper2<T> {
    inner: T,
}

impl<T: Clone> Wrapper2<T> {
    fn new(val: T) -> Self {
        Wrapper2 { inner: val }
    }

    fn get(&self) -> T {
        self.inner.clone()
    }
}

fn test_generic_impl() -> i32 {
    let w = Wrapper2::new(42);
    w.get()
}

// --- 6ll: trait with default method implementation ---
trait Greetable {
    fn name(&self) -> &str;
    fn greet(&self) -> String {
        let mut s = String::from("Hello, ");
        s.push_str(self.name());
        s
    }
}

struct Person {
    name_str: String,
}

impl Greetable for Person {
    fn name(&self) -> &str {
        &self.name_str
    }
}

fn test_trait_default_method() -> String {
    let p = Person { name_str: String::from("Rust") };
    p.greet()
}

// --- 6mm: associated type bounds in trait ---
trait Iterable {
    type Item: Clone + std::fmt::Debug;
    fn first_item(&self) -> Option<Self::Item>;
}

// --- 6nn: Multiple use patterns ---
use std::collections::BTreeMap;

// --- 6oo: let with underscore pattern ---
fn test_let_underscore() {
    let _ = compute_something();
    let _unused = 42;
}

fn compute_something() -> i32 {
    100
}

// --- 6pp: async fn with await in expression position ---
async fn test_async_expr() -> i32 {
    let x = async_helper().await + 1;
    x
}

// --- 6qq: Complex generic function ---
fn test_complex_generic<T, U>(a: T, _b: U) -> T
where
    T: Clone,
    U: std::fmt::Debug,
{
    a.clone()
}

// --- 6rr: Trait object in Box ---
fn test_trait_object() -> Box<dyn std::fmt::Display> {
    let x: Box<dyn std::fmt::Display> = Box::new(42);
    x
}

// --- 6ss: Scoped type identifier in type position ---
fn test_scoped_type() -> std::collections::HashMap<String, i32> {
    let mut m = std::collections::HashMap::new();
    m.insert(String::from("key"), 42);
    m
}

// ============================================================
// SECTION 7: Final coverage push - reachable but uncovered paths
// ============================================================

// --- 7a: Block-style macro invocation at top level (DeclarationHandler L64)
// Without semicolon, tree-sitter produces macro_invocation directly (not expression_statement)
thread_local! {
    static THREAD_COUNTER: std::cell::Cell<i32> = std::cell::Cell::new(0);
}

// --- 7b: Tuple let without initializer (StatementHandler L224-227)
fn test_tuple_let_no_init() {
    let (a, b): (i32, i32);
    a = 1;
    b = 2;
    let _ = a + b;
}

// --- 7c: Trait with const item (DeclarationHandler L426 else->null in trait body)
trait TraitWithConst {
    const MAX_VALUE: i32;
    fn value(&self) -> i32;
}

// --- 7d: Another block macro invocation to exercise the path
lazy_static! {
    static ref GLOBAL_MAP: std::collections::HashMap<String, i32> = {
        let mut m = std::collections::HashMap::new();
        m.insert(String::from("key"), 1);
        m
    };
}
