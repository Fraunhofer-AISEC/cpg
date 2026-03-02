struct Point {
    x: i32,
    y: i32,
}

impl Point {
    fn new(x: i32, y: i32) -> Point {
        Point { x: x, y: y }
    }

    fn dist_sq(&self) -> i32 {
        self.x * self.x + self.y * self.y
    }
}

fn generic_id<T>(x: T) -> T {
    x
}

fn main() {
    let p = Point::new(3, 4);
    let d = p.dist_sq();

    if d > 10 {
        let val = generic_id(100);
    } else {
        let val = generic_id(0);
    }

    let mut vec = Vec::new();
    vec.push(1);
    vec.push(2);

    for i in vec {
        let _ = i;
    }
}
