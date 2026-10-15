fn main() {
    let result: Result<i32, String> = Ok(42);

    // Try expression that succeeds
    let value = result?;

    // Use the value
    println!("{}", value);
}

fn try_in_function() -> Result<i32, String> {
    let x = Some(10);

    // Try expression in a function returning Result
    let inner_result: Result<i32, String> = Ok(5);
    let y = inner_result?;

    let z = y;

    Ok(x.unwrap_or(0) + y)
}

