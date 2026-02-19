fn if_let() {
    let opt = Some(5);
    if let Some(x) = opt {
        let y = x;
    }
}

fn while_let() {
    let mut iter = [1, 2, 3].iter();
    while let Some(x) = iter.next() {
        let y = x;
    }
}
