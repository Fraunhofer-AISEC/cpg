uniffi::setup_scaffolding!();



#[uniffi::export]
fn print_string(s: String) {
    println!("{}", s);
}

#[uniffi::export]
fn get_some_struct() -> SomeStruct {
    SomeStruct{number: 42, a_string: "Hello, World!".to_string(), optional_string: None}
}

#[derive(uniffi::Object)]
struct SomeStruct {
    number: i32,
    a_string: String,
    optional_string: Option<String>
}
