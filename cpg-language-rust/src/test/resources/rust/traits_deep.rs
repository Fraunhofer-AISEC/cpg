// Traits with methods and associated types
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
