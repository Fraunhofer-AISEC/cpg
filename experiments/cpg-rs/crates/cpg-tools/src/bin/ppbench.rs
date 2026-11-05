use std::time::Instant;
struct PpStatsView<'a>(&'a cpg_pp::PpStats);
impl std::fmt::Debug for PpStatsView<'_> {
    fn fmt(&self, f: &mut std::fmt::Formatter) -> std::fmt::Result {
        let s = self.0;
        write!(f, "conditionals={} undecided={} blanked={} substitutions={} expansions={}", s.conditionals, s.undecided, s.blanked_bytes, s.substitutions, s.expansions)
    }
}
fn main() {
    let path = std::env::args().nth(1).expect("path");
    let src = std::fs::read(&path).unwrap();
    let t = Instant::now();
    let (pp, stats) = cpg_pp::preprocess(&src, cpg_pp::C_STDLIB_SYMBOLS);
    let tpp = t.elapsed();
    assert_eq!(pp.len(), src.len());
    let t = Instant::now();
    let flat = cpg_ffi::flatten_with(&cpg_c::parse(&pp), &stats.spans);
    let tparse = t.elapsed();
    let nodes = flat.len() / cpg_ffi::RECORD;
    let errs = flat.chunks(cpg_ffi::RECORD).filter(|r| r[2] & 6 != 0).count();
    println!("preprocess {tpp:?}, parse+flatten {tparse:?}, nodes {nodes}, error/missing {errs}");
    println!("{:?} spans={}", PpStatsView(&stats), stats.spans.len());
    if let Some(out) = std::env::args().nth(2) { std::fs::write(out, &pp).unwrap(); }
}
