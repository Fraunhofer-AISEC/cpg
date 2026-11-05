use crate::{Graph, Kind, NONE};

/// Dump in the format of the Kotlin oracle (`TmpDump`): one line per node, pre-order, indented by
/// depth, with `refersTo` for references and `invokes` for calls.
pub fn dump(g: &Graph, unit_name: &str) -> String {
    let mut line_starts: Vec<u32> = vec![0];
    for (i, &b) in g.src.iter().enumerate() {
        if b == b'\n' {
            line_starts.push(i as u32 + 1);
        }
    }
    let pos = |off: u32| -> (usize, u32) {
        let l = line_starts.partition_point(|&s| s <= off) - 1;
        (l + 1, off - line_starts[l] + 1)
    };

    // pre-order numbering, which is what the ids of the Kotlin dump are
    let mut order: Vec<(u32, usize)> = Vec::with_capacity(g.nodes.len());
    let mut stack: Vec<(u32, usize)> = vec![(0, 0)];
    while let Some((id, depth)) = stack.pop() {
        order.push((id, depth));
        let kids: Vec<u32> = g.children(id).collect();
        stack.extend(kids.iter().rev().map(|&k| (k, depth + 1)));
    }
    let mut dump_id = vec![NONE; g.nodes.len()];
    for (i, &(id, _)) in order.iter().enumerate() {
        dump_id[id as usize] = i as u32;
    }
    let mut refers: Vec<u32> = vec![NONE; g.nodes.len()];
    for &(r, d) in &g.refers_to {
        refers[r as usize] = d;
    }
    let mut invokes: Vec<Vec<u32>> = vec![Vec::new(); g.nodes.len()];
    for &(c, f) in &g.invokes {
        invokes[c as usize].push(f);
    }

    let mut eog: Vec<Vec<(u32, Option<bool>, bool)>> = vec![Vec::new(); g.nodes.len()];
    for e in &g.eog {
        eog[e.from as usize].push((e.to, e.branch, e.unreachable));
    }

    let mut dfg_in: Vec<Vec<usize>> = vec![Vec::new(); g.nodes.len()];
    for (k, e) in g.dfg.iter().enumerate() {
        dfg_in[e.to as usize].push(k);
    }

    let ref_str = |id: u32| -> String {
        if id >= crate::SYNTH_BASE {
            let (class, name) = &g.synth[(id - crate::SYNTH_BASE) as usize];
            format!("ext:{class}({name})")
        } else {
            format!("#{}", dump_id[id as usize])
        }
    };
    let mut out = String::new();
    for (i, &(id, depth)) in order.iter().enumerate() {
        let n = g.nodes[id as usize];
        let (loc, code) = if n.start == NONE {
            ("?".to_string(), g.code_override.get(&id).cloned().unwrap_or_default())
        } else {
            let (sl, sc) = pos(n.start);
            let (el, ec) = if n.end > n.start {
                let (l, c) = pos(n.end - 1);
                (l, c + 1)
            } else {
                pos(n.end)
            };
            let code: String = String::from_utf8_lossy(&g.src[n.start as usize..n.end as usize])
                .replace('\n', "\\n")
                .chars()
                .take(60)
                .collect();
            (format!("{sl}:{sc}-{el}:{ec}"), code)
        };
        let name = if n.kind == Kind::Unit { unit_name } else { g.name_str(id) };
        let ty = g.type_str(id).unwrap_or("-");
        out.push_str(&format!(
            "{}#{} {} name='{}' type='{}' loc={} code='{}'",
            "  ".repeat(depth), i, n.kind.class_name(), name, ty, loc, code
        ));
        match n.access {
            crate::Access::Read => {}
            crate::Access::Write => out.push_str(" acc=WRITE"),
            crate::Access::ReadWrite => out.push_str(" acc=READWRITE"),
        }
        if matches!(n.kind, Kind::Ref | Kind::PointerRef | Kind::PointerDeref | Kind::Member) {
            match refers[id as usize] {
                NONE => out.push_str(" refersTo=-"),
                d => out.push_str(&format!(" refersTo=#{}", dump_id[d as usize])),
            }
        }
        if n.kind == Kind::Call {
            let t: Vec<String> =
                invokes[id as usize].iter().map(|&f| format!("#{}", dump_id[f as usize])).collect();
            out.push_str(&format!(" invokes=[{}]", t.join(",")));
        }
        if !eog[id as usize].is_empty() {
            let t: Vec<String> = eog[id as usize]
                .iter()
                .map(|&(to, b, u)| {
                    let mut s = format!("#{}", dump_id[to as usize]);
                    match b {
                        Some(true) => s.push_str(":T"),
                        Some(false) => s.push_str(":F"),
                        None => {}
                    }
                    if u {
                        s.push_str(":U");
                    }
                    s
                })
                .collect();
            out.push_str(&format!(" eog=[{}]", t.join(",")));
        }
        if !dfg_in[id as usize].is_empty() {
            let t: Vec<String> = dfg_in[id as usize]
                .iter()
                .map(|&k| {
                    let e = &g.dfg[k];
                    {
                    let mut s = format!("{}{}", ref_str(e.from), e.granularity.tag());
                    if e.fs {
                        s.push_str("!fs");
                    }
                    for (is_in, calls) in &e.ctx {
                        let c: Vec<String> = calls.iter().map(|&c| ref_str(c)).collect();
                        s.push_str(&format!("<{}:{}>", if *is_in { "in" } else { "out" }, c.join("/")));
                    }
                    s
                }
                })
                .collect();
            out.push_str(&format!(" dfg=[{}]", t.join(",")));
        }
        out.push('\n');
    }
    out
}
