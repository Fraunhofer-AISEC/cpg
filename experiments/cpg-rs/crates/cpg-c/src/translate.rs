use cpg_core::{Kind, NONE};
use rustc_hash::FxHashMap;
use tree_sitter::Node as Ts;

use crate::builder::{text, Builder};

impl<'a> Builder<'a, '_> {
    pub(crate) fn tr(&mut self, n: Ts, parent: u32) {
        let kind = match n.kind() {
            "comment" | "primitive_type" | "type_identifier" | "sized_type_specifier"
            | "storage_class_specifier" | "type_qualifier" | "attribute_specifier"
            | "macro_type_specifier" | "statement_identifier" | "type_descriptor" | "ERROR" => return,
            k if k.starts_with("preproc_") => {
                if matches!(k, "preproc_if" | "preproc_ifdef" | "preproc_else" | "preproc_elif"
                    | "preproc_elifdef") {
                    self.children(&n, parent);
                }
                return;
            }
            "function_definition" => return self.function(n, parent),
            "declaration" => return self.declaration(n, parent),
            "type_definition" => return self.typedef(n, parent),
            "struct_specifier" | "union_specifier" => return self.record(n, parent),
            "enum_specifier" => return self.enumeration(n, parent),
            "identifier" => return self.reference(n, parent),
            "compound_statement" => return self.scoped(n, parent, Kind::Block),
            "call_expression" => {
                let name = match n.child_by_field_name("function") {
                    Some(f) if f.kind() == "identifier" => self.intern(text(self.g.src, &f)),
                    _ => NONE,
                };
                let node = self.add(parent, Kind::Call, &n, name);
                if let Some(args) = n.child_by_field_name("arguments") {
                    self.children(&args, node);
                }
                if let Some(f) = n.child_by_field_name("function") {
                    self.tr(f, node);
                }
                if name == NONE {
                    let callee = self.g.nodes[node as usize].last_child;
                    if callee != NONE {
                        self.g.nodes[node as usize].name = self.name_of_child_node(callee);
                    }
                }
                return;
            }
            "translation_unit" => Kind::Unit,
            "return_statement" => Kind::Return,
            "break_statement" => Kind::Break,
            "continue_statement" => Kind::Continue,
            "case_statement" => {
                // only `case X:` is the Case; the statements that follow are its siblings
                let mut c = n.walk();
                let colon = n.children(&mut c).find(|x| x.kind() == ":").map(|x| x.end_byte());
                self.loc_override = Some((n.start_byte() as u32, colon.unwrap_or(n.end_byte()) as u32));
                let value = n.child_by_field_name("value");
                let node = self.add(parent, if value.is_some() { Kind::Case } else { Kind::Default }, &n, NONE);
                if let Some(v) = value {
                    self.tr(v, node);
                }
                let mut c = n.walk();
                for ch in n.named_children(&mut c) {
                    if value.is_none_or(|v| v.id() != ch.id()) {
                        self.tr(ch, parent);
                    }
                }
                return;
            }
            "cast_expression" => {
                // the target type, with typedefs resolved (like every type in the frontend)
                let target = match n.child_by_field_name("type") {
                    Some(t) => {
                        let base = self.base_type(t.child_by_field_name("type"));
                        match t.child_by_field_name("declarator") {
                            Some(d) => self.declarator_type(d, base).0,
                            None => base,
                        }
                    }
                    None => String::new(),
                };
                let name = self.intern_owned(target.clone().into_bytes());
                let node = self.add(parent, Kind::Cast, &n, name);
                self.set_ty(node, &target);
                if let Some(v) = n.child_by_field_name("value") {
                    if v.kind() == "parenthesized_expression" {
                        // unlike elsewhere, a parenthesized operand of a cast is a '()' operator
                        let op = self.intern(b"()");
                        let u = self.add(node, Kind::Unary, &v, op);
                        self.children(&v, u);
                    } else {
                        self.tr(v, node);
                    }
                }
                return;
            }
            "initializer_pair" => {
                let eq = self.intern(b"=");
                let node = self.add(parent, Kind::Assign, &n, eq);
                let mut c = n.walk();
                let ds: Vec<Ts> = n.children_by_field_name("designator", &mut c).collect();
                self.designators(&ds, node);
                if let Some(v) = n.child_by_field_name("value") {
                    self.tr(v, node);
                }
                return;
            }
            "if_statement" => {
                let node = self.add(parent, Kind::If, &n, NONE);
                if let Some(c) = n.child_by_field_name("condition") {
                    self.tr(c, node);
                }
                if let Some(c) = n.child_by_field_name("consequence") {
                    self.tr(c, node);
                }
                if let Some(e) = n.child_by_field_name("alternative") {
                    self.children(&e, node); // the else clause itself is not a node
                }
                return;
            }
            "while_statement" | "do_statement" => {
                let kind = if n.kind() == "while_statement" { Kind::While } else { Kind::DoWhile };
                let node = self.add(parent, kind, &n, NONE);
                // the Kotlin CPG lists the body before the condition
                if let Some(b) = n.child_by_field_name("body") {
                    self.tr(b, node);
                }
                if let Some(c) = n.child_by_field_name("condition") {
                    self.tr(c, node);
                }
                return;
            }
            "switch_statement" => {
                let node = self.add(parent, Kind::Switch, &n, NONE);
                if let Some(c) = n.child_by_field_name("condition") {
                    self.tr(c, node);
                }
                if let Some(b) = n.child_by_field_name("body") {
                    self.tr(b, node);
                }
                return;
            }
            "for_statement" => {
                let node = self.add(parent, Kind::For, &n, NONE);
                self.typedefs.push(FxHashMap::default());
                // the Kotlin CPG lists: body, initializer, condition, iteration
                if let Some(b) = n.child_by_field_name("body") {
                    self.tr(b, node);
                }
                let mut c = n.walk();
                let semicolon = n.children(&mut c).find(|x| x.kind() == ";");
                match n.child_by_field_name("initializer") {
                    Some(i) if i.kind() == "declaration" => self.tr(i, node),
                    Some(i) => {
                        // like an expression statement, the expression includes the `;`
                        let end = semicolon.map_or(i.end_byte(), |s| s.end_byte());
                        self.loc_override = Some((i.start_byte() as u32, end as u32));
                        self.tr(i, node);
                        self.loc_override = None;
                    }
                    None => {
                        if let Some(s) = semicolon {
                            self.add(node, Kind::Empty, &s, NONE);
                        }
                    }
                }
                match n.child_by_field_name("condition") {
                    Some(cond) => self.tr(cond, node),
                    None => {
                        // an absent condition is an implicit `true`
                        let t = self.add_implicit(node, Kind::Literal, NONE);
                        self.set_ty(t, "bool");
                        self.g.code_override.insert(t, "true".to_string());
                    }
                }
                if let Some(u) = n.child_by_field_name("update") {
                    self.tr(u, node);
                }
                self.typedefs.pop();
                return;
            }
            "conditional_expression" => {
                // has no location in the Kotlin CPG
                let node = self.add_implicit(parent, Kind::Conditional, NONE);
                for f in ["condition", "consequence", "alternative"] {
                    if let Some(c) = n.child_by_field_name(f) {
                        self.tr(c, node);
                    }
                }
                return;
            }
            "sizeof_expression" => {
                let op = self.intern(b"sizeof");
                if n.child_by_field_name("type").is_some() {
                    let node = self.add(parent, Kind::TypeReference, &n, op);
                    self.set_ty(node, "std::size_t");
                    return;
                }
                let node = self.add(parent, Kind::Unary, &n, op);
                if let Some(v) = n.child_by_field_name("value") {
                    if v.kind() == "parenthesized_expression" {
                        let paren = self.intern(b"()");
                        let u = self.add(node, Kind::Unary, &v, paren);
                        self.children(&v, u);
                    } else {
                        self.tr(v, node);
                    }
                }
                return;
            }
            "comma_expression" => {
                let node = self.add(parent, Kind::ExprList, &n, NONE);
                let mut items = Vec::new();
                flatten_commas(n, &mut items);
                for i in items {
                    self.tr(i, node);
                }
                return;
            }
            "goto_statement" | "labeled_statement" => {
                let kind = if n.kind() == "goto_statement" { Kind::Goto } else { Kind::Label };
                let name = n
                    .child_by_field_name("label")
                    .map_or(NONE, |l| self.intern(text(self.g.src, &l)));
                let node = self.add(parent, kind, &n, name);
                self.children(&n, node);
                return;
            }
            "expression_statement" => {
                // no node of its own: the expression takes the statement's range (incl. `;`)
                if n.named_child_count() == 0 {
                    self.add(parent, Kind::Empty, &n, NONE); // a lone `;`
                    return;
                }
                let range = (n.start_byte() as u32, n.end_byte() as u32);
                let mut c = n.walk();
                for ch in n.named_children(&mut c) {
                    self.loc_override = Some(range);
                    self.tr(ch, parent);
                    self.loc_override = None;
                }
                return;
            }
            "pointer_expression" => {
                let op = n.child_by_field_name("operator").map(|o| text(self.g.src, &o));
                let kind = if op == Some(b"*") { Kind::PointerDeref } else { Kind::PointerRef };
                let node = self.add(parent, kind, &n, NONE);
                self.children(&n, node);
                self.g.nodes[node as usize].name = self.name_of_child(node);
                return;
            }
            "parenthesized_expression" => {
                // transparent in the Kotlin CPG
                self.children(&n, parent);
                return;
            }
            "binary_expression" | "assignment_expression" | "unary_expression"
            | "update_expression" => {
                let kind = match n.kind() {
                    "binary_expression" => Kind::Binary,
                    "assignment_expression" => Kind::Assign,
                    _ => Kind::Unary,
                };
                let name = n
                    .child_by_field_name("operator")
                    .map_or(NONE, |o| self.intern(text(self.g.src, &o)));
                // `&&` and `||` are ShortCircuitOperators in the Kotlin CPG
                let kind = if kind == Kind::Binary
                    && name != NONE
                    && matches!(&*self.g.names[name as usize], b"&&" | b"||")
                {
                    Kind::ShortCircuit
                } else {
                    kind
                };
                let node = self.add(parent, kind, &n, name);
                self.children(&n, node);
                return;
            }
            "char_literal" => {
                // an escape like \9 is not an octal digit: the Kotlin frontend gives up on it
                let t = text(self.g.src, &n);
                let bad = t.windows(2).any(|w| w[0] == b'\\' && matches!(w[1], b'8' | b'9'));
                let node = self.add(parent, if bad { Kind::Problem } else { Kind::Literal }, &n, NONE);
                if !bad {
                    if let Some(t) = self.literal_type(&n) {
                        self.set_ty(node, &t);
                    }
                }
                return;
            }
            "number_literal" if text(self.g.src, &n).first().is_some_and(|c| matches!(c, b'-' | b'+')) => {
                // tree-sitter lexes `-1` as one literal; the Kotlin CPG has `-` applied to `1`
                let t = text(self.g.src, &n);
                let op = self.intern(&t[..1]);
                let u = self.add(parent, Kind::Unary, &n, op);
                self.loc_override = Some((n.start_byte() as u32 + 1, n.end_byte() as u32));
                let lit = self.add(u, Kind::Literal, &n, NONE);
                if let Some(ty) = self.literal_type_text("number_literal", &t[1..]) {
                    self.set_ty(lit, &ty);
                }
                return;
            }
            "number_literal" | "string_literal" | "true" | "false" | "null"
            | "concatenated_string" => Kind::Literal,
            "field_expression" => {
                let field = n
                    .child_by_field_name("field")
                    .map_or(NONE, |f| self.intern(text(self.g.src, &f)));
                let node = self.add(parent, Kind::Member, &n, field);
                if let Some(a) = n.child_by_field_name("argument") {
                    let arrow = n
                        .child_by_field_name("operator")
                        .is_some_and(|o| text(self.g.src, &o) == b"->");
                    if arrow {
                        // `p->x` is a member access of `*p` (with the range of the whole access)
                        let d = self.add(node, Kind::PointerDeref, &n, NONE);
                        self.tr(a, d);
                        self.g.nodes[d as usize].name = self.name_of_child(d);
                    } else {
                        self.tr(a, node);
                    }
                }
                return;
            }
            "subscript_expression" => Kind::Subscript,
            "initializer_list" => Kind::InitList,
            _ => Kind::Other,
        };
        let node = self.add(parent, kind, &n, NONE);
        if kind == Kind::Literal {
            if let Some(t) = self.literal_type(&n) {
                self.set_ty(node, &t);
            }
        } else {
            self.children(&n, node);
        }
    }
}

/// `a, b, c` is `comma(a, comma(b, c))` in tree-sitter, but one list in the Kotlin CPG.
fn flatten_commas<'t>(n: Ts<'t>, out: &mut Vec<Ts<'t>>) {
    if n.kind() == "comma_expression" {
        if let Some(l) = n.child_by_field_name("left") {
            flatten_commas(l, out);
        }
        if let Some(r) = n.child_by_field_name("right") {
            flatten_commas(r, out);
        }
    } else {
        out.push(n);
    }
}
