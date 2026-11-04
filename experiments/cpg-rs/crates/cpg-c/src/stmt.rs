use cpg_core::{Kind, NONE};
use rustc_hash::FxHashMap;
use tree_sitter::Node as Ts;

use crate::builder::Builder;

impl<'a> Builder<'a, '_> {
    pub(crate) fn scoped(&mut self, n: Ts, parent: u32, kind: Kind) {
        let node = self.add(parent, kind, &n, NONE);
        self.typedefs.push(FxHashMap::default());
        self.children(&n, node);
        self.typedefs.pop();
    }
}
