fn parse_pair(input: &str) -> (i32, i32) {
    let mut parts = input.split(',');

    // Try to extract two values
    let (Some(a_str), Some(b_str)) = (parts.next(), parts.next()) else {
        panic!("Expected two comma-separated values");
    };

    // Try to parse them as integers
    let Ok(a) = a_str.parse::<i32>() else {
        panic!("First value is not a valid integer");
    };

    let Ok(b) = b_str.parse::<i32>() else {
        panic!("Second value is not a valid integer");
    };

    (a, b)
}

fn main() {
    let result = parse_pair("10,20");
    println!("{:?}", result); // (10, 20)
}