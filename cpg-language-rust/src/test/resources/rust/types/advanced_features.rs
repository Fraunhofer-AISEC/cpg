// Pointer types, dyn trait, never type, nested function
// Pointer types in function signatures
fn test_raw_pointers(p: *const i32, q: *mut i32) -> bool {
    p.is_null() || q.is_null()
}
// Dynamic type (dyn Trait)
fn test_dyn_trait(d: &dyn std::fmt::Display) -> String {
    format!("{}", d)
}
// Never type
fn test_never() -> ! {
    loop {}
}
// Nested function
fn test_nested_fn() -> i32 {
    fn inner_add(a: i32, b: i32) -> i32 {
        a + b
    }
    inner_add(3, 4)
}
