use cpg_core::{Kind, NONE};
use rustc_hash::FxHashMap;
use tree_sitter::Node as Ts;

use crate::builder::{text, Builder};

impl<'a> Builder<'a, '_> {
    /// Unwinds a declarator chain; returns (identifier, pointer depth, first function_declarator).
    pub(crate) fn unwind<'t>(&self, mut d: Ts<'t>) -> (Option<Ts<'t>>, u8, Option<Ts<'t>>) {
        let (mut ptr, mut func) = (0u8, None);
        loop {
            match d.kind() {
                "identifier" | "field_identifier" | "type_identifier" | "primitive_type" => {
                    return (Some(d), ptr, func)
                }
                "pointer_declarator" => ptr += 1,
                "function_declarator" => {
                    if func.is_none() {
                        func = Some(d)
                    }
                }
                "array_declarator" | "parenthesized_declarator" | "attributed_declarator"
                | "init_declarator" => {}
                _ => return (None, ptr, func),
            }
            match d.child_by_field_name("declarator").or_else(|| d.named_child(0)) {
                Some(n) => d = n,
                None => return (None, ptr, func),
            }
        }
    }

    pub(crate) fn params(&mut self, f: Ts, func: u32) {
        let Some(list) = f.child_by_field_name("parameters") else { return };
        let mut c = list.walk();
        for p in list.named_children(&mut c) {
            if p.kind() != "parameter_declaration" {
                continue;
            }
            let ty = p.child_by_field_name("type");
            if p.child_by_field_name("declarator").is_none()
                && ty.is_some_and(|t| text(self.g.src, &t) == b"void")
            {
                continue; // `f(void)` has no parameters
            }
            let (id, ptr, _) = match p.child_by_field_name("declarator") {
                Some(d) => self.unwind(d),
                None => (None, 0, None),
            };
            let name = id.map_or(NONE, |i| self.intern(text(self.g.src, &i)));
            let node = self.add(func, Kind::Param, &p, name);
            self.typed(node, ty, ptr);
            let base = self.base_type(ty);
            let pty = match p.child_by_field_name("declarator") {
                Some(d) => self.declarator_type(d, base).0,
                None => base,
            };
            self.set_ty(node, &pty);
        }
    }

    pub(crate) fn function(&mut self, n: Ts, parent: u32) {
        let ty = n.child_by_field_name("type");
        let (id, ptr, f) = match n.child_by_field_name("declarator") {
            Some(d) => self.unwind(d),
            None => (None, 0, None),
        };
        let name = id.map_or(NONE, |i| self.intern(text(self.g.src, &i)));
        let node = self.add(parent, Kind::Function, &n, name);
        self.typed(node, ty, ptr);
        if let Some(d) = n.child_by_field_name("declarator") {
            let base = self.base_type(ty);
            let (t, _) = self.declarator_type(d, base);
            self.set_ty(node, &t);
        }
        self.typedefs.push(FxHashMap::default());
        // the Kotlin CPG lists the body before the parameters
        if let Some(b) = n.child_by_field_name("body") {
            self.tr(b, node);
            // ... and appends an implicit return to every function body
            let body = self.g.nodes[node as usize].last_child;
            if body != NONE && self.g.nodes[body as usize].kind == Kind::Block {
                let last = self.g.nodes[body as usize].last_child;
                if last == NONE || self.g.nodes[last as usize].kind != Kind::Return {
                    self.add_implicit(body, Kind::Return, NONE);
                }
            }
        }
        if let Some(f) = f {
            self.params(f, node);
        }
        self.typedefs.pop();
    }

    pub(crate) fn declaration(&mut self, n: Ts, parent: u32) {
        let local = parent != NONE
            && matches!(self.g.nodes[parent as usize].kind, Kind::Block | Kind::For);
        let parent = if local { self.add(parent, Kind::DeclStmt, &n, NONE) } else { parent };
        let ty = n.child_by_field_name("type");
        if let Some(t) = ty {
            if matches!(t.kind(), "struct_specifier" | "union_specifier" | "enum_specifier") {
                self.decl_start = Some(n.start_byte() as u32);
                self.tr(t, parent);
                self.decl_start = None;
            }
        }
        let mut c = n.walk();
        let decls: Vec<Ts> = n.children_by_field_name("declarator", &mut c).collect();
        for d in decls {
            let value = if d.kind() == "init_declarator" { d.child_by_field_name("value") } else { None };
            let (id, ptr, f) = self.unwind(d);
            let name = id.map_or(NONE, |i| self.intern(text(self.g.src, &i)));
            let is_proto = f
                .as_ref()
                .is_some_and(|fd| fd.child_by_field_name("declarator").is_some_and(|x| x.kind() == "identifier"));
            let kind = if is_proto { Kind::Function } else { Kind::Variable };
            if is_proto {
                // a prototype takes the range of the whole declaration (incl. `;`)
                self.loc_override = Some((n.start_byte() as u32, n.end_byte() as u32));
            }
            let node = self.add(parent, kind, &d, name);
            self.typed(node, ty, ptr);
            let base = self.base_type(ty);
            let (vty, fp) = self.declarator_type(d, base);
            // quirk: a pointer to a function has an empty type name
            self.set_ty(node, if fp { "" } else { &vty });
            if is_proto {
                self.params(f.unwrap(), node);
            }
            // `char buf[len + 1]`: an ArrayConstruction (with the declaration's range) holds the size
            let mut inner = if d.kind() == "init_declarator" { d.child_by_field_name("declarator") } else { Some(d) };
            while let Some(x) = inner {
                if x.kind() == "array_declarator" {
                    // (only without an initializer)
                    if let (Some(size), None) = (x.child_by_field_name("size"), value) {
                        self.loc_override = Some((n.start_byte() as u32, n.end_byte() as u32));
                        let arr = self.add(node, Kind::ArrayConstruction, &n, NONE);
                        self.tr(size, arr);
                    }
                    break;
                }
                inner = x.child_by_field_name("declarator");
            }
            if let Some(v) = value {
                let saved = self.init_target;
                self.init_target = name;
                self.tr(v, node);
                self.init_target = saved;
            }
        }
    }

    pub(crate) fn record(&mut self, n: Ts, parent: u32) {
        let Some(body) = n.child_by_field_name("body") else { return };
        let name = n.child_by_field_name("name").map_or(NONE, |i| self.intern(text(self.g.src, &i)));
        if let Some(ds) = self.decl_start.take() {
            self.loc_override = Some((ds, n.end_byte() as u32));
        }
        let node = self.add(parent, Kind::Record, &n, name);
        let rec_name: Vec<u8> =
            if name == NONE { Vec::new() } else { self.g.names[name as usize].to_vec() };
        let mut c = body.walk();
        for f in body.named_children(&mut c) {
            if f.kind() != "field_declaration" {
                continue;
            }
            let ty = f.child_by_field_name("type");
            if let Some(t) = ty {
                if matches!(t.kind(), "struct_specifier" | "union_specifier" | "enum_specifier") {
                    self.tr(t, node);
                }
            }
            let mut c2 = f.walk();
            let ds: Vec<Ts> = f.children_by_field_name("declarator", &mut c2).collect();
            // quirk: no Field is created for `int x, y, z;` (they are only inferred when used)
            if ds.len() > 1 {
                continue;
            }
            for d in ds {
                let (id, ptr, _) = self.unwind(d);
                let nm = match id {
                    Some(i) => {
                        let mut q = rec_name.clone();
                        q.extend_from_slice(b"::");
                        q.extend_from_slice(text(self.g.src, &i));
                        self.intern_owned(q)
                    }
                    None => NONE,
                };
                let fld = self.add(node, Kind::Field, &d, nm);
                self.typed(fld, ty, ptr);
                let base = self.base_type(ty);
                let fty = self.declarator_type(d, base).0;
                self.set_ty(fld, &fty);
            }
        }
        // C records get an implicit constructor with a `this` variable in the Kotlin CPG
        let mut q = rec_name.clone();
        q.extend_from_slice(b"::");
        q.extend_from_slice(&rec_name);
        let qn = self.intern_owned(q);
        let ctor = self.add_implicit(node, Kind::Constructor, qn);
        let rn = String::from_utf8_lossy(&rec_name).into_owned();
        self.set_ty(ctor, &format!("{rn}(){rn}"));
        let this = self.intern(b"this");
        let tv = self.add_implicit(ctor, Kind::Variable, this);
        self.set_ty(tv, &format!("{rn}*"));
    }

    pub(crate) fn enumeration(&mut self, n: Ts, parent: u32) {
        let Some(body) = n.child_by_field_name("body") else { return };
        let name = n.child_by_field_name("name").map_or(NONE, |i| self.intern(text(self.g.src, &i)));
        if let Some(ds) = self.decl_start.take() {
            let end = self.decl_end.take().unwrap_or(n.end_byte() as u32);
            self.loc_override = Some((ds, end));
        }
        let node = self.add(parent, Kind::Enum, &n, name);
        let enum_name: Vec<u8> =
            if name == NONE { Vec::new() } else { self.g.names[name as usize].to_vec() };
        let mut c = body.walk();
        for e in body.named_children(&mut c) {
            if e.kind() != "enumerator" {
                continue;
            }
            let nm = match e.child_by_field_name("name") {
                Some(i) => {
                    let mut q = enum_name.clone();
                    q.extend_from_slice(b"::");
                    q.extend_from_slice(text(self.g.src, &i));
                    self.intern_owned(q)
                }
                None => NONE,
            };
            let en = self.add(node, Kind::Enumerator, &e, nm);
            self.set_ty(en, "int");
        }
    }

    pub(crate) fn typedef(&mut self, n: Ts, parent: u32) {
        let ty = n.child_by_field_name("type");
        if let Some(t) = ty {
            if matches!(t.kind(), "struct_specifier" | "union_specifier" | "enum_specifier") {
                let mut c = n.walk();
                let _ = &mut c;
                self.decl_end = Some(n.end_byte() as u32);
                self.decl_start = Some(n.start_byte() as u32);
                self.tr(t, parent);
                self.decl_start = None;
                self.decl_end = None;
            }
        }
        let mut c = n.walk();
        let ds: Vec<Ts> = n.children_by_field_name("declarator", &mut c).collect();
        for d in ds {
            let (id, ptr, _) = self.unwind(d);
            let nm = id.map_or(NONE, |i| self.intern(text(self.g.src, &i)));
            let node = self.add_implicit(parent, Kind::Typedef, nm);
            self.typed(node, ty, ptr);
            if let Some(i) = id {
                let base = self.base_type(ty);
                let resolved = self.declarator_type(d, base).0;
                self.typedefs.last_mut().unwrap().insert(text(self.g.src, &i).to_vec(), resolved);
            }
        }
    }
}
