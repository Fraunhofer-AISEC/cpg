use std::collections::BTreeMap;
use std::time::Instant;
fn main() {
    let path = std::env::args().nth(1).expect("path");
    std::thread::Builder::new().stack_size(1 << 29).spawn(move || {
        let src = std::fs::read(&path).unwrap();
        for run in 0..3 {
            let t = Instant::now();
            let (pp, stats) = cpg_pp::preprocess(&src, cpg_pp::C_STDLIB_SYMBOLS);
            let t_pp = t.elapsed();
            let t = Instant::now();
            let tree = cpg_c::parse(&pp);
            let t_parse = t.elapsed();
            let t = Instant::now();
            let mut g = cpg_c::build(&pp, &tree, &stats.spans);
            let t_tr = t.elapsed();
            let t = Instant::now();
            cpg_passes::eog(&mut g);
            let t_eog = t.elapsed();
            let t = Instant::now();
            cpg_passes::resolve(&mut g);
            let t_res = t.elapsed();
            let t = Instant::now();
            cpg_passes::typing(&mut g);
            let t_typing = t.elapsed();
            println!("run {run}: preprocess {t_pp:?}, parse {t_parse:?}, translate {t_tr:?}, eog {t_eog:?}, resolve pass {t_res:?}, typing pass {t_typing:?}");
            if run == 2 {
                let mut by: BTreeMap<String, usize> = BTreeMap::new();
                for n in &g.nodes { *by.entry(format!("{:?}", n.kind)).or_default() += 1; }
                println!("nodes {} ({} MB), names {}, refers_to {}, unresolved refs {}, expansions {}",
                    g.nodes.len(), g.nodes.len() * std::mem::size_of::<cpg_core::Node>() >> 20,
                    g.names.len(), g.refers_to.len(), g.unresolved, stats.expansions);
                println!("{by:?}");
                let mut h: std::collections::HashMap<u32, usize> = Default::default();
                for &s in &g.unresolved_syms { *h.entry(s).or_default() += 1; }
                let mut v: Vec<_> = h.into_iter().collect();
                v.sort_by(|a, b| b.1.cmp(&a.1));
                println!("distinct unresolved names: {}", v.len());
                for (s, c) in v.iter().take(25) { println!("{c:6} {}", String::from_utf8_lossy(&g.names[*s as usize])); }
            }
        }
    }).unwrap().join().unwrap();
}
