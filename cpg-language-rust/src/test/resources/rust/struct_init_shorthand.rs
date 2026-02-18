// Struct with shorthand initializer
struct Config {
    width: i32,
    height: i32,
    debug: bool,
}
fn test_shorthand_init() {
    let width = 800;
    let height = 600;
    let debug = true;
    let config = Config { width, height, debug };
}
