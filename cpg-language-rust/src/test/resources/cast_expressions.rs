fn main() {
    // Cast from integer to integer
    let x: u32 = 10;
    let y: i64 = x as i64;

    // Cast from float to integer
    let f: f32 = 3.14;
    let i: i32 = f as i32;

    // Cast from integer to float
    let i2: i32 = 42;
    let f2: f64 = i2 as f64;

    // Cast from char to integer
    let ch: char = 'A';
    let code: u32 = ch as u32;

    // Cast from integer to boolean (if supported by Rust semantics)
    // Note: Direct int to bool casts are not allowed in Rust
    // but we can test casting through other types

    // Cast in expressions
    let result = (10 as i64) + (20 as i64);

    // Multiple casts
    let original = 256u16;
    let casted = original as u8 as u16;

    println!("{} {} {} {}", y, i, f2, code);
    println!("Result: {}", result);
    println!("Casted: {}", casted);
}

fn cast_in_function(value: i32) -> f64 {
    (value as f64) * 2.5
}

fn cast_in_condition(x: i64) {
    if (x as i32) > 0 {
        println!("Positive");
    }
}

