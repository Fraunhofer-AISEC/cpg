// Type system - reference types with mut/lifetime
fn test_mutable_ref(x: &mut i32) {
    *x += 1;
}
fn test_lifetime_ref<'a>(x: &'a str) -> &'a str {
    x
}
fn test_double_ref(x: &&i32) -> i32 {
    **x
}
