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

trait Drawable {
    type Output;
    fn draw(&self) -> Self::Output;
    fn name(&self) -> &str;
}

trait Resizable: Drawable {
    fn resize(&mut self, factor: f64);
}

struct Circle {
    radius: f64,
}

impl Drawable for Circle {
    type Output = String;
    fn draw(&self) -> String {
        String::from("circle")
    }
    fn name(&self) -> &str {
        "circle"
    }
}

impl Resizable for Circle {
    fn resize(&mut self, factor: f64) {
        self.radius *= factor;
    }
}
