use rustc_hash::{FxHashMap, FxHashSet};

use crate::Kind;

pub const NONE: u32 = u32::MAX;

/// Ids from here on are synthetic nodes (`Graph::synth`), not nodes of the AST.
pub const SYNTH_BASE: u32 = 0x8000_0000;

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

/// The granularity of a data flow edge (the `Granularity` of the Kotlin CPG).
#[derive(Clone, PartialEq, Eq, Hash, Debug)]
pub enum Granularity {
    Full,
    /// `ADDRESS`, `CURRENT_DEREF_VALUE`, `CURRENT_DEREF_DEREF_VALUE`
    Pointer(&'static str),
    /// A field, by its (qualified) name.
    Field(String),
    /// An index, as the number is printed.
    Indexed(String),
    StringIndexed(String),
    /// Some other part, e.g. `PointerReference`.
    Partial(String),
}

impl Granularity {
    /// The suffix of an edge in the dump.
    pub fn tag(&self) -> String {
        match self {
            Granularity::Full => String::new(),
            Granularity::Pointer(p) => format!(":P({p})"),
            Granularity::Field(f) => format!(":F({f})"),
            Granularity::Indexed(i) => format!(":I({i})"),
            Granularity::StringIndexed(s) => format!(":S({s})"),
            Granularity::Partial(p) => format!(":Part({p})"),
        }
    }
}

/// A data flow edge from `from` to `to`.
#[derive(Clone, PartialEq, Eq, Debug)]
pub struct DfgEdge {
    pub from: u32,
    pub to: u32,
    pub granularity: Granularity,
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
    /// Data flow edges, in creation order.
    pub dfg: Vec<DfgEdge>,
    dfg_seen: FxHashSet<(u32, u32, Granularity)>,
    /// Nodes the EOG reaches from its starters (the nodes the Kotlin `SymbolResolver` visits).
    /// Empty if the EOG pass has not run, which means everything counts as reachable.
    pub eog_visited: Vec<bool>,
    pub unresolved: usize,
    pub unresolved_syms: Vec<u32>,
    type_ids: FxHashMap<String, u32>,
    /// Code of implicit nodes that have no source range (e.g. the `true` of `for (;;)`).
    pub code_override: FxHashMap<u32, String>,
    /// Nodes the analyses create that are not part of the AST (`MemoryAddress`, ...): the simple
    /// class name and the name. Their id is `SYNTH_BASE + index`.
    pub synth: Vec<(&'static str, String)>,
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
            dfg: Vec::new(),
            dfg_seen: FxHashSet::default(),
            eog_visited: Vec::new(),
            unresolved: 0,
            unresolved_syms: Vec::new(),
            type_ids: FxHashMap::default(),
            code_override: FxHashMap::default(),
            synth: Vec::new(),
        }
    }

    /// Adds a data flow edge `from -> to` unless the same edge exists.
    pub fn add_dfg(&mut self, from: u32, to: u32, granularity: Granularity) {
        if self.dfg_seen.insert((from, to, granularity.clone())) {
            self.dfg.push(DfgEdge { from, to, granularity });
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
