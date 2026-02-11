// for_loops.rs
fn basic_for() {
    let items = [1, 2, 3];
    for x in items {
        let y = x;
    }
}

fn labeled_for() {
    let matrix = [[1, 2], [3, 4]];
    'outer: for row in matrix {
        for val in row {
            if val > 2 {
                break 'outer;
            }
        }
    }
}

fn for_range() {
    for i in 0..10 {
        let x = i;
    }
}
