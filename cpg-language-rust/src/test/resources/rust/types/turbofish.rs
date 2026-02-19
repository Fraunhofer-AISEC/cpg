fn identity<T>(x: T) -> T {
    x
}

fn test_turbofish() {
    let val1 = identity::<i32>(42);
}
