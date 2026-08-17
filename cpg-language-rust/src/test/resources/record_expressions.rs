struct Point {
    x: i32,
    y: i32,
}

struct Person {
    name: String,
    age: u32,
    email: String,
}

fn print_proxy1(x: u32, y: u32) {
    println!("Point is ({}, {})", x, y);
}


fn print_proxy2(x: String, y: u32) {
    println!("Point is ({}, {})", x, y);
}

fn main() {
    // Simple record expression
    let p1 = Point { x: 10, y: 20 };

    // Record expression with multiple fields
    let person = Person {
        name: String::from("Alice"),
        age: 30,
        email: String::from("alice@example.com"),
    };

    // Record expression using struct update syntax
    let p2 = Point { x: 15, ..p1 };

    let person2 = Person {
        name: String::from("Bob"),
        ..person
    };

    print_proxy1(p1.x, p1.y);
    print_proxy2(person.name, person.age);
}

