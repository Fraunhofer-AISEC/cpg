fn main() {
    let a = 1 + 2;
    let b = !true;
    let c = (1, 2);
    let d = [1, 2, 3];
    let mut x = 1;
    x = 2;
    x += 1;
}

fn test_index() {
    let arr = [1, 2, 3];
    let x = arr[0];
    let y = arr[1 + 1];
}

fn test_range() {
    let r1 = 0..10;
    let r2 = 0..=9;
    let r3 = ..5;
    let r4 = 3..;
}

fn test_try(val: i32) -> i32 {
    let x = val;
    x
}

fn test_type_cast() {
    let x = 42;
    let y = x as i64;
}

fn test_negation() {
    let x = -5;
    let b = !true;
}

fn test_tuple_index() {
    let t = (1, 2, 3);
    let first = t.0;
}

fn test_raw_string() {
    let s = r#"hello "world""#;
}

fn expr_return_in_closure() -> i32 {
    let f = |x: i32| -> i32 { return x + 1 };
    f(5)
}

fn expr_while_in_block() {
    let _result = {
        let mut i = 0;
        while i < 3 {
            i += 1;
        }
    };
}

fn expr_for_in_block() {
    let _result = {
        let mut sum = 0;
        for i in 0..5 {
            sum += i;
        }
    };
}

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

fn test_negative_float_literal() -> f64 {
    let a = -3.14;
    let b = -0.5;
    let c = -100.0;
    a + b + c
}

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

fn test_uppercase_literal_prefixes() {
    let hex_upper = 0xFF;
    let oct = 0o77;
    let bin = 0b1010;
    let _ = (hex_upper, oct, bin);
}
