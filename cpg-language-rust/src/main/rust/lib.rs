uniffi::setup_scaffolding!();

use std::fs;
use ra_ap_syntax::ast::SourceFile;
use ra_ap_syntax::Edition;

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
    pub number: i32,
    pub a_string: String,
    pub optional_string: Option<String>
}

#[uniffi::export]
fn parse_rust_code(source: &str) -> Result<SourceFile, std::io::Error> {
    // This depends on what the parser API exactly is; this is a conceptual example
    let text = fs::read_to_string(source);

     match text {
         Ok(sourceCode) => Ok(SourceFile::parse(sourceCode.as_str(), Edition::CURRENT).tree()),
         Err(e) => Err(e)
     }

}