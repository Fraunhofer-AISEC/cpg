fn test_unsafe() {
    let x = unsafe { 42 };
}

fn test_async_block() {
    let fut = async { 42 };
}

fn implicit_return() -> i32 {
    42
}
