//! Access values (read, write, read-write) of expressions, like the Kotlin CPG derives them while the
//! frontend builds `Assign` and `UnaryOperator` nodes.
//!
//! - The target of an assignment is written (`=`) or read and written (compound operators); the
//!   target is the reference found by `unwrapReference`, i.e., through casts and `*`/`&`. The
//!   array of a subscription is not written. The nested member accesses a target is made of are
//!   read and written.
//! - The operand of `++` and `--` is read and written.
//! - A cast, a unary operator and an initializer list pass the access they get on to their operands.

use cpg_core::{Access, Graph, Kind, NONE};

/// C's compound assignment operators.
const COMPOUND: &[&str] = &["+=", "-=", "*=", "/=", "%=", "<<=", ">>=", "&=", "|=", "^="];

pub fn access(g: &mut Graph) {
    // The Kotlin frontend builds nodes bottom-up, and each rule fires when the node it belongs to is
    // built, so a later (outer) rule can overwrite an earlier (inner) one: the operand of a
    // dereference or address-of is set to `READ`, which a unary operator hands on, so the `++` of
    // `*p++` no longer writes `p`. Node ids are in pre-order, so this walks them backwards.
    for id in (0..g.nodes.len() as u32).rev() {
        match g.nodes[id as usize].kind {
            Kind::PointerRef | Kind::PointerDeref => {
                let input = g.nodes[id as usize].first_child;
                if input != NONE {
                    set(g, input, Access::Read);
                }
            }
            Kind::Assign => assign(g, id),
            Kind::Unary if matches!(g.name_str(id), "++" | "--") => {
                let input = g.nodes[id as usize].first_child;
                if input != NONE {
                    set(g, input, Access::ReadWrite);
                }
            }
            _ => {}
        }
    }
}

fn assign(g: &mut Graph, id: u32) {
    let lhs = g.nodes[id as usize].first_child;
    if lhs == NONE {
        return;
    }
    // the bases of a member access that is assigned to are read and written
    if g.nodes[lhs as usize].kind == Kind::Member {
        let mut base = g.nodes[lhs as usize].first_child;
        while base != NONE && g.nodes[base as usize].kind == Kind::Member {
            g.nodes[base as usize].access = Access::ReadWrite;
            base = g.nodes[base as usize].first_child;
        }
    }
    let op = g.name_str(id);
    let simple = op == "=";
    if g.nodes[lhs as usize].kind == Kind::Subscript {
        // the subscription itself is written; the array it indexes is not
        g.nodes[lhs as usize].access = if simple { Access::Write } else { Access::ReadWrite };
    } else if simple {
        if let Some(target) = unwrap_reference(g, lhs) {
            set(g, target, Access::Write);
        }
    } else if let Some(target) = unwrap_reference(g, lhs) {
        // compound (or something that is neither): read and written
        let _ = COMPOUND.contains(&op);
        set(g, target, Access::ReadWrite);
    }
}

/// `Expression.unwrapReference()`: the reference an expression stands for, through casts and the
/// `*` and `&` operators.
fn unwrap_reference(g: &Graph, mut id: u32) -> Option<u32> {
    loop {
        match g.nodes[id as usize].kind {
            Kind::PointerRef | Kind::PointerDeref | Kind::Ref | Kind::Member => return Some(id),
            Kind::Unary if matches!(g.name_str(id), "*" | "&") => {}
            Kind::Cast => {}
            _ => return None,
        }
        id = g.nodes[id as usize].first_child;
        if id == NONE {
            return None;
        }
    }
}

/// Sets the access of a node; casts, unary operators and initializer lists hand it on to their
/// operands (the setters of the Kotlin classes).
fn set(g: &mut Graph, id: u32, value: Access) {
    g.nodes[id as usize].access = value;
    if matches!(g.nodes[id as usize].kind, Kind::Cast | Kind::Unary | Kind::InitList) {
        let kids: Vec<u32> = g.children(id).collect();
        for k in kids {
            set(g, k, value);
        }
    }
}
