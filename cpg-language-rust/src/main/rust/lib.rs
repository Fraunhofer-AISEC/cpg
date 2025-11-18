uniffi::setup_scaffolding!();

#[uniffi::export]
fn print_string(s: String) {
    println!("{}", s);
}
