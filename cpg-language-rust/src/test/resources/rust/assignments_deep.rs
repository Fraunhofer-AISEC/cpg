// Assignments and compound assignments
fn test_assignments() {
    let mut x = 0;
    x = 42;
    let mut arr = [1, 2, 3];
    arr[0] = 10;
    let _ = (x, arr);
}
fn test_compound_assignments() {
    let mut x: i32 = 100;
    x += 1;
    x -= 2;
    x *= 3;
    x /= 4;
    x %= 5;
    x &= 0xFF;
    x |= 0x01;
    x ^= 0x10;
    x <<= 1;
    x >>= 1;
    let _ = x;
}
