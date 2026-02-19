// Enum with all variant types
enum Message {
    Quit,
    Move { x: i32, y: i32 },
    Write(String),
    ChangeColor(i32, i32, i32),
}
fn test_enum_match(msg: Message) -> i32 {
    match msg {
        Message::Quit => 0,
        Message::Move { x, y } => x + y,
        Message::Write(text) => text.len() as i32,
        Message::ChangeColor(r, g, b) => r + g + b,
    }
}
