fn test_labeled_block() -> i32 {
    let result = 'outer: {
        if true {
            break 'outer 42;
        }
        0
    };
    result
}

fn test_labeled_block_stmt() -> i32 {
    'block: {
        if true {
            break 'block 42;
        }
        0
    }
}
