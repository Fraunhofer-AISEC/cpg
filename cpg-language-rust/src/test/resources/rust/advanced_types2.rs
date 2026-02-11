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
