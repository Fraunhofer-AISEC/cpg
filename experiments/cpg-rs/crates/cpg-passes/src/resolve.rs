use cpg_core::{Graph, Kind, NONE};
use rustc_hash::FxHashMap;

/// The scope-based symbol resolution pass: walks the finished AST and adds `refers_to` edges.
/// Scopes are functions, blocks and `for` statements; a declaration is visible from where it
/// appears. Functions, enumerators and globals are visible file-wide for references that come
/// before their declaration (second phase), approximating C's implicit declarations.
pub fn resolve(g: &mut Graph) {
    // symbol index of every name that a reference uses (to find enum constants by local name)
    let mut by_text: FxHashMap<Vec<u8>, u32> = FxHashMap::default();
    for n in &g.nodes {
        if n.kind == Kind::Ref && n.name != NONE {
            by_text.entry(g.names[n.name as usize].to_vec()).or_insert(n.name);
        }
    }
    let mut scopes: Vec<FxHashMap<u32, u32>> = vec![FxHashMap::default()];
    let mut pending: Vec<(u32, u32)> = Vec::new();
    let mut stack: Vec<(u32, bool)> = vec![(0, false)];
    let mut kids: Vec<u32> = Vec::new();
    while let Some((id, exit)) = stack.pop() {
        let node = g.nodes[id as usize];
        let scoping = matches!(node.kind, Kind::Function | Kind::Block | Kind::For);
        if exit {
            scopes.pop();
            continue;
        }
        match node.kind {
            Kind::Function if node.name != NONE => {
                // the Kotlin CPG lists the body before the parameters
                let has_body = node.first_child != NONE
                    && g.nodes[node.first_child as usize].kind == Kind::Block;
                if has_body {
                    scopes[0].insert(node.name, id);
                } else {
                    scopes[0].entry(node.name).or_insert(id);
                }
            }
            Kind::Param | Kind::Variable | Kind::Typedef if node.name != NONE => {
                scopes.last_mut().unwrap().insert(node.name, id);
            }
            Kind::Enumerator if node.name != NONE => {
                // the node is named `Enum::CONSTANT`, but references use the local name
                let full = g.name_str(id);
                let local = full.rsplit("::").next().unwrap_or(full).as_bytes();
                if let Some(&sym) = by_text.get(local) {
                    scopes[0].insert(sym, id);
                }
            }
            // the symbol resolver follows the EOG, so code that is never reached stays unresolved
            Kind::Ref if !g.eog_reached(id) => {}
            Kind::Ref => {
                match scopes.iter().rev().find_map(|s| s.get(&node.name)) {
                    Some(&d) => g.refers_to.push((id, d)),
                    None => pending.push((id, node.name)),
                }
            }
            _ => {}
        }
        if scoping {
            scopes.push(FxHashMap::default());
            stack.push((id, true));
        }
        // parameters are visible in the body although they are listed after it
        if node.kind == Kind::Function {
            let mut c = node.first_child;
            while c != NONE {
                let ch = g.nodes[c as usize];
                if ch.kind == Kind::Param && ch.name != NONE {
                    scopes.last_mut().unwrap().insert(ch.name, c);
                }
                c = ch.next;
            }
        }
        kids.clear();
        let mut c = node.first_child;
        while c != NONE {
            kids.push(c);
            c = g.nodes[c as usize].next;
        }
        // the Kotlin CPG lists the body of a `for` first, but it is evaluated last
        if node.kind == Kind::For && kids.len() > 1 {
            let body = kids.remove(0);
            kids.push(body);
        }
        stack.extend(kids.iter().rev().map(|&k| (k, false)));
    }
    for (id, sym) in pending {
        match scopes[0].get(&sym) {
            Some(&d) => g.refers_to.push((id, d)),
            None => {
                g.unresolved += 1;
                g.unresolved_syms.push(sym);
            }
        }
    }
}
