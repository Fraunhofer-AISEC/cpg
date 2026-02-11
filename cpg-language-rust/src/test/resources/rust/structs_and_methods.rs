struct Point {
    x: i32,
    y: i32,
}

impl Point {
    fn new(x: i32, y: i32) -> Point {
        Point { x: x, y: y }
    }

    fn sum(&self) -> i32 {
        self.x + self.y
    }
}

fn test_struct_expr() {
    let p = Point { x: 1, y: 2 };
    let s = p.sum();
}
