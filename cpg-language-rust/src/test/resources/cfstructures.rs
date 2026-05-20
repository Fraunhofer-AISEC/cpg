fn mark(point: &str) {
    println!("{point}");
}

fn if_examples(value: i32) {
    // if without else
    if value > 0 {
        mark("if_without_else_true");
    }

    // if with else
    if value % 2 == 0 {
        mark("if_with_else_true");
    } else {
        mark("if_with_else_false");
    }

    // if / else if / else
    if value < 0 {
        mark("if_else_if_negative");
    } else if value == 0 {
        mark("if_else_if_zero");
    } else {
        mark("if_else_if_positive");
    }

    // nested if
    if value >= 0 {
        mark("nested_if_outer_true");

        if value > 10 {
            mark("nested_if_inner_true");
        } else {
            mark("nested_if_inner_false");
        }
    } else {
        mark("nested_if_outer_false");
    }

    // if as expression
    let result = if value > 100 {
        mark("if_expression_true");
        "large"
    } else {
        mark("if_expression_false");
        "small"
    };

    println!("result = {result}");
}

fn if_let_examples(opt: Option<i32>) {
    // if let without else
    if let Some(v) = opt {
        mark("if_let_without_else_some");
    }

}

fn match_examples(value: i32, opt: Option<i32>) {
    // simple match
    match value {
        0 => mark("match_zero"),
        1 => mark("match_one"),
        _ => mark("match_default"),
    }

    // match with ranges
    match value {
        0..=10 => mark("match_range_small"),
        11..=100 => mark("match_range_medium"),
        _ => mark("match_range_large"),
    }

    // match with guard
    match value {
        x if x < 0 => mark("match_guard_negative"),
        x if x % 2 == 0 => mark("match_guard_even"),
        _ => mark("match_guard_other"),
    }

    // match on enum-like structure
    match opt {
        Some(v) => {
            mark("match_option_some");
        }
        None => {
            mark("match_option_none");
        }
    }
}

fn loop_examples() {
    // infinite loop with break
    let mut counter = 0;

    loop {
        if counter >= 2 {
            mark("loop_break");
            break;
        }

        mark("loop_iteration");
        counter += 1;
    }

    // loop with value
    let mut x = 0;

    let result = loop {
        x += 1;

        if x == 3 {
            mark("loop_break_with_value");
            break x * 10;
        }

        mark("loop_continue_iteration");
    };

    println!("loop result = {result}");
}

fn while_examples() {
    // standard while
    let mut count = 0;

    while count < 3 {
        mark("while_iteration");
        count += 1;
    }

    // while with continue
    let mut n = 0;

    while n < 5 {
        n += 1;

        if n % 2 == 0 {
            mark("while_continue");
            continue;
        }

        mark("while_non_continue");
    }
}

fn while_let_examples() {
    // while let with Some
    let mut values = vec![1, 2, 3];

    while let Some(v) = values.pop() {
        mark("while_let_some");
    }

    // while let with Result
    let mut results = vec![Ok(1), Ok(2), Err("stop")];

    while let Some(item) = results.pop() {
        match item {
            Ok(v) => {
                mark("while_let_result_ok");
            }
            Err(e) => {
                mark("while_let_result_err");
                break;
            }
        }
    }
}

fn for_examples() {
    // for over range
    for i in 0..3 {
        mark("for_range");
    }

    // for over iterator
    let items = ["a", "b", "c"];

    for item in items {
        mark("for_iterator");
    }

    // for with enumerate
    for (index, value) in items.iter().enumerate() {
        mark("for_enumerate");
    }
}

fn let_else_example(opt: Option<&str>) {
    let Some(value) = opt else {
        mark("let_else_none");
        return;
    };
    value;

    mark("let_else_some");
}

fn main() {
    if_examples(12);
    if_examples(-1);

    if_let_examples(Some(10));
    if_let_examples(None);

    match_examples(5, Some(42));
    match_examples(-2, None);

    loop_examples();

    while_examples();

    while_let_examples();

    for_examples();

    let_else_example(Some("hello"));
    let_else_example(None);
}
