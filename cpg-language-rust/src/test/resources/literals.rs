fn main() {
    // Character literals
    let ch1 = 'a';
    let ch2 = 'Z';
    let ch3 = '0';
    let ch_special = '\n';

    // String literals
    let s1 = "Hello, World!";
    let s2 = "Rust";
    let s_empty = "";

    // Integer literals
    let i1 = 42;
    let i2 = 1_000_000;
    let i3 = 0xFF;
    let i4 = 0o77;
    let i5 = 0b1010;

    // Floating point literals
    let f1 = 3.14;
    let f2 = 1.0;
    let f3 = 2.5e10;
    let f4 = 2e-10;

    // Boolean literals
    let b1 = true;
    let b2 = false;

    // Byte literals
    let byte1 = b'A';
    let byte2 = b'0';

    // Byte string literals
    let bytes = b"hello";

    println!("{} {} {} {}", ch1, s1, i1, f1);
}

fn literal_in_expression() {
    let result = 10 + 20 * 2;
    let string = "computed: ".to_string() + &result.to_string();
    let boolean_check = true && false;

    println!("{} {}", result, string);
}

