// Type cast, unsafe, and async blocks
fn test_type_cast() {
    let x: i32 = 42;
    let y = x as f64;
    let z = y as i32;
    let p = &x as *const i32;
    let _ = (y, z, p);
}
fn test_unsafe() {
    let mut x = 42;
    let r = &mut x as *mut i32;
    unsafe {
        *r = 100;
    }
}
fn test_async() {
    let fut = async {
        42
    };
    let fut2 = async move {
        100
    };
}
