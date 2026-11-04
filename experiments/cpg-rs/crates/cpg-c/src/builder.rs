use cpg_core::{Graph, Kind, Node, NONE};
use rustc_hash::FxHashMap;
use tree_sitter::Node as Ts;

pub(crate) struct Builder<'a, 's> {
    pub(crate) g: Graph<'a>,
    /// Macro expansion spans: nodes starting or ending inside one get the range of the macro use.
    pub(crate) spans: &'s [(u32, u32)],
    pub(crate) sym: FxHashMap<&'a [u8], u32>,
    /// One-shot location for the next node (an expression statement's range goes to its
    /// expression, like in the Kotlin frontend).
    pub(crate) loc_override: Option<(u32, u32)>,
    /// Start of the enclosing declaration, which the Kotlin frontend uses for a record's range.
    pub(crate) decl_start: Option<u32>,
    /// End of the last declarator of the enclosing typedef (used for an enumeration's range).
    pub(crate) decl_end: Option<u32>,
    /// Name of the variable whose initializer is being translated (root of designators).
    pub(crate) init_target: u32,
    /// Typedefs visible per scope (name -> resolved type name); the Kotlin frontend resolves them
    /// while translating.
    pub(crate) typedefs: Vec<FxHashMap<Vec<u8>, String>>,
}

pub(crate) fn text<'a>(src: &'a [u8], n: &Ts) -> &'a [u8] {
    &src[n.start_byte()..n.end_byte()]
}

impl<'a> Builder<'a, '_> {
    pub(crate) fn intern(&mut self, s: &'a [u8]) -> u32 {
        if let Some(&i) = self.sym.get(s) {
            return i;
        }
        let i = self.g.names.len() as u32;
        self.g.names.push(std::borrow::Cow::Borrowed(s));
        self.sym.insert(s, i);
        i
    }

    pub(crate) fn intern_owned(&mut self, s: Vec<u8>) -> u32 {
        let i = self.g.names.len() as u32;
        self.g.names.push(std::borrow::Cow::Owned(s));
        i
    }

    pub(crate) fn set_ty(&mut self, id: u32, ty: &str) {
        self.g.set_type(id, ty);
    }

    /// A node without source location (implicit in the Kotlin CPG).
    pub(crate) fn add_implicit(&mut self, parent: u32, kind: Kind, name: u32) -> u32 {
        let id = self.g.nodes.len() as u32;
        self.g.nodes.push(Node {
            kind, ptr: 0, name, ty: NONE, start: NONE, end: NONE, ty_start: 0, ty_end: 0,
            first_child: NONE, last_child: NONE, next: NONE,
        });
        self.link(parent, id);
        id
    }

    pub(crate) fn link(&mut self, parent: u32, id: u32) {
        if parent != NONE {
            let last = self.g.nodes[parent as usize].last_child;
            if last == NONE {
                self.g.nodes[parent as usize].first_child = id;
            } else {
                self.g.nodes[last as usize].next = id;
            }
            self.g.nodes[parent as usize].last_child = id;
        }
    }

    pub(crate) fn add(&mut self, parent: u32, kind: Kind, n: &Ts, name: u32) -> u32 {
        let (start, end) = self.loc_override.take().unwrap_or_else(|| {
            cpg_pp::clamp_to_spans(self.spans, n.start_byte() as u32, n.end_byte() as u32)
        });
        let id = self.g.nodes.len() as u32;
        self.g.nodes.push(Node {
            kind, ptr: 0, name, ty: NONE, start, end,
            ty_start: 0, ty_end: 0, first_child: NONE, last_child: NONE, next: NONE,
        });
        self.link(parent, id);
        id
    }

    pub(crate) fn children(&mut self, n: &Ts, parent: u32) {
        let mut c = n.walk();
        for ch in n.named_children(&mut c) {
            self.tr(ch, parent);
        }
    }

    pub(crate) fn typed(&mut self, id: u32, ty: Option<Ts>, ptr: u8) {
        if let Some(t) = ty {
            let n = &mut self.g.nodes[id as usize];
            n.ty_start = t.start_byte() as u32;
            n.ty_end = t.end_byte() as u32;
        }
        self.g.nodes[id as usize].ptr = ptr;
    }

    /// The name of a reference-like node (a reference or a pointer reference/dereference of one).
    pub(crate) fn name_of_child_node(&self, c: u32) -> u32 {
        match self.g.nodes[c as usize].kind {
            Kind::Ref | Kind::PointerRef | Kind::PointerDeref => self.g.nodes[c as usize].name,
            _ => NONE,
        }
    }

    /// The name of the first child, whatever it is (a pointer reference or dereference is named
    /// after its operand, e.g. `++` for `*p++`).
    pub(crate) fn name_of_child(&self, node: u32) -> u32 {
        let c = self.g.nodes[node as usize].first_child;
        if c == NONE { NONE } else { self.g.nodes[c as usize].name }
    }
}
