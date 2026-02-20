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

enum MessageAdvanced {
    Quit,
    Move { x: i32, y: i32 },
    Write(String),
    ChangeColor(i32, i32, i32),
}

fn test_enum_match(msg: MessageAdvanced) -> i32 {
    match msg {
        MessageAdvanced::Quit => 0,
        MessageAdvanced::Move { x, y } => x + y,
        MessageAdvanced::Write(text) => text.len() as i32,
        MessageAdvanced::ChangeColor(r, g, b) => r + g + b,
    }
}
