// Generic function reference without call
fn identity<T>(x: T) -> T { x }
fn test_generic_ref() {
    let f = identity::<i32>;
}
// Method call on field expression
fn test_method_on_field() {
    let s = String::from("hello");
    let len = s.len();
    let upper = s.to_uppercase();
}
// Chained method calls
fn test_chained_methods() {
    let v: Vec<i32> = Vec::new();
    let result = v.iter().count();
}
