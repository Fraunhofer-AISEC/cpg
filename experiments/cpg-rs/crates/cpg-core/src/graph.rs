use rustc_hash::FxHashMap;

use crate::Kind;

pub const NONE: u32 = u32::MAX;

/// How an expression is used: read, written, or both (see `AccessValues` of the Kotlin CPG).
#[derive(Clone, Copy, PartialEq, Eq, Debug, Default)]
pub enum Access {
    #[default]
    Read,
    Write,
    ReadWrite,
}

#[derive(Clone, Copy)]
pub struct Node {
    pub kind: Kind,
    pub ptr: u8,
    pub access: Access,
    pub name: u32,
    /// Index into `Graph::names` of the type name (NONE = node has no type).
    pub ty: u32,
    pub start: u32,
    pub end: u32,
    pub ty_start: u32,
    pub ty_end: u32,
    pub first_child: u32,
    pub last_child: u32,
    pub next: u32,
}

/// An evaluation order edge.
#[derive(Clone, Copy, Debug)]
pub struct EogEdge {
    pub from: u32,
    pub to: u32,
    /// The branch (`true`/`false`) the edge belongs to, if it leaves a condition.
    pub branch: Option<bool>,
    pub unreachable: bool,
}

pub struct Graph<'a> {
    pub src: &'a [u8],
    pub nodes: Vec<Node>,
    pub names: Vec<std::borrow::Cow<'a, [u8]>>,
    /// `(reference, declaration)` edges.
    pub refers_to: Vec<(u32, u32)>,
    /// `(call, function)` edges.
    pub invokes: Vec<(u32, u32)>,
    /// Evaluation order edges, in creation order (which is the order per source node).
    pub eog: Vec<EogEdge>,
    /// Nodes the EOG reaches from its starters (the nodes the Kotlin `SymbolResolver` visits).
    /// Empty if the EOG pass has not run, which means everything counts as reachable.
    pub eog_visited: Vec<bool>,
    pub unresolved: usize,
    pub unresolved_syms: Vec<u32>,
    type_ids: FxHashMap<String, u32>,
    /// Code of implicit nodes that have no source range (e.g. the `true` of `for (;;)`).
    pub code_override: FxHashMap<u32, String>,
}

impl<'a> Graph<'a> {
    pub fn new(src: &'a [u8]) -> Self {
        Graph {
            src,
            nodes: Vec::with_capacity(1 << 20),
            names: Vec::new(),
            refers_to: Vec::new(),
            invokes: Vec::new(),
            eog: Vec::new(),
            eog_visited: Vec::new(),
            unresolved: 0,
            unresolved_syms: Vec::new(),
            type_ids: FxHashMap::default(),
            code_override: FxHashMap::default(),
        }
    }

    /// Adds an owned string (a name or type name) and returns its index.
    pub fn add_name(&mut self, s: Vec<u8>) -> u32 {
        self.names.push(std::borrow::Cow::Owned(s));
        (self.names.len() - 1) as u32
    }

    /// Sets the type name of a node (interned).
    pub fn set_type(&mut self, id: u32, ty: &str) {
        let t = match self.type_ids.get(ty) {
            Some(&t) => t,
            None => {
                let t = self.add_name(ty.as_bytes().to_vec());
                self.type_ids.insert(ty.to_string(), t);
                t
            }
        };
        self.nodes[id as usize].ty = t;
    }

    /// The type name of a node, if it has one.
    pub fn type_str(&self, id: u32) -> Option<&str> {
        let t = self.nodes[id as usize].ty;
        (t != NONE).then(|| std::str::from_utf8(&self.names[t as usize]).unwrap_or(""))
    }

    /// Whether the EOG pass reached this node (always true if the pass has not run).
    pub fn eog_reached(&self, id: u32) -> bool {
        self.eog_visited.is_empty() || self.eog_visited[id as usize]
    }

    /// The name of a node (empty if it has none).
    pub fn name_str(&self, id: u32) -> &str {
        let n = self.nodes[id as usize].name;
        if n == NONE { "" } else { std::str::from_utf8(&self.names[n as usize]).unwrap_or("") }
    }

    /// The AST children of a node, in order.
    pub fn children(&self, id: u32) -> impl Iterator<Item = u32> + '_ {
        let mut c = self.nodes[id as usize].first_child;
        std::iter::from_fn(move || {
            if c == NONE {
                return None;
            }
            let cur = c;
            c = self.nodes[cur as usize].next;
            Some(cur)
        })
    }
}
