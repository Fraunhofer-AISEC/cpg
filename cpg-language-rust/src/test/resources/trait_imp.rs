trait Example {
    const ID: u32;
    type Alias;

    fn id() -> u32 {
        Self::ID
    }
}

struct MyType;

impl Example for MyType {
    const ID: u32 = 42;
    type Alias = u64;

    fn id() -> u32 {
        println!("Returning ID"); // macro call
        Self::ID
    }
}

fn main() {
    println!("{}", MyType::id());
}