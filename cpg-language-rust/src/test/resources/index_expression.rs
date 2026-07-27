fn show<T: std::fmt::Display>(value: T) {
    println!("{value}");
}

fn show_dbg<T: std::fmt::Debug>(value: T) {
    println!("{value:?}");
}

// ---------------------------------------------------------------------
// 1. Array indexing
// ---------------------------------------------------------------------
fn array_index() {
    let numbers = [10, 20, 30, 40];

    show(numbers[2]);
}

// ---------------------------------------------------------------------
// 2. Vector indexing
// ---------------------------------------------------------------------
fn vector_index() {
    let numbers = vec![10, 20, 30, 40];

    show(numbers[1]);
}

// ---------------------------------------------------------------------
// 3. Mutable indexing
// ---------------------------------------------------------------------
fn mutable_index() {
    let mut numbers = vec![1, 2, 3];

    numbers[1] = 42;

    show_dbg(&numbers);
}

// ---------------------------------------------------------------------
// 4. String slicing (Range)
// ---------------------------------------------------------------------
fn string_slice() {
    let s = "Hello";

    let part = &s[1..4];

    show(part);
}

// ---------------------------------------------------------------------
// 5. Slice indexing
// ---------------------------------------------------------------------
fn slice_index() {
    let data = [10, 20, 30, 40, 50];

    let middle = &data[1..4];

    show_dbg(middle);
}

// ---------------------------------------------------------------------
// 6. Different range types
// ---------------------------------------------------------------------
fn ranges() {
    let v = [0, 1, 2, 3, 4];

    show_dbg(&v[..]);     // RangeFull
    show_dbg(&v[..3]);    // RangeTo<usize>
    show_dbg(&v[2..]);    // RangeFrom<usize>
    show_dbg(&v[1..4]);   // Range<usize>
    show_dbg(&v[..=2]);   // RangeToInclusive<usize>
    show_dbg(&v[1..=3]);  // RangeInclusive<usize>
}

fn main() {
    array_index();
    vector_index();
    mutable_index();
    string_slice();
    slice_index();
    ranges();
}