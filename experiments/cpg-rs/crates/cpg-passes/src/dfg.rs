//! The data flow graph without control flow, ported from the Kotlin `DFGPass` for C.
//!
//! The rules are local to a node and its operands (operands flow into operators, arguments into
//! parameters and calls, an assigned value into its target). References are connected to their
//! declaration: all of them if `flow_sensitive_references` is false; otherwise only the points-to
//! pass does that (apart from global variables, which the Kotlin pass still handles here).

use cpg_core::{Access, Granularity, Graph, Kind, NONE};

/// C's compound assignment operators.
const COMPOUND: &[&str] = &["+=", "-=", "*=", "/=", "%=", "<<=", ">>=", "&=", "|=", "^="];

pub fn dfg(g: &mut Graph, flow_sensitive_references: bool) {
    // references by node, to find declarations
    let mut ref_of = vec![NONE; g.nodes.len()];
    for &(r, d) in &g.refers_to {
        ref_of[r as usize] = d;
    }
    let mut parent = vec![NONE; g.nodes.len()];
    for id in 0..g.nodes.len() as u32 {
        let kids: Vec<u32> = g.children(id).collect();
        for k in kids {
            parent[k as usize] = id;
        }
    }
    for id in 0..g.nodes.len() as u32 {
        let kids: Vec<u32> = g.children(id).collect();
        let kind = g.nodes[id as usize].kind;
        let access = g.nodes[id as usize].access;
        let full = Granularity::Full;
        match kind {
            Kind::Call if !flow_sensitive_references => call(g, id, &kids),
            // with the points-to pass, only the edge from the invoked functions (leaving the call)
            Kind::Call => {
                let invoked: Vec<u32> = g.invokes.iter().filter(|e| e.0 == id).map(|e| e.1).collect();
                if invoked.is_empty() {
                    for &a in kids.split_last().map_or(&[][..], |(_, a)| a) {
                        g.add_dfg(a, id, Granularity::Full);
                    }
                }
                for f in invoked {
                    g.add_dfg_ext(f, id, Granularity::Full, false, vec![(false, vec![id])]);
                }
            }
            Kind::Cast => {
                if let Some(&e) = kids.first() {
                    g.add_dfg(e, id, full);
                }
            }
            Kind::Binary | Kind::ShortCircuit => {
                for &k in kids.iter().take(2) {
                    g.add_dfg(k, id, Granularity::Full);
                }
            }
            Kind::Assign => {
                let (Some(&lhs), Some(&rhs)) = (kids.first(), kids.get(1)) else { continue };
                if COMPOUND.contains(&g.name_str(id)) {
                    g.add_dfg(lhs, id, Granularity::Full);
                    g.add_dfg(id, lhs, Granularity::Full);
                    g.add_dfg(rhs, id, Granularity::Full);
                } else {
                    g.add_dfg(rhs, lhs, Granularity::Full);
                }
            }
            Kind::Member => {
                let Some(&base) = kids.first() else { continue };
                let field = ref_of[id as usize];
                let gran = if field != NONE && g.nodes[field as usize].kind == Kind::Field {
                    Granularity::Field(g.name_str(field).to_string())
                } else {
                    Granularity::Full
                };
                match access {
                    Access::Write | Access::ReadWrite => g.add_dfg(id, base, gran),
                    Access::Read => g.add_dfg(base, id, gran),
                }
            }
            Kind::PointerRef | Kind::PointerDeref => {
                if let Some(&input) = kids.first() {
                    let what = if kind == Kind::PointerRef { "PointerReference" } else { "PointerDereference" };
                    g.add_dfg(input, id, Granularity::Partial(what.to_string()));
                }
            }
            Kind::Ref => {
                let decl = ref_of[id as usize];
                if decl != NONE && (!flow_sensitive_references || is_global(g, decl, &parent)) {
                    match access {
                        Access::Write => g.add_dfg(id, decl, full),
                        Access::Read => g.add_dfg(decl, id, full),
                        Access::ReadWrite => {
                            g.add_dfg(id, decl, Granularity::Full);
                            g.add_dfg(decl, id, Granularity::Full);
                        }
                    }
                }
            }
            Kind::ExprList => {
                if let Some(&last) = kids.last() {
                    g.add_dfg(last, id, full);
                }
            }
            Kind::InitList => {
                let p = parent[id as usize];
                // the target of an assignment: the data flows from the list into the initializers
                let is_target = p != NONE
                    && g.nodes[p as usize].kind == Kind::Assign
                    && g.nodes[p as usize].first_child == id;
                for (idx, &init) in kids.iter().enumerate() {
                    let gran = Granularity::Indexed(idx.to_string());
                    if is_target {
                        g.add_dfg(id, init, gran);
                    } else {
                        g.add_dfg(init, id, gran);
                    }
                }
            }
            Kind::Unary => {
                let Some(&input) = kids.first() else { continue };
                let input_is_written_ref =
                    g.nodes[input as usize].kind == Kind::Ref && g.nodes[input as usize].access == Access::Write;
                if input_is_written_ref {
                    g.add_dfg(id, input, full);
                } else {
                    g.add_dfg(input, id, Granularity::Full);
                    if matches!(g.name_str(id), "++" | "--") {
                        g.add_dfg(id, input, Granularity::Full);
                    }
                }
            }
            Kind::Conditional => {
                for &k in kids.iter().skip(1).take(2) {
                    g.add_dfg(k, id, Granularity::Full);
                }
            }
            Kind::Subscript => {
                let (Some(&array), Some(&index)) = (kids.first(), kids.get(1)) else { continue };
                let gran = match literal_number(g, index) {
                    Some(n) => Granularity::Indexed(n),
                    None => Granularity::Partial(format!("{}:{}", g.nodes[index as usize].kind.class_name(), g.name_str(index))),
                };
                if access == Access::Write {
                    g.add_dfg(id, array, gran);
                } else {
                    g.add_dfg(array, id, gran);
                }
            }
            // the initializer (also an array construction) flows into the variable
            Kind::Variable | Kind::Field => {
                if let Some(&init) = kids.first() {
                    g.add_dfg(init, id, full);
                }
            }
            Kind::Return => {
                for &k in &kids {
                    g.add_dfg(k, id, Granularity::Full);
                }
            }
            Kind::DoWhile => {
                if let Some(&cond) = kids.get(1) {
                    g.add_dfg(cond, id, full);
                }
            }
            _ => {}
        }
    }
}

