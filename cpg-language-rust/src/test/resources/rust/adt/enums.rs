enum Color {
    Red,
    Green,
    Blue,
}

enum Shape {
    Circle(f64),
    Rectangle(f64, f64),
}

enum Message {
    Quit,
    Move { x: i32, y: i32 },
    Write(String),
}
