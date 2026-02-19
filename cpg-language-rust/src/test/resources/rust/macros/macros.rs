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

fn test_macro_args() {
    let name = "world";
    println!("hello {}", name);
    let v = vec![1, 2, 3];
    let _ = v;
}

thread_local! {
    static THREAD_COUNTER: std::cell::Cell<i32> = std::cell::Cell::new(0);
}

fn test_tuple_let_no_init() {
    let (a, b): (i32, i32);
    a = 1;
    b = 2;
    let _ = a + b;
}

trait TraitWithConst {
    const MAX_VALUE: i32;
    fn value(&self) -> i32;
}

lazy_static! {
    static ref GLOBAL_MAP: std::collections::HashMap<String, i32> = {
        let mut m = std::collections::HashMap::new();
        m.insert(String::from("key"), 1);
        m
    };
}
