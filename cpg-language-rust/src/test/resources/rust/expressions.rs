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

fn test_closure() {
    let add = |a: i32, b: i32| a + b;
    let result = add(1, 2);
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
