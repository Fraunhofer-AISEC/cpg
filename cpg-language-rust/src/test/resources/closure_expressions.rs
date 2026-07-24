fn main() {
    // Simple closure with one parameter
    let add_one = |x: i32| x + 1;
    let result1 = add_one(5);

    // Closure with multiple parameters
    let add = |a: i32, b: i32| a + b;
    let result2 = add(10, 20);

    // Closure without type annotations
    let multiply = |x, y| x * y;
    let result3 = multiply(3, 4);

    // Closure with a block body
    let complex = |x: i32| {
        let doubled = x * 2;
        doubled + 1
    };
    let result4 = complex(5);

    // Closure capturing variables from outer scope
    let y = 10;
    let add_y = |x: i32| x + y;
    let result5 = add_y(5);

    // Closure that moves captured variables
    let owned = String::from("hello");
    let take_owned = move || owned.len();
    let result6 = take_owned();

    println!("{} {} {} {} {}", result1, result2, result3, result4, result5);
}

fn apply_closure<F>(f: F, value: i32) -> i32
where
    F: Fn(i32) -> i32,
{
    f(value)
}

fn filter_closure() {
    let numbers = vec![1, 2, 3, 4, 5];
    let evens: Vec<i32> = numbers.into_iter().filter(|&x| x % 2 == 0).collect();
    println!("{:?}", evens);
}

