mod my_mod {
    fn inner_func() -> i32 {
        return 42;
    }
}

type MyAlias = i32;

fn test_types(a: &i32, b: [i32; 3]) {
    let x: MyAlias = 1;
}

fn test_complex_types() {
    let a: (i32, bool) = (1, true);
    let b: [i32; 3] = [1, 2, 3];
    let c: Vec<i32> = Vec::new();
}

fn test_unsupported() {
    // This will trigger the "Unknown expression type" or "Unknown statement type" problem nodes
}
