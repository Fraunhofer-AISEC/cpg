uniffi::setup_scaffolding!();

use std::fs;
use std::string::ParseError;
use ra_ap_syntax::ast::{AsmExpr, Const, Enum, ExternBlock, ExternCrate, Fn, HasModuleItem, Impl, MacroCall, MacroDef, MacroRules, Module, SourceFile, Static, Struct, Trait, TypeAlias, Union, Use};
use ra_ap_syntax::{AstNode, Edition};

#[derive(uniffi::Object)]
pub struct RSSourceFile {
//    file: SourceFile,
}


pub struct RSAsmExpr {}

#[derive(uniffi::Object)]
pub enum RSItem {
    AsmExpr(RSAsmExpr),
}

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
fn parse_rust_code(source: &str) -> Option<RSSourceFile>  {
    // This depends on what the parser API exactly is; this is a conceptual example
    let text = fs::read_to_string(source);

     let res = match text {
         Ok(sourceCode) => Ok(SourceFile::parse(sourceCode.as_str(), Edition::CURRENT).tree()),
         Err(e) => Err(e)
     };
    /*if let Ok(file) = res {
        for item in file.items() {
            println!("Sourcefile subitem: {:?}", item.syntax().to_string());
        }
    }*/

    match res {
        Ok(sf) => Some(RSSourceFile{}),
        Err(e) => None
    }
}

