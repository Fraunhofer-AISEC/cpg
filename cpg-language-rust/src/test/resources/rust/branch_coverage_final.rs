// Branch coverage - Final coverage push - reachable but uncovered paths

// Block-style macro invocation at top level (DeclarationHandler L64)
// Without semicolon, tree-sitter produces macro_invocation directly (not expression_statement)
thread_local! {
    static THREAD_COUNTER: std::cell::Cell<i32> = std::cell::Cell::new(0);
}

// Tuple let without initializer (StatementHandler L224-227)
fn test_tuple_let_no_init() {
    let (a, b): (i32, i32);
    a = 1;
    b = 2;
    let _ = a + b;
}

// Trait with const item (DeclarationHandler L426 else->null in trait body)
trait TraitWithConst {
    const MAX_VALUE: i32;
    fn value(&self) -> i32;
}

// Another block macro invocation to exercise the path
lazy_static! {
    static ref GLOBAL_MAP: std::collections::HashMap<String, i32> = {
        let mut m = std::collections::HashMap::new();
        m.insert(String::from("key"), 1);
        m
    };
}

