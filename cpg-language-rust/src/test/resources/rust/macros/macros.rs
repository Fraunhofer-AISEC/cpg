#[derive(Clone, Debug)]
struct Point {
    x: i32,
    y: i32,
}

#[derive(Default)]
struct Config {
    name: i32,
}

macro_rules! my_macro {
    ($x:expr) => {
        $x + 1
    };
}

fn use_macros() {
    let a = my_macro!(5);
    println!("hello");
}
