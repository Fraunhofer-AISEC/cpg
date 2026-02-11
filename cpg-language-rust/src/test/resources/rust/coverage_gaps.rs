// Tuple index expression (t.0, t.1)
fn test_tuple_index() {
    let t = (10, 20, 30);
    let first = t.0;
    let second = t.1;
}

// Negative literal (used in patterns, e.g., match arms)
fn test_negative_literal() -> i32 {
    let x = 5;
    match x {
        -1 => -100,
        0 => 0,
        _ => 1,
    }
}

// Continue with label
fn test_continue_with_label() {
    'outer: for i in 0..10 {
        for j in 0..10 {
            if j == 5 {
                continue 'outer;
            }
        }
    }
}

// Continue without label
fn test_continue_simple() {
    for i in 0..10 {
        if i == 5 {
            continue;
        }
    }
}

// Generic function reference without call
fn identity<T>(x: T) -> T { x }
fn test_generic_ref() {
    let f = identity::<i32>;
}

// Break with value
fn test_break_with_value() -> i32 {
    let result = loop {
        break 42;
    };
    result
}

// Method call on field expression
fn test_method_on_field() {
    let s = String::from("hello");
    let len = s.len();
    let upper = s.to_uppercase();
}

// Chained method calls
fn test_chained_methods() {
    let v: Vec<i32> = Vec::new();
    let result = v.iter().count();
}

// Complex closures
fn test_complex_closure() {
    let add = |a: i32, b: i32| -> i32 { a + b };
    let no_params = || 42;
    let multi_line = |x: i32| {
        let y = x + 1;
        y * 2
    };
}

// All binary operators
fn test_all_binary_ops() {
    let a = 10;
    let b = 3;
    let _add = a + b;
    let _sub = a - b;
    let _mul = a * b;
    let _div = a / b;
    let _rem = a % b;
    let _and = a & b;
    let _or = a | b;
    let _xor = a ^ b;
    let _shl = a << b;
    let _shr = a >> b;
    let _eq = a == b;
    let _ne = a != b;
    let _lt = a < b;
    let _le = a <= b;
    let _gt = a > b;
    let _ge = a >= b;
    let _land = true && false;
    let _lor = true || false;
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

// Array with index
fn test_array_index() {
    let arr = [1, 2, 3, 4, 5];
    let first = arr[0];
    let last = arr[4];
}

// If-else chain
fn test_if_else_chain(x: i32) -> &'static str {
    if x < 0 {
        "negative"
    } else if x == 0 {
        "zero"
    } else {
        "positive"
    }
}

// While let
fn test_while_let() {
    let mut stack: Vec<i32> = Vec::new();
    while let Some(top) = stack.pop() {
        let _ = top;
    }
}
