fn loop_labels() {
    'outer: loop {
        'inner: while true {
            break 'outer;
        }
    }
}
