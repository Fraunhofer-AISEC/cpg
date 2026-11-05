//! Runs the pipeline up to and including the points-to pass on one file and prints the timings.
//! `PT_TIMEOUT_MS` sets a per-function timeout, `PT_LOG_MS` logs functions slower than that.
use std::time::Instant;
fn main() {
    let path = std::env::args().nth(1).expect("path");
    std::thread::Builder::new()
        .stack_size(1 << 29)
        .spawn(move || {
            let src = std::fs::read(&path).unwrap();
            let t0 = Instant::now();
            let (pp, stats) = cpg_pp::preprocess(&src, cpg_pp::C_STDLIB_SYMBOLS);
            let tree = cpg_c::parse(&pp);
            let mut g = cpg_c::build(&pp, &tree, &stats.spans);
            cpg_passes::eog(&mut g);
            cpg_passes::resolve(&mut g);
            cpg_passes::typing(&mut g);
            cpg_passes::access(&mut g);
            cpg_passes::dfg(&mut g, true);
            eprintln!("frontend + passes up to DFG: {:?}", t0.elapsed());
            let t = Instant::now();
            let before = g.dfg.len();
            cpg_passes::pointsto2(&mut g);
            println!(
                "points-to: {:?}, DFG edges {} -> {}, synthetic nodes {}",
                t.elapsed(),
                before,
                g.dfg.len(),
                g.synth.len()
            );
        })
        .unwrap()
        .join()
        .unwrap();
}
