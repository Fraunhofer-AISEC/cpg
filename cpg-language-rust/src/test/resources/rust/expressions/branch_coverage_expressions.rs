// Branch coverage - ExpressionHandler uncovered branches

// return_expression dispatched from ExpressionHandler
// When return appears as an expression (not in expression_statement)
fn expr_return_in_closure() -> i32 {
    let f = |x: i32| -> i32 { return x + 1 };
    f(5)
}

// while_expression dispatched from ExpressionHandler
// while used as trailing expression value in a block
fn expr_while_in_block() {
    let _result = {
        let mut i = 0;
        while i < 3 {
            i += 1;
        }
    };
}

// for_expression dispatched from ExpressionHandler
// for used as trailing expression value in a block
fn expr_for_in_block() {
    let _result = {
        let mut sum = 0;
        for i in 0..5 {
            sum += i;
        }
    };
}

// tuple_index_expression
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

// negative float literal
fn test_negative_float_literal() -> f64 {
    let a = -3.14;
    let b = -0.5;
    let c = -100.0;
    a + b + c
}

// integer literal suffixes
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

// uppercase hex, octal, binary prefixes
fn test_uppercase_literal_prefixes() {
    let hex_upper = 0xFF;
    let oct = 0o77;
    let bin = 0b1010;
    let _ = (hex_upper, oct, bin);
}

