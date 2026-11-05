use std::collections::HashMap;
fn main() {
    let path = std::env::args().nth(1).unwrap();
    let src = std::fs::read(&path).unwrap();
    let tree = cpg_c::parse(&src);
    let mut cur = tree.walk();
    let mut hist: HashMap<String, usize> = HashMap::new();
    let mut samples: Vec<String> = vec![];
    loop {
        let n = cur.node();
        if n.is_error() || n.is_missing() {
            let line = n.start_position().row;
            let text = std::str::from_utf8(&src[n.start_byte()..n.end_byte().min(n.start_byte() + 60)]).unwrap_or("?").replace('\n', " ");
            let key = if n.is_missing() { format!("MISSING {}", n.kind()) } else {
                text.split(|c: char| !c.is_alphanumeric() && c != '_').find(|w| !w.is_empty()).unwrap_or("?").to_string() };
            *hist.entry(key).or_default() += 1;
            if samples.len() < 12 && n.is_error() { samples.push(format!("{}:{}: {}", line + 1, n.start_position().column, text)); }
        }
        if cur.goto_first_child() { continue; }
        loop {
            if cur.goto_next_sibling() { break; }
            if !cur.goto_parent() { 
                let mut v: Vec<_> = hist.into_iter().collect();
                v.sort_by(|a, b| b.1.cmp(&a.1));
                for (k, c) in v.iter().take(15) { println!("{c:5}  {k}"); }
                println!("--- samples");
                for s in samples { println!("{s}"); }
                return;
            }
        }
    }
}
