// Range expressions - all forms
fn test_ranges() {
    let full = 0..10;
    let inclusive = 0..=9;
    let from = 5..;
    let to = ..10;
    let to_inclusive = ..=9;
    let full_range = ..;
    let _ = (full, inclusive, from, to, to_inclusive, full_range);
}
