//! Expression types and member resolution: a bottom-up pass over the resolved graph, following the
//! Kotlin CPG (`Reference` takes the type of its declaration, `Call` the return type of what it
//! invokes, ...). Runs after `resolve`.

use cpg_core::{Graph, Kind, NONE};
use rustc_hash::FxHashMap;

use crate::numeric;

const UNKNOWN: &str = "UNKNOWN";

pub fn typing(g: &mut Graph) {
    let n = g.nodes.len();
    let mut ref_of = vec![NONE; n];
    for &(r, d) in &g.refers_to {
        ref_of[r as usize] = d;
    }
    // calls are resolved here, since viability depends on the types of the arguments
    g.invokes.clear();
    let mut invoked = vec![NONE; n];
    let mut functions: FxHashMap<String, Vec<u32>> = FxHashMap::default();
    for id in 0..n as u32 {
        if g.nodes[id as usize].kind == Kind::Function {
            functions.entry(g.name_str(id).to_string()).or_default().push(id);
        }
    }
    // records by name, to resolve members
    let mut records: FxHashMap<String, u32> = FxHashMap::default();
    for id in 0..n as u32 {
        if g.nodes[id as usize].kind == Kind::Record {
            records.entry(g.name_str(id).to_string()).or_insert(id);
        }
    }

    // post-order: children before their parent
    let mut stack: Vec<(u32, bool)> = vec![(0, false)];
    while let Some((id, exit)) = stack.pop() {
        if !exit {
            stack.push((id, true));
            let kids: Vec<u32> = g.children(id).collect();
            stack.extend(kids.iter().rev().map(|&k| (k, false)));
            continue;
        }
        let kind = g.nodes[id as usize].kind;
        let first = g.nodes[id as usize].first_child;
        let first_ty = |g: &Graph| -> String {
            if first == NONE { UNKNOWN.into() } else { g.type_str(first).unwrap_or(UNKNOWN).to_string() }
        };

        let ty: Option<String> = match kind {
            // these keep what the frontend gave them, or have no type
            Kind::Unit | Kind::Record | Kind::Typedef | Kind::Enum => None,
            Kind::Variable => {
                // an initializer list has the type of the variable
                let vt = g.type_str(id).map(str::to_string);
                let lists: Vec<u32> =
                    g.children(id).filter(|&c| g.nodes[c as usize].kind == Kind::InitList).collect();
                for l in lists {
                    if let Some(t) = &vt {
                        g.set_type(l, t);
                    }
                }
                None
            }
            Kind::Param | Kind::Field | Kind::Function | Kind::Enumerator | Kind::Constructor
            | Kind::Literal | Kind::Cast | Kind::TypeReference => None,
            Kind::Ref => {
                let d = ref_of[id as usize];
                Some(match d {
                    NONE => UNKNOWN.to_string(),
                    d => match g.nodes[d as usize].kind {
                        Kind::Function | Kind::Variable | Kind::Param | Kind::Field | Kind::Enumerator => {
                            g.type_str(d).unwrap_or(UNKNOWN).to_string()
                        }
                        _ => UNKNOWN.to_string(),
                    },
                })
            }
            Kind::PointerRef | Kind::PointerDeref => {
                // refers to what the inner reference refers to
                if first != NONE && ref_of[first as usize] != NONE {
                    let d = ref_of[first as usize];
                    ref_of[id as usize] = d;
                }
                let inner = first_ty(g);
                Some(if kind == Kind::PointerRef {
                    if inner.starts_with('(') { String::new() } else if inner == UNKNOWN { UNKNOWN.into() } else { format!("{inner}*") }
                } else if let Some(s) = inner.strip_suffix('*') {
                    s.to_string()
                } else if inner.is_empty() {
                    String::new()
                } else {
                    UNKNOWN.into()
                })
            }
            Kind::Member => {
                let field = g.name_str(id).to_string();
                let base = first_ty(g);
                let rec = base.trim_end_matches('*');
                if base != UNKNOWN && !rec.is_empty() {
                    let qualified = format!("{rec}::{field}");
                    let q = g.add_name(qualified.clone().into_bytes());
                    g.nodes[id as usize].name = q;
                    let found = records.get(rec).and_then(|&r| {
                        g.children(r).find(|&c| {
                            g.nodes[c as usize].kind == Kind::Field && g.name_str(c) == qualified
                        })
                    });
                    match found {
                        Some(f) => {
                            ref_of[id as usize] = f;
                            Some(g.type_str(f).unwrap_or(UNKNOWN).to_string())
                        }
                        None => Some(UNKNOWN.into()),
                    }
                } else {
                    Some(UNKNOWN.into())
                }
            }
            Kind::Subscript => {
                let base = first_ty(g);
                let has_range = g.children(id).any(|c| g.nodes[c as usize].kind == Kind::Range);
                Some(if has_range {
                    base
                } else if let Some(s) = base.strip_suffix("[]") {
                    s.to_string()
                } else if let Some(s) = base.strip_suffix('*') {
                    s.to_string()
                } else {
                    UNKNOWN.into()
                })
            }
            Kind::Call => {
                let callee = g.nodes[id as usize].last_child;
                let kids: Vec<u32> = g.children(id).collect();
                let args: Vec<String> = kids[..kids.len().saturating_sub(1)]
                    .iter()
                    .map(|&a| g.type_str(a).unwrap_or(UNKNOWN).to_string())
                    .collect();
                if callee != NONE && g.nodes[callee as usize].kind == Kind::Ref && g.eog_reached(callee) {
                    let cur = ref_of[callee as usize];
                    if cur == NONE || g.nodes[cur as usize].kind == Kind::Function {
                        let name = g.name_str(callee).to_string();
                        let set = pick_targets(g, functions.get(&name), &args);
                        if set.is_empty() && cur != NONE {
                            // an unresolvable call leaves its callee unresolved as well
                            ref_of[callee as usize] = NONE;
                            g.set_type(callee, UNKNOWN);
                        }
                        if let Some(&target) = set
                            .iter()
                            .find(|&&f| has_body(g, f))
                            .or_else(|| set.first())
                        {
                            ref_of[callee as usize] = target;
                            let fty = g.type_str(target).unwrap_or(UNKNOWN).to_string();
                            g.set_type(callee, &fty);
                            for &f in &set {
                                g.invokes.push((id, f));
                            }
                            invoked[id as usize] = target;
                        }
                    }
                }
                Some(match invoked[id as usize] {
                    NONE => UNKNOWN.to_string(),
                    f => g
                        .type_str(f)
                        .and_then(numeric::return_type)
                        .map_or(UNKNOWN.to_string(), str::to_string),
                })
            }
            Kind::Binary | Kind::ShortCircuit => {
                let kids: Vec<u32> = g.children(id).collect();
                if kids.len() == 2 {
                    let (l, r) = (g.type_str(kids[0]).unwrap_or(UNKNOWN), g.type_str(kids[1]).unwrap_or(UNKNOWN));
                    Some(numeric::binary(g.name_str(id), l, r))
                } else {
                    Some(UNKNOWN.into())
                }
            }
            Kind::Unary => Some(first_ty(g)),
            Kind::If | Kind::Conditional => {
                // the type of the branches if they agree (a lone `then` is the type of the if)
                let kids: Vec<u32> = g.children(id).collect();
                let t = |i: usize| kids.get(i).map(|&k| g.type_str(k).unwrap_or(UNKNOWN).to_string());
                let (then, other) = (t(1), t(2));
                Some(match (then, other) {
                    (Some(a), None) => a,
                    (Some(a), Some(b)) if a == b => a,
                    _ => UNKNOWN.into(),
                })
            }
            // statements, blocks, assignments, problems, ... have the unknown type
            _ => Some(UNKNOWN.into()),
        };
        if let Some(t) = ty {
            // the initializer list of a variable was typed with the variable, keep that
            if !(kind == Kind::InitList && g.type_str(id).is_some()) {
                g.set_type(id, &t);
            }
        }
    }
    g.refers_to =
        (0..n as u32).filter(|&i| ref_of[i as usize] != NONE).map(|i| (i, ref_of[i as usize])).collect();
}

