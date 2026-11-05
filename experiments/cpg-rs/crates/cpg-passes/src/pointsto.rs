//! The flow-sensitive part of the points-to analysis (port of the Kotlin `PointsToPass`), stage 1:
//! reaching definitions of scalar local variables and parameters over the EOG.
//!
//! - A definition is a variable declaration, a parameter (at entry), or a written reference. A write
//!   takes effect at its `Assign` (or `++`/`--`) node, after the right-hand side was evaluated.
//! - A read reference gets an edge from every definition that reaches it. States are merged (union)
//!   where paths join, and loops are iterated to a fixpoint.
//! - Every `Return` flows into its function.
//!
//! Stage 2: struct fields, also nested. A struct variable has three kinds of definitions, for a
//! path of fields (`o.in.a` is `[in, a]`):
//! - `Whole`: the declaration (and whole assignments),
//! - `Field`: the member access that last wrote a path, read by a member access of that path,
//! - `Partial`: for every prefix of a written path, the node that stands for the prefix (the base
//!   reference, or the member access) as a partial definition of its parent, with the next field as
//!   granularity. A read of the parent gets these.
//!
//! A member that is read without any definition for its path (or its sub-fields) gets the
//! declaration of its field. A whole assignment to the variable does not copy field definitions; it
//! only replaces everything known about the variable.
//!
//! Not yet: pointers and memory addresses, arrays, calls and function summaries.

use std::collections::{BTreeMap, BTreeSet};

use cpg_core::{Access, Granularity, Graph, Kind, NONE};

/// What a set of definitions is about: a declaration as a whole, one of its fields (written by the
/// member access), or a field as a partial definition of the whole (the base reference of a write).
#[derive(Clone, PartialEq, Eq, PartialOrd, Ord, Debug)]
enum Key {
    Whole(u32),
    /// The declaration and the path of fields.
    Field(u32, Vec<u32>),
    /// The declaration, the path of the parent, and the field in it.
    Partial(u32, Vec<u32>, u32),
}

impl Key {
    fn decl(&self) -> u32 {
        match self {
            Key::Whole(d) | Key::Field(d, _) | Key::Partial(d, _, _) => *d,
        }
    }
}

/// A member access chain on a local variable: `nodes[0]` is the base reference, `nodes[i]` the member
/// access of `path[i - 1]`.
struct MemberPath {
    decl: u32,
    nodes: Vec<u32>,
    path: Vec<u32>,
}

/// Key -> the nodes whose value may be the current one.
type State = BTreeMap<Key, BTreeSet<u32>>;

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
        let kind = g.nodes[f as usize].kind;
        if kind == Kind::Function && !succ[f as usize].is_empty() {
            analyze(g, f, &ref_of, &succ);
        }
        // the implicit constructor of a record returns its `this`
        if kind == Kind::Constructor {
            let this: Vec<u32> = g
                .children(f)
                .filter(|&c| g.nodes[c as usize].kind == Kind::Variable && g.name_str(c) == "this")
                .collect();
            for t in this {
                g.add_dfg(t, f, Granularity::Full);
            }
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
        entry.entry(Key::Whole(p)).or_default().insert(p);
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
    let mut edges: Vec<(u32, u32, Granularity)> = Vec::new();
    for (&node, state) in &inn {
        let kind = g.nodes[node as usize].kind;
        let access = g.nodes[node as usize].access;
        if kind == Kind::Ref && access != Access::Write {
            let d = ref_of[node as usize];
            if local(d) {
                for (key, defs) in state.iter().filter(|(k, _)| k.decl() == d) {
                    let gran = match key {
                        Key::Whole(_) => Granularity::Full,
                        // only the fields of the variable itself, not of its fields
                        Key::Partial(_, parent, fld) if parent.is_empty() => {
                            Granularity::Partial(format!("Field:{}", g.name_str(*fld)))
                        }
                        _ => continue,
                    };
                    edges.extend(defs.iter().map(|&def| (def, node, gran.clone())));
                }
            }
        }
        if kind == Kind::Member && access != Access::Write {
            if let Some(m) = member_path(g, node, ref_of, &local) {
                let mut found = false;
                if let Some(defs) = state.get(&Key::Field(m.decl, m.path.clone())) {
                    found |= !defs.is_empty();
                    edges.extend(defs.iter().map(|&def| (def, node, Granularity::Full)));
                }
                // what was written to the fields of this member
                for (key, defs) in state.iter().filter(|(k, _)| k.decl() == m.decl) {
                    if let Key::Partial(_, parent, fld) = key {
                        if *parent == m.path {
                            found |= !defs.is_empty();
                            let gran = Granularity::Partial(format!("Field:{}", g.name_str(*fld)));
                            edges.extend(defs.iter().map(|&def| (def, node, gran.clone())));
                        }
                    }
                }
                if !found {
                    // nothing known: the declaration of the field is its (default) value
                    if let Some(&last) = m.path.last() {
                        edges.push((last, node, Granularity::Full));
                    }
                }
            }
        }
        if kind == Kind::Return {
            edges.push((node, f, Granularity::Full));
        }
    }
    for (from, to, gran) in edges {
        g.add_dfg(from, to, gran);
    }
}

