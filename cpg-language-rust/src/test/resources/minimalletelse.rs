fn mark(point: &str) {
    println!("{point}");
}

fn let_else_example(opt: Option<&str>) {
    let Some(value) = opt else {
        mark("let_else_none");
        return;
    };
    mark(value);

    mark("let_else_some");
}

fn main() {
    let_else_example(Some("hello"));
    let_else_example(None);
}
