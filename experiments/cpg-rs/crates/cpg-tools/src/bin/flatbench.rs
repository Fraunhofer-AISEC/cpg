use std::time::Instant;
fn main() {
    let path = std::env::args().nth(1).expect("path");
    let src = std::fs::read(&path).unwrap();
    for i in 0..5 {
        let t = Instant::now();
        let tree = cpg_c::parse(&src);
        let tp = t.elapsed();
        let t = Instant::now();
        let flat = cpg_ffi::flatten(&tree);
        let tf = t.elapsed();
        let n = flat.len() / cpg_ffi::RECORD;
        let errs = flat.chunks(cpg_ffi::RECORD).filter(|r| r[2] & 6 != 0).count();
        println!("run {i}: parse {tp:?}, flatten {tf:?}, nodes {n}, error/missing {errs}, buf {} MB",
            flat.len() >> 20);
    }
}
