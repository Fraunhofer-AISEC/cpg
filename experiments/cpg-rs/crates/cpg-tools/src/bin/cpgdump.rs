fn main() {
    let args: Vec<String> = std::env::args().collect();
    let src = std::fs::read(&args[1]).unwrap();
    let (pp, stats) = cpg_pp::preprocess(&src, cpg_pp::C_STDLIB_SYMBOLS);
    let tree = cpg_c::parse(&pp);
    let mut g = cpg_c::build(&pp, &tree, &stats.spans);
    cpg_passes::eog(&mut g);
    cpg_passes::resolve(&mut g);
    cpg_passes::typing(&mut g);
    cpg_passes::access(&mut g);
    // DFG_MODE=pointsto: the points-to pass is part of the pipeline (references are not handled here)
    cpg_passes::dfg(&mut g, std::env::var("DFG_MODE").is_ok_and(|m| m == "pointsto"));
    let unit = std::path::Path::new(&args[1]).file_name().unwrap().to_string_lossy().into_owned();
    print!("{}", cpg_core::dump(&g, &unit));
}
