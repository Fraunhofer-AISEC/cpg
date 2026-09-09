#[derive(Debug)]
struct Coord {
    a1: i32,
    b1: i32,
}

#[derive(Debug)]
enum Signal {
    Stop,
    Shift { dx: i32, dy: i32 },
    Note(&'static str),
    Tint(i32, i32, i32),
}

// --------------------------------------------------
// Output helpers (hide println! here)
// --------------------------------------------------

fn print_i32(tag: &str, num: i32) {
    println!("{} = {}", tag, num);
}

fn print_pair(tag: &str, p: i32, q: i32) {
    println!("{} = ({}, {})", tag, p, q);
}

fn print_str(tag: &str, txt: &str) {
    println!("{} = {}", tag, txt);
}

// --------------------------------------------------
// Main demonstrating let-pattern usage
// --------------------------------------------------

fn main() {
    // --------------------------------------------------
    // 1. Simple binding
    // --------------------------------------------------
    let alpha = 77;
    print_i32("alpha", alpha); // prints: "alpha = 77"

    // --------------------------------------------------
    // 2. Tuple destructuring
    // --------------------------------------------------
    let (beta, gamma) = (11, 22);
    print_pair("tuple (beta,gamma)", beta, gamma); // prints: "tuple (beta,gamma) = (11, 22)"

    let ((delta, epsilon), zeta) = ((33, 44), 55);
    print_i32("delta", delta);     // prints: "delta = 33"
    print_i32("epsilon", epsilon); // prints: "epsilon = 44"
    print_i32("zeta", zeta);       // prints: "zeta = 55"

    // --------------------------------------------------
    // 3. Struct destructuring
    // --------------------------------------------------
    let c1 = Coord { a1: 100, b1: 200 };
    let Coord { a1, b1 } = c1;
    print_pair("Coord", a1, b1); // prints: "Coord = (100, 200)"

    let c2 = Coord { a1: 300, b1: 400 };
    let Coord { a1: left_val, b1: _ } = c2;
    print_i32("left_val", left_val); // prints: "left_val = 300"

    // --------------------------------------------------
    // 4. Array destructuring (no vec!)
    // --------------------------------------------------
    let data_arr = [9, 8, 7, 6];
    let [first_val, second_val, ..] = data_arr;
    print_pair("array first two", first_val, second_val); // prints: "array first two = (9, 8)"

    // --------------------------------------------------
    // 5. ref / mut in patterns
    // --------------------------------------------------
    let greeting = "world";
    let ref greet_ref = greeting;
    print_str("ref binding", greet_ref); // prints: "ref binding = world"

    let mut counter = 12;
    let ref mut counter_ref = counter;
    *counter_ref += 5;
    print_i32("mut via pattern", counter); // prints: "mut via pattern = 17"

    // --------------------------------------------------
    // 6. Enum destructuring
    // --------------------------------------------------
    let sig = Signal::Shift { dx: 70, dy: 90 };
    let Signal::Shift { dx, dy } = sig;
    print_pair("Shift", dx, dy); // prints: "Shift = (70, 90)"

    // --------------------------------------------------
    // 7. if let (refutable pattern)
    // --------------------------------------------------
    let maybe_num = Some(999);

    if let Some(found) = maybe_num {
        print_i32("if let matched", found); // prints: "if let matched = 999"
    }

    // --------------------------------------------------
    // 8. while let (no vec!)
    // --------------------------------------------------
    let mut series = [Some(5), Some(6), Some(7), None];
    let mut idx = 0;

    while let Some(elem) = series[idx] {
        print_i32("while let value", elem); // prints: "while let value = 5", then 6, then 7
        idx += 1;
    }

    // --------------------------------------------------
    // 9. let-else
    // --------------------------------------------------
    let maybe_text = Some("Pattern");

    let Some(extracted) = maybe_text else {
        panic!("Expected value");
    };
    print_str("let-else", extracted); // prints: "let-else = Pattern"

    // --------------------------------------------------
    // 10. @ binding
    // --------------------------------------------------
    let value_check = 8;
    let bound_val @ 5..=15 = value_check;
    print_i32("range binding", bound_val); // prints: "range binding = 8"

    // --------------------------------------------------
    // 11. OR patterns
    // --------------------------------------------------
    let choice = 3;
    let 3 | 4 | 5 = choice;
    print_i32("matched 3|4|5", choice); // prints: "matched 3|4|5 = 3"

    // --------------------------------------------------
    // 12. Reference destructuring
    // --------------------------------------------------
    let ref_tuple = &(111, 222);
    let &(left_side, right_side) = ref_tuple;
    print_pair("destructured ref", left_side, right_side); // prints: "destructured ref = (111, 222)"

    // --------------------------------------------------
    // 13. Ignoring values
    // --------------------------------------------------
    let (keep_a, _, keep_c) = (13, 14, 15);
    print_pair("ignore middle", keep_a, keep_c); // prints: "ignore middle = (13, 15)"

    let large_tuple = (21, 22, 23, 24, 25);
    let (start_val, .., end_val) = large_tuple;
    print_pair("first/last", start_val, end_val); // prints: "first/last = (21, 25)"
}