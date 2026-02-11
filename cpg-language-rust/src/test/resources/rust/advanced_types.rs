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
