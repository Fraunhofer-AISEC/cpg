use cpg_core::{Kind, NONE};
use tree_sitter::Node as Ts;

use crate::builder::{text, Builder};

impl<'a> Builder<'a, '_> {
    /// Builds the left-hand side of a designated initializer: the last designator is the outermost
    /// node, and the implicit reference to the initialized variable is the innermost.
    pub(crate) fn designators(&mut self, ds: &[Ts], parent: u32) {
        let Some(d) = ds.last() else {
            self.add_implicit(parent, Kind::Ref, self.init_target);
            return;
        };
        let rest = &ds[..ds.len() - 1];
        match d.kind() {
            "field_designator" => {
                let field = d.named_child(0).map_or(NONE, |f| self.intern(text(self.g.src, &f)));
                let m = self.add(parent, Kind::Member, d, field);
                self.designators(rest, m);
            }
            "subscript_range_designator" => {
                let sub = self.add(parent, Kind::Subscript, d, NONE);
                self.designators(rest, sub);
                let r = self.add(sub, Kind::Range, d, NONE);
                self.children(d, r);
            }
            _ => {
                let sub = self.add(parent, Kind::Subscript, d, NONE);
                self.designators(rest, sub);
                self.children(d, sub);
            }
        }
    }

    pub(crate) fn reference(&mut self, n: Ts, parent: u32) {
        let sym = self.intern(text(self.g.src, &n));
        self.add(parent, Kind::Ref, &n, sym);
    }
}