/// The chain of member accesses `member` ends, if it starts at a local variable and all fields are
/// resolved.
fn member_path(g: &Graph, member: u32, ref_of: &[u32], local: &dyn Fn(u32) -> bool) -> Option<MemberPath> {
    let mut nodes = vec![member];
    let mut path = Vec::new();
    let mut cur = member;
    loop {
        let field = ref_of[cur as usize];
        if field == NONE || g.nodes[field as usize].kind != Kind::Field {
            return None;
        }
        path.push(field);
        let base = g.nodes[cur as usize].first_child;
        if base == NONE {
            return None;
        }
        nodes.push(base);
        match g.nodes[base as usize].kind {
            Kind::Member => cur = base,
            Kind::Ref => {
                let decl = ref_of[base as usize];
                if !local(decl) {
                    return None;
                }
                nodes.reverse();
                path.reverse();
                return Some(MemberPath { decl, nodes, path });
            }
            _ => return None,
        }
    }
}

/// A write to `target` (the target of an assignment, or the operand of `++`/`--`), as a definition.
fn define(g: &Graph, target: u32, out: &mut State, ref_of: &[u32], local: &dyn Fn(u32) -> bool) {
    match g.nodes[target as usize].kind {
        Kind::Ref => {
            let d = ref_of[target as usize];
            if local(d) {
                // a whole write replaces everything known about the variable
                out.retain(|k, _| k.decl() != d);
                out.insert(Key::Whole(d), BTreeSet::from([target]));
            }
        }
        Kind::Member => {
            if let Some(m) = member_path(g, target, ref_of, local) {
                out.insert(Key::Field(m.decl, m.path.clone()), BTreeSet::from([target]));
                // every prefix of the path is a partial definition of its parent
                for i in 0..m.path.len() {
                    out.insert(Key::Partial(m.decl, m.path[..i].to_vec(), m.path[i]), BTreeSet::from([m.nodes[i]]));
                }
            }
        }
        _ => {}
    }
}

/// The state after a node.
fn transfer(g: &Graph, node: u32, state: &State, ref_of: &[u32], local: &dyn Fn(u32) -> bool) -> State {
    let mut out = state.clone();
    match g.nodes[node as usize].kind {
        Kind::Variable if local(node) => {
            out.retain(|k, _| k.decl() != node);
            out.insert(Key::Whole(node), BTreeSet::from([node]));
        }
        Kind::Assign => {
            let lhs = g.nodes[node as usize].first_child;
            if lhs != NONE {
                define(g, lhs, &mut out, ref_of, local);
            }
        }
        Kind::Unary if matches!(g.name_str(node), "++" | "--") => {
            let input = g.nodes[node as usize].first_child;
            if input != NONE {
                define(g, input, &mut out, ref_of, local);
            }
        }
        _ => {}
    }
    out
}

/// Merges `from` into `into` (union); returns whether `into` changed.
fn merge(into: &mut State, from: &State) -> bool {
    let mut changed = false;
    for (key, defs) in from {
        let slot = into.entry(key.clone()).or_default();
        for &d in defs {
            changed |= slot.insert(d);
        }
    }
    changed
}
