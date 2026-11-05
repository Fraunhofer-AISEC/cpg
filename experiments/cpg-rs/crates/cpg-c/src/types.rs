use tree_sitter::Node as Ts;

use crate::builder::{text, Builder};

impl<'a> Builder<'a, '_> {
    pub(crate) fn lookup_typedef(&self, name: &[u8]) -> Option<&String> {
        self.typedefs.iter().rev().find_map(|s| s.get(name))
    }

    /// A type name that may be a typedef.
    pub(crate) fn named_type(&self, name: &[u8]) -> String {
        match self.lookup_typedef(name) {
            Some(t) => t.clone(),
            None => String::from_utf8_lossy(name).into_owned(),
        }
    }

    /// The type described by the declaration specifier (qualifiers dropped, typedefs resolved).
    pub(crate) fn base_type(&self, t: Option<Ts>) -> String {
        let Some(t) = t else { return "int".to_string() };
        let txt = text(self.g.src, &t);
        // GNU `typeof(...)`: the Kotlin frontend names the type `typeof(<raw signature of the
        // parenthesized operand>)`, e.g. `typeof((int))`
        for kw in [&b"__typeof__"[..], b"__typeof", b"typeof"] {
            if let Some(rest) = txt.strip_prefix(kw) {
                let rest = rest.trim_ascii_start();
                if rest.first() == Some(&b'(') && rest.last() == Some(&b')') {
                    return format!("typeof({})", String::from_utf8_lossy(rest));
                }
            }
        }
        match t.kind() {
            "primitive_type" => {
                if matches!(txt, b"void" | b"char" | b"int" | b"float" | b"double" | b"_Bool" | b"bool") {
                    String::from_utf8_lossy(txt).into_owned()
                } else {
                    // size_t, int32_t, wchar_t, ...: tree-sitter lexes them as primitives, CDT as names
                    self.named_type(txt)
                }
            }
            "type_identifier" => self.named_type(txt),
            "sized_type_specifier" => {
                let (mut unsigned, mut signed, mut short, mut longs) = (false, false, false, 0);
                let mut base: Option<&[u8]> = None;
                let mut c = t.walk();
                for ch in t.children(&mut c) {
                    match ch.kind() {
                        "unsigned" => unsigned = true,
                        "signed" => signed = true,
                        "short" => short = true,
                        "long" => longs += 1,
                        "primitive_type" | "type_identifier" => base = Some(text(self.g.src, &ch)),
                        _ => {}
                    }
                }
                let mut parts: Vec<&str> = Vec::new();
                if unsigned {
                    parts.push("unsigned");
                } else if signed && base == Some(b"char") {
                    parts.push("signed");
                }
                let sized = short || longs > 0;
                if short {
                    parts.push("short");
                } else if longs == 1 {
                    parts.push("long");
                } else if longs > 1 {
                    parts.push("long long");
                }
                let base_s = base.map(|b| String::from_utf8_lossy(b).into_owned());
                match (&base_s, sized || signed || unsigned) {
                    (Some(b), _) => parts.push(b.as_str()),
                    (None, true) => parts.push("int"),
                    _ => {}
                }
                parts.join(" ")
            }
            "struct_specifier" | "union_specifier" | "enum_specifier" => t
                .child_by_field_name("name")
                .map_or(String::new(), |n| String::from_utf8_lossy(text(self.g.src, &n)).into_owned()),
            _ => String::from_utf8_lossy(txt).into_owned(),
        }
    }

    /// Applies a declarator chain to `base`. The flag is set for a pointer to a function, whose
    /// type name is empty in the Kotlin CPG.
    pub(crate) fn declarator_type(&self, d: Ts, base: String) -> (String, bool) {
        let (mut ty, mut fp, mut is_fn) = (base, false, false);
        let mut cur = Some(d);
        while let Some(x) = cur {
            match x.kind() {
                "pointer_declarator" | "abstract_pointer_declarator" => {
                    ty.push('*');
                    fp |= is_fn;
                }
                "array_declarator" | "abstract_array_declarator" => ty.push_str("[]"),
                "function_declarator" | "abstract_function_declarator" => {
                    ty = format!("({}){}", self.param_types(&x).join(", "), ty);
                    is_fn = true;
                }
                "init_declarator" | "parenthesized_declarator" | "attributed_declarator"
                | "abstract_parenthesized_declarator" => {}
                _ => break,
            }
            cur = match x.kind() {
                "parenthesized_declarator" | "abstract_parenthesized_declarator"
                | "attributed_declarator" => x.child_by_field_name("declarator").or_else(|| x.named_child(0)),
                _ => x.child_by_field_name("declarator"),
            };
        }
        (ty, fp)
    }

    pub(crate) fn param_types(&self, f: &Ts) -> Vec<String> {
        let Some(list) = f.child_by_field_name("parameters") else { return Vec::new() };
        let mut out = Vec::new();
        let mut c = list.walk();
        for p in list.named_children(&mut c) {
            if p.kind() != "parameter_declaration" {
                continue;
            }
            let ty = p.child_by_field_name("type");
            let decl = p.child_by_field_name("declarator");
            if decl.is_none() && ty.is_some_and(|t| text(self.g.src, &t) == b"void") {
                continue;
            }
            let base = self.base_type(ty);
            out.push(match decl {
                Some(d) => self.declarator_type(d, base).0,
                None => base,
            });
        }
        out
    }

    /// The Kotlin type of a literal.
    pub(crate) fn literal_type(&self, n: &Ts) -> Option<String> {
        self.literal_type_text(n.kind(), text(self.g.src, n))
    }

    pub(crate) fn literal_type_text(&self, kind: &str, t: &[u8]) -> Option<String> {
        match kind {
            "string_literal" | "concatenated_string" => Some("char[]".into()),
            "true" | "false" => Some("bool".into()),
            "char_literal" => {
                let wide = t.starts_with(b"L");
                let inner = &t[t.iter().position(|&b| b == b'\'').unwrap_or(0)..];
                let _ = inner;
                let body = t.strip_prefix(b"L").unwrap_or(t);
                let body = &body[1..body.len().saturating_sub(1).max(1)];
                // count characters: an escape is one character
                let (mut count, mut i) = (0, 0);
                while i < body.len() {
                    if body[i] == b'\\' && i + 1 < body.len() {
                        i += 1;
                        match body[i] {
                            b'x' => {
                                i += 1;
                                let max = if wide { 8 } else { 2 };
                                let mut k = 0;
                                while i < body.len() && k < max && body[i].is_ascii_hexdigit() {
                                    i += 1;
                                    k += 1;
                                }
                            }
                            b'0'..=b'7' => {
                                let mut k = 0;
                                while i < body.len() && k < 3 && (b'0'..=b'7').contains(&body[i]) {
                                    i += 1;
                                    k += 1;
                                }
                            }
                            _ => i += 1,
                        }
                    } else {
                        i += 1;
                    }
                    count += 1;
                }
                Some(if wide { "wchar_t" } else if count == 1 { "char" } else { "int" }.into())
            }
            "number_literal" => {
                let s = String::from_utf8_lossy(t).to_lowercase();
                let hex = s.starts_with("0x");
                let float = !hex && (s.contains('.') || s.contains('e')) || s.contains('p');
                if float {
                    return Some(if s.ends_with('f') { "float" } else if s.ends_with('l') { "long double" } else { "double" }.into());
                }
                let digits_end = s.trim_end_matches(|c| c == 'u' || c == 'l').len();
                let (num, suffix) = s.split_at(digits_end);
                let num = num.replace('\'', "");
                let value = if let Some(h) = num.strip_prefix("0x") {
                    u128::from_str_radix(h, 16)
                } else if let Some(b) = num.strip_prefix("0b") {
                    u128::from_str_radix(b, 2)
                } else if num.len() > 1 && num.starts_with('0') {
                    u128::from_str_radix(&num[1..], 8)
                } else {
                    num.parse::<u128>()
                }
                .ok()?;
                let big = value > i64::MAX as u128;
                Some(
                    match suffix {
                        "ul" => "unsigned long int",
                        "ull" => "unsigned long long int",
                        "ll" if !big => "long long int",
                        "l" if !big => "long int",
                        _ if big => "unsigned long long int",
                        _ if value > i32::MAX as u128 => "long int",
                        _ => "int",
                    }
                    .into(),
                )
            }
            _ => None,
        }
    }
}
