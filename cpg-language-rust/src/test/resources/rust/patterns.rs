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
