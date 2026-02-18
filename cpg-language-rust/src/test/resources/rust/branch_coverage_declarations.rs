// Branch coverage - DeclarationHandler uncovered branches

// tuple struct (ordered_field_declaration_list)
struct Pair(i32, i32);
struct Triple(f64, f64, f64);
struct Wrapper(String);

fn test_tuple_structs() {
    let p = Pair(1, 2);
    let t = Triple(1.0, 2.0, 3.0);
    let w = Wrapper(String::from("hello"));
}

// trait with type parameters
trait Converter<T, U> {
    fn convert(&self, input: T) -> U;
}

// mut parameter pattern
fn test_mut_param(mut x: i32) -> i32 {
    x += 10;
    x
}

// attribute propagation in trait body
trait Annotated {
    #[must_use]
    fn compute(&self) -> i32;
    fn describe(&self) -> &str;
}

// attribute propagation in impl body
struct MyType {
    value: i32,
}

impl MyType {
    #[inline]
    fn get_value(&self) -> i32 {
        self.value
    }

    fn set_value(&mut self, v: i32) {
        self.value = v;
    }
}

// attribute propagation in module body
mod annotated_mod {
    #[allow(dead_code)]
    pub fn helper() -> i32 { 42 }

    pub fn other() -> i32 { 0 }
}

// extern "C" block with string literal ABI
extern "C" {
    fn c_abs(x: i32) -> i32;
    fn c_strlen(s: *const u8) -> usize;
}

// empty_statement in handleNode
// Semicolons at top level can produce empty_statement
;

// macro_invocation at declaration level
macro_rules! define_const {
    ($name:ident, $val:expr) => {
        const $name: i32 = $val;
    };
}

define_const!(MY_CONST, 99);