/// `handleCall`: arguments flow into an unresolved call; for resolved calls into the parameters
/// (without the points-to pass) and the function flows into the call.
fn call(g: &mut Graph, id: u32, kids: &[u32]) {
    let Some((_callee, args)) = kids.split_last().map(|(c, a)| (*c, a)) else { return };
    let invoked: Vec<u32> = g.invokes.iter().filter(|e| e.0 == id).map(|e| e.1).collect();
    if invoked.is_empty() {
        for &a in args {
            g.add_dfg(a, id, Granularity::Full);
        }
        return;
    }
    for f in invoked {
        let params: Vec<u32> =
            g.children(f).filter(|&c| g.nodes[c as usize].kind == Kind::Param).collect();
        for (param, &arg) in params.iter().zip(args) {
            g.add_dfg(arg, *param, Granularity::Full);
        }
        g.add_dfg(f, id, Granularity::Full);
    }
}

/// A global variable (or a function): declared directly in the translation unit.
fn is_global(g: &Graph, decl: u32, parent: &[u32]) -> bool {
    match g.nodes[decl as usize].kind {
        Kind::Function => true,
        Kind::Variable => parent[decl as usize] == 0,
        _ => false,
    }
}

/// The value of an integer literal (decimal, hex, octal), as the Kotlin number is printed.
fn literal_number(g: &Graph, id: u32) -> Option<String> {
    let n = g.nodes[id as usize];
    if n.kind != Kind::Literal || n.start == NONE {
        return None;
    }
    let t = String::from_utf8_lossy(&g.src[n.start as usize..n.end as usize]).to_lowercase();
    if t.contains('.') || t.starts_with('\'') || t.starts_with('"') {
        return None;
    }
    let digits = t.trim_end_matches(|c| c == 'u' || c == 'l');
    let v = if let Some(h) = digits.strip_prefix("0x") {
        u128::from_str_radix(h, 16).ok()?
    } else if digits.len() > 1 && digits.starts_with('0') {
        u128::from_str_radix(&digits[1..], 8).ok()?
    } else {
        digits.parse::<u128>().ok()?
    };
    Some(v.to_string())
}