fn has_body(g: &Graph, f: u32) -> bool {
    let c = g.nodes[f as usize].first_child;
    c != NONE && g.nodes[c as usize].kind == Kind::Block
}

/// The functions a call with the given argument types can mean (`CLanguage.bestViableResolution`):
/// a single candidate is the target whatever the arguments are; with several, the viable ones.
fn pick_targets(g: &Graph, candidates: Option<&Vec<u32>>, args: &[String]) -> Vec<u32> {
    let Some(cands) = candidates else { return Vec::new() };
    if cands.len() == 1 {
        return cands.clone();
    }
    cands
        .iter()
        .copied()
        .filter(|&f| {
            let params: Vec<&str> = g
                .children(f)
                .filter(|&c| g.nodes[c as usize].kind == Kind::Param)
                .map(|c| g.type_str(c).unwrap_or(UNKNOWN))
                .collect();
            params.len() == args.len() && params.iter().zip(args).all(|(p, a)| castable(a, p))
        })
        .collect()
}

/// Whether an argument of type `arg` can implicitly be passed as a parameter of type `param`.
fn castable(arg: &str, param: &str) -> bool {
    if arg == param {
        return true;
    }
    if arg == UNKNOWN || param == UNKNOWN {
        return false;
    }
    if numeric::numeric(arg).is_some() && numeric::numeric(param).is_some() {
        return true;
    }
    let root = |t: &str| t.trim_end_matches(|c| c == '*' || c == '[' || c == ']').to_string();
    let is_ptr = |t: &str| t.ends_with('*') || t.ends_with("[]");
    if is_ptr(arg) && is_ptr(param) {
        // same element type (pointer and array are interchangeable), or anything to/from void*
        return root(arg) == root(param) || param == "void*" || arg == "void*";
    }
    false
}
