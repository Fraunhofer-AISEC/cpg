//! The tree-sitter-c frontend: translates a C syntax tree into a `cpg_core::Graph`, following the
//! Kotlin C frontend (including its quirks), split by concern like the Kotlin handlers.

mod builder;
mod decl;
mod expr;
mod stmt;
mod translate;
mod types;

use std::cell::RefCell;

use builder::Builder;
use cpg_core::{Graph, Kind, NONE};
use rustc_hash::FxHashMap;
use tree_sitter::{Parser, Tree};

thread_local! {
    static PARSER: RefCell<Parser> = RefCell::new({
        let mut p = Parser::new();
        p.set_language(&tree_sitter_c::LANGUAGE.into()).expect("language");
        p
    });
}

/// Parses C source (already preprocessed by `cpg-pp`) with tree-sitter-c.
pub fn parse(src: &[u8]) -> Tree {
    PARSER.with(|p| p.borrow_mut().parse(src, None).expect("parse"))
}

/// Pure translation: AST edges, names and type ranges. Name resolution is the separate `resolve`.
pub fn build<'a>(src: &'a [u8], tree: &Tree, spans: &[(u32, u32)]) -> Graph<'a> {
    let mut b = Builder {
        g: Graph::new(src),
        sym: FxHashMap::default(),
        spans,
        loc_override: None,
        decl_start: None,
        decl_end: None,
        init_target: NONE,
        typedefs: vec![FxHashMap::default()],
    };
    b.tr(tree.root_node(), NONE);
    link_gotos(&mut b.g);
    if let Some(root) = b.g.nodes.first_mut() {
        root.end = src.len() as u32;
    }
    b.g
}

/// Of several gotos that jump forward to the same label, only the last one is linked to it in the
/// Kotlin CPG; the earlier ones keep neither the label's name nor an EOG edge.
fn link_gotos(g: &mut Graph) {
    use std::collections::HashMap;
    let mut labels: HashMap<(u32, String), u32> = HashMap::new();
    let mut gotos: Vec<(u32, u32)> = Vec::new();
    let mut stack = vec![(0u32, NONE)];
    while let Some((id, f)) = stack.pop() {
        let kind = g.nodes[id as usize].kind;
        let f = if matches!(kind, Kind::Function | Kind::Constructor) { id } else { f };
        match kind {
            Kind::Label => {
                labels.insert((f, g.name_str(id).to_string()), id);
            }
            Kind::Goto => gotos.push((id, f)),
            _ => {}
        }
        let kids: Vec<u32> = g.children(id).collect();
        stack.extend(kids.into_iter().map(|k| (k, f)));
    }
    for ((f, name), label) in labels {
        let mut forward: Vec<u32> = gotos
            .iter()
            .filter(|&&(gt, gf)| gf == f && gt < label && g.name_str(gt) == name)
            .map(|&(gt, _)| gt)
            .collect();
        forward.sort_unstable();
        forward.pop();
        for gt in forward {
            g.nodes[gt as usize].name = NONE;
        }
    }
}
