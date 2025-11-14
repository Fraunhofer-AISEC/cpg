#![crate_type = "cdylib"]

use std::ffi::{CStr, CString};
use libc::c_char;

use serde::Serialize;

// Use `syn` to parse Rust source into a syntax tree and produce a simplified JSON AST.
use syn::{Item};

#[repr(C)]
pub struct MyStruct {
    pub field: i32,
}

#[no_mangle]
pub extern "C" fn treble(value: i32) -> *mut MyStruct {
    let s = Box::new(MyStruct { field: value * 3 });
    Box::into_raw(s)
}

#[no_mangle]
pub extern "C" fn free_mystruct(ptr: *mut MyStruct) {
    if ptr.is_null() { return; }
    unsafe { let _ = Box::from_raw(ptr); }
}

#[derive(Serialize)]
struct SimpleItem {
    kind: String,
    name: Option<String>,
}

#[no_mangle]
pub extern "C" fn parse_rust(input: *const c_char) -> *mut c_char {
    if input.is_null() {
        return std::ptr::null_mut();
    }
    let cstr = unsafe { CStr::from_ptr(input) };
    let src = match cstr.to_str() {
        Ok(s) => s,
        Err(_) => return std::ptr::null_mut(),
    };

    match syn::parse_file(src) {
        Ok(file) => {
            let mut items: Vec<SimpleItem> = Vec::new();
            for it in file.items {
                match it {
                    Item::Fn(f) => items.push(SimpleItem { kind: "fn".into(), name: Some(f.sig.ident.to_string()) }),
                    Item::Struct(s) => items.push(SimpleItem { kind: "struct".into(), name: Some(s.ident.to_string()) }),
                    Item::Enum(e) => items.push(SimpleItem { kind: "enum".into(), name: Some(e.ident.to_string()) }),
                    Item::Mod(m) => items.push(SimpleItem { kind: "mod".into(), name: Some(m.ident.to_string()) }),
                    Item::Use(_) => items.push(SimpleItem { kind: "use".into(), name: None }),
                    _ => items.push(SimpleItem { kind: "other".into(), name: None }),
                }
            }
            let json = match serde_json::to_string(&items) {
                Ok(j) => j,
                Err(e) => format!("{{\"error\":\"serde error: {}\"}}", e),
            };
            let cstring = CString::new(json).unwrap_or_else(|_| CString::new("{\"error\":\"nul in json\"}").unwrap());
            cstring.into_raw()
        }
        Err(e) => {
            let msg = format!("{{\"error\":\"parse error: {}\"}}", e);
            CString::new(msg).unwrap().into_raw()
        }
    }
}

#[no_mangle]
pub extern "C" fn free_cstring(s: *mut c_char) {
    if s.is_null() { return; }
    unsafe { let _ = CString::from_raw(s); }
}
