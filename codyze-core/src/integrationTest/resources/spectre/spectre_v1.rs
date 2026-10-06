static ARRAY1_SIZE: usize = 16;
static ARRAY1: [u8; 16] = [0; 16];
static ARRAY2: [u8; 256 * 512] = [0; 256 * 512];
static mut TEMP: u8 = 0;

fn _mm_lfence() {}

fn victim_function(x: usize) {
    if x < ARRAY1_SIZE {
        unsafe { TEMP &= ARRAY2[ARRAY1[x] as usize * 512]; }
    }
}

fn victim_function_split(x: usize) {
    if x < ARRAY1_SIZE {
        let v = ARRAY1[x];
        unsafe { TEMP &= ARRAY2[v as usize * 512]; }
    }
}

fn victim_function_fenced(x: usize) {
    if x < ARRAY1_SIZE {
        _mm_lfence();
        unsafe { TEMP &= ARRAY2[ARRAY1[x] as usize * 512]; }
    }
}

fn safe_function(x: usize) -> u8 {
    if x < ARRAY1_SIZE {
        return ARRAY1[x];
    }
    0
}
