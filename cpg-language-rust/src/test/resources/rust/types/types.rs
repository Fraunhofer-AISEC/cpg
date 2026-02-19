fn foo(a: i32, b: &str, c: Vec<u8>) {
}

// Advanced types
pub struct Pair(i32, i32);

pub enum Option {
    Some(i32),
    None,
}

pub enum Error {
    NotFound { code: i32 },
    Other,
}

pub fn public_fn() {
    let p = Pair(1, 2);
}

fn takes_fn_ptr(f: fn(i32) -> i32) -> i32 {
    f(5)
}

fn takes_impl_trait(x: &impl Clone) {
    let y = 1;
}

fn takes_raw_ptr(p: *const i32) {
    let x = 1;
}

type Callback = fn(i32) -> i32;

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

// Raw pointer parameters
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

// Reference with lifetime
fn test_lifetime_ref<'a>(s: &'a str) -> &'a str {
    s
}

// Array type with size
fn test_array_type() {
    let arr: [i32; 5] = [1, 2, 3, 4, 5];
}

// Bounded type (dyn Display + Send)
fn test_bounded_type(x: &(dyn std::fmt::Display + Send)) {
    let _ = x;
}

// Qualified type
fn test_qualified_type() {
    let _x: <Vec<i32> as IntoIterator>::Item = 42;
}

// Turbofish type (generic_type_with_turbofish)
fn test_turbofish_type() {
    let x = Vec::<i32>::new();
}

// Function type with no return (FunctionType without return_type)
fn test_fn_type_no_return(f: fn(i32, i32)) {
    f(1, 2);
}

// Generic function with bounds
fn test_generic_with_bounds<T: Clone + Send>(x: T) -> T {
    x.clone()
}

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
