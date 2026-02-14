// Generic types with type arguments
fn test_generic_types() {
    let v: Vec<i32> = Vec::new();
    let m: HashMap<String, Vec<i32>> = HashMap::new();
    let o: Option<bool> = Some(true);
}

// Tuple types
fn test_tuple_types() -> (i32, String, bool) {
    let pair: (i32, f64) = (1, 2.0);
    (42, String::from("hello"), true)
}

// Function types
fn test_function_types() {
    let f: fn(i32) -> bool = |x| x > 0;
    let g: fn(i32, i32) -> i32 = |a, b| a + b;
}

// Never type
fn test_never_type() -> ! {
    panic!("diverging")
}

// Unit type
fn test_unit_type() -> () {
    let u: () = ();
}

// Dynamic types
fn test_dynamic_types(d: &dyn Display) {
    let _x = d;
}

// Impl trait (abstract type)
fn test_impl_trait(x: impl Clone) -> impl Display {
    x
}

// Pointer types
fn test_pointer_types() {
    let x = 42;
    let p: *const i32 = &x;
    let q: *mut i32 = &x as *const i32 as *mut i32;
}

// Reference with lifetime
fn test_lifetime_ref<'a>(s: &'a str) -> &'a str {
    s
}

// Array type with size
fn test_array_type() {
    let arr: [i32; 5] = [1, 2, 3, 4, 5];
}

// Bounded type (T + Send + Sync)
fn test_bounded_type<T: Clone + Send + Sync>(x: T) {}

// Qualified type
fn test_qualified_type() {
    let _x: <Vec<i32> as IntoIterator>::Item = 42;
}

// Scoped type
fn test_scoped_type() {
    let _v: std::vec::Vec<i32> = Vec::new();
}

// Turbofish type (generic_type_with_turbofish)
fn test_turbofish_type() {
    let x = Vec::<i32>::new();
}
