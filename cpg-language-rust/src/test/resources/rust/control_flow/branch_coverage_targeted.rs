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

