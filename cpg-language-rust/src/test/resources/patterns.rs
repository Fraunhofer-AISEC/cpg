#![allow(dead_code)]

enum Color {
    Red,
    Green,
    Blue,
}

struct Point {
    x: i32,
    y: i32,
}

struct Person {
    name: String,
    age: u32,
}

struct TupleStruct(i32, i32);

fn example_box_pat() {
    let boxed = Box::new(42);

    // BoxPat: dereferences and matches the inside value
    match boxed {
        box value => println!("BoxPat: {value}"),
    }

    let boxed_tuple = Box::new((1, 2));

    match boxed_tuple {
        box (a, b) => println!("BoxPat nested: {a}, {b}"),
    }
}

fn example_literal_pat() {
    let value = 10;

    // LiteralPat with integer
    match value {
        10 => println!("LiteralPat integer"),
        _ => {}
    }

    let text = "hello";

    // LiteralPat with string literal
    match text {
        "hello" => println!("LiteralPat string"),
        _ => {}
    }
}

fn example_paren_pat() {
    let pair = (1, 2);

    // ParenPat: parentheses around a pattern
    match pair {
        ((a, b)) => println!("ParenPat tuple: {a}, {b}"),
    }

    let value = Some(5);

    match value {
        (Some(x)) => println!("ParenPat nested: {x}"),
        None => {}
    }
}

fn example_path_pat() {
    let color = Color::Red;

    // PathPat: enum variant
    match color {
        Color::Red => println!("PathPat enum variant"),
        Color::Green => println!("green"),
        Color::Blue => println!("blue"),
    }

    const ANSWER: i32 = 42;

    let x = 42;

    // PathPat: constant
    match x {
        ANSWER => println!("PathPat constant"),
        _ => {}
    }
}

fn example_record_pat() {
    let point = Point { x: 10, y: 20 };

    // RecordPat: matching named fields
    match point {
        Point { x, y } => {
            println!("RecordPat: {x}, {y}");
        }
    }

    let person = Person {
        name: "Alice".into(),
        age: 30,
    };

    // RecordPat with selected fields
    match person {
        Person { name, .. } => {
            println!("RecordPat partial: {name}");
        }
    }
}

fn example_ref_pat() {
    let value = 100;

    // RefPat: matches through a reference, dereferencing into the binding
    match &value {
        &x => println!("RefPat: {x}"),
    }

    let tuple = (1, 2);

    match &tuple {
        &(a, b) => {
            println!("RefPat tuple: {a}, {b}");
        }
    }
}

fn example_rest_pat() {
    let tuple = (1, 2, 3, 4);

    // RestPat: ignore remaining fields
    match tuple {
        (first, ..) => println!("RestPat tuple first: {first}"),
    }

    let array = [10, 20, 30, 40];

    match array {
        [head, ..] => println!("RestPat slice head: {head}"),
    }
}

fn example_slice_pat() {
    let values = [1, 2, 3, 4];

    // SlicePat: fixed length
    match values {
        [a, b, c, d] => println!("SlicePat fixed: {a} {b} {c} {d}"),
    }

    let values = [10, 20, 30, 40, 50];

    // SlicePat with RestPat
    match values {
        [first, middle @ .., last] => {
            println!("SlicePat range: first={first}, middle={middle:?}, last={last}");
        }
    }

    let empty: &[i32] = &[];

    // SlicePat empty
    match empty {
        [] => println!("SlicePat empty"),
        _ => {}
    }
}

fn example_record_pat_field() {
    let point = Point { x: 5, y: 8 };

    // RecordPatField shorthand:
    // field name and binding name are identical
    match point {
        Point { x, y } => {
            println!("RecordPatField shorthand: {x}, {y}");
        }
    }

    let point = Point { x: 5, y: 8 };

    // RecordPatField explicit rename
    match point {
        Point {
            x: renamed_x,
            y: renamed_y,
        } => {
            println!("RecordPatField rename: {renamed_x}, {renamed_y}");
        }
    }
}

fn main() {
    example_box_pat();
    example_literal_pat();
    example_paren_pat();
    example_path_pat();
    example_record_pat();
    example_ref_pat();
    example_rest_pat();
    example_slice_pat();
    example_record_pat_field();
}