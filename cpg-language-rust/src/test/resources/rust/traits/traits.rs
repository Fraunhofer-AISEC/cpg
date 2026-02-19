trait MyTrait {
    fn required_method(&self);
    fn default_method(&self) {
        let x = 1;
    }
}

struct MyStruct;

impl MyTrait for MyStruct {
    fn required_method(&self) {
        let y = 2;
    }
}

fn generic_foo<T: Clone>(x: T) where T: MyTrait {
    x.required_method();
}

trait Iterator {
    type Item;
    fn next(&self) -> i32;
}

struct Counter;

impl Iterator for Counter {
    type Item = i32;
    fn next(&self) -> i32 {
        let val = 0;
        val
    }
}

