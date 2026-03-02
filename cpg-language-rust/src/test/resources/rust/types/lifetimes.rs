fn longest<'a>(x: &'a str, y: &'a str) -> &'a str {
    if x.len() > y.len() {
        x
    } else {
        y
    }
}

struct Excerpt<'a> {
    part: &'a str,
}

fn mutability_example() {
    let mut counter = 0;
    counter = counter + 1;

    let immutable = 42;

    let r1 = &counter;
    let r2 = &mut counter;
}

fn borrow_example(data: &mut i32) {
    let val = *data;
}
