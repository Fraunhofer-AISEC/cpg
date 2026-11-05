//! The flow-sensitive part of the points-to analysis (port of the Kotlin `PointsToPass`), stage 1:
//! reaching definitions of scalar local variables and parameters over the EOG.
//!
//! - A definition is a variable declaration, a parameter (at entry), or a written reference. A write
//!   takes effect at its `Assign` (or `++`/`--`) node, after the right-hand side was evaluated.
//! - A read reference gets an edge from every definition that reaches it. States are merged (union)
//!   where paths join, and loops are iterated to a fixpoint.
//! - Every `Return` flows into its function.
//!
//! Not yet: fields, pointers and memory addresses, arrays, calls and function summaries.

use std::collections::{BTreeMap, BTreeSet};

use cpg_core::{Access, Granularity, Graph, Kind, NONE};

/// Declaration -> the nodes whose value may be the current one.
type State = BTreeMap<u32, BTreeSet<u32>>;

pub fn pointsto(g: &mut Graph) {
    let n = g.nodes.len();
    let mut ref_of = vec![NONE; n];
    for &(r, d) in &g.refers_to {
        ref_of[r as usize] = d;
    }
    let mut succ: Vec<Vec<u32>> = vec![Vec::new(); n];
    for e in &g.eog {
        succ[e.from as usize].push(e.to);
    }
    for f in 0..n as u32 {
        if g.nodes[f as usize].kind == Kind::Function && !succ[f as usize].is_empty() {
            analyze(g, f, &ref_of, &succ);
        }
    }
}

/// The last node id in the subtree of `id` (node ids are in pre-order).
fn subtree_end(g: &Graph, id: u32) -> u32 {
    let mut last = id;
    let mut stack = vec![id];
    while let Some(x) = stack.pop() {
        last = last.max(x);
        stack.extend(g.children(x));
    }
    last
}

fn analyze(g: &mut Graph, f: u32, ref_of: &[u32], succ: &[Vec<u32>]) {
    let end = subtree_end(g, f);
    // a declaration is local if it is a parameter or a variable of this function
    let local = |d: u32| d != NONE && d >= f && d <= end && matches!(g.nodes[d as usize].kind, Kind::Param | Kind::Variable);

    // entry state: the parameters
    let mut entry = State::new();
    for p in g.children(f).filter(|&c| g.nodes[c as usize].kind == Kind::Param) {
        entry.entry(p).or_default().insert(p);
    }

    // reachable nodes and their in-states, to a fixpoint
    let mut inn: BTreeMap<u32, State> = BTreeMap::new();
    let mut work: Vec<u32> = Vec::new();
    for &s in &succ[f as usize] {
        if merge(inn.entry(s).or_default(), &entry) || !inn.contains_key(&s) {
            work.push(s);
        }
    }
    while let Some(node) = work.pop() {
        let state = inn.get(&node).cloned().unwrap_or_default();
        let out = transfer(g, node, &state, ref_of, &local);
        for &s in &succ[node as usize] {
            let slot = inn.entry(s).or_default();
            if merge(slot, &out) {
                work.push(s);
            }
        }
    }

    // edges: reads get the definitions that reach them
    let mut edges: Vec<(u32, u32)> = Vec::new();
    for (&node, state) in &inn {
        let k = g.nodes[node as usize].kind;
        if k == Kind::Ref && g.nodes[node as usize].access != Access::Write {
            let d = ref_of[node as usize];
            if local(d) {
                if let Some(defs) = state.get(&d) {
                    edges.extend(defs.iter().map(|&def| (def, node)));
                }
            }
        }
        if k == Kind::Return {
            edges.push((node, f));
        }
    }
    for (from, to) in edges {
        g.add_dfg(from, to, Granularity::Full);
    }
}

/// The state after a node.
fn transfer(g: &Graph, node: u32, state: &State, ref_of: &[u32], local: &dyn Fn(u32) -> bool) -> State {
    let mut out = state.clone();
    match g.nodes[node as usize].kind {
        Kind::Variable if local(node) => {
            out.insert(node, BTreeSet::from([node]));
        }
        Kind::Assign => {
            let lhs = g.nodes[node as usize].first_child;
            if lhs != NONE && g.nodes[lhs as usize].kind == Kind::Ref {
                let d = ref_of[lhs as usize];
                if local(d) {
                    out.insert(d, BTreeSet::from([lhs]));
                }
            }
        }
        Kind::Unary if matches!(g.name_str(node), "++" | "--") => {
            let input = g.nodes[node as usize].first_child;
            if input != NONE && g.nodes[input as usize].kind == Kind::Ref {
                let d = ref_of[input as usize];
                if local(d) {
                    out.insert(d, BTreeSet::from([input]));
                }
            }
        }
        _ => {}
    }
    out
}

/// Merges `from` into `into` (union); returns whether `into` changed.
fn merge(into: &mut State, from: &State) -> bool {
    let mut changed = false;
    for (decl, defs) in from {
        let slot = into.entry(*decl).or_default();
        for &d in defs {
            changed |= slot.insert(d);
        }
    }
    changed
}
