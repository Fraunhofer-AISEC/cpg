fn match_guard() {
    let x = 4;
    match x {
        y if y < 5 => {
            let z = 1;
        },
        _ => {
            let z = 2;
        }
    }
}
