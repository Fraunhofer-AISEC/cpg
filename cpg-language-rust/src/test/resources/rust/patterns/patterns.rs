struct Point {
    x: i32,
    y: i32,
}

fn test_struct_pattern() {
    let p = Point { x: 1, y: 2 };
    let Point { x, y } = p;
    let sum = x + y;
}

fn test_or_pattern(val1: i32) -> i32 {
    match val1 {
        1 | 2 | 3 => 10,
        _ => 0,
    }
}

fn test_slice_pattern() {
    let arr = [1, 2, 3, 4, 5];
    let [first, ..] = arr;
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
