// Pattern matching with various pattern types
struct Point { x: i32, y: i32 }
enum Shape {
    Circle(f64),
    Rect(f64, f64),
    Named { width: f64, height: f64 },
}
fn test_match_struct_pattern(p: Point) -> i32 {
    match p {
        Point { x, y } => x + y,
    }
}
fn test_match_struct_rename(p: Point) -> i32 {
    match p {
        Point { x: a, y: b } => a + b,
    }
}
fn test_match_tuple_struct(s: Shape) -> f64 {
    match s {
        Shape::Circle(r) => r,
        Shape::Rect(w, h) => w * h,
        Shape::Named { width, height } => width * height,
    }
}
fn test_match_or_pattern(x: i32) -> bool {
    match x {
        1 | 2 | 3 => true,
        _ => false,
    }
}
fn test_match_ref_pattern(x: &i32) {
    match x {
        &0 => {},
        val => {
            let _ = val;
        },
    }
}
fn test_match_guard(x: i32) -> &'static str {
    match x {
        n if n < 0 => "negative",
        0 => "zero",
        n if n > 100 => "big",
        _ => "small",
    }
}
fn test_match_nested_tuple(pair: (i32, i32)) -> i32 {
    match pair {
        (0, y) => y,
        (x, 0) => x,
        (x, y) => x + y,
    }
}
fn test_match_slice(data: &[i32]) -> i32 {
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
