#![crate_type = "cdylib"]

use std::ffi::{CStr};
use libc::c_char;

#[no_mangle]
pub extern "C" fn print_file(file: *const c_char) {
        if !file.is_null() {
            // return std::ptr::null_mut();
            let cstr = unsafe { CStr::from_ptr(file) };
            let src = match cstr.to_str() {
                Ok(s) => s,
                Err(_) => "",
            };
            println!("Hello, world: {filename}" , filename=src);
        }
}
