//! Lightweight, offset-preserving "preprocessor" for the tree-sitter spike.
//!
//! Not a conforming C preprocessor. It only does two things, and never changes the length of the
//! source (everything it removes is replaced by spaces, newlines are kept):
//!   1. Evaluates `#if/#ifdef/#ifndef/#elif/#else/#endif` and blanks inactive branches and the
//!      conditional directives themselves.
//!   2. Blanks uses of "safe" object-like macros: empty bodies, attribute-like bodies and short
//!      keyword aliases (e.g. `#define SQLITE_API`, `#define LOCAL static`).
//! Everything else (function-like macros, token pasting, includes) is left to the parser.

use std::collections::HashMap;

#[derive(Default, Debug, Clone)]
pub struct PpStats {
    pub conditionals: usize,
    pub undecided: usize,
    pub blanked_bytes: usize,
    pub substitutions: usize,
    pub expansions: usize,
    /// Byte ranges `(start, end)` of function-like macro calls that were expanded in place. The
    /// expansion is padded with spaces, so node ranges inside a span must be clamped to the span
    /// (see `clamp_to_spans`) to point at the original call.
    pub spans: Vec<(u32, u32)>,
}

struct Macro {
    func_like: bool,
    /// Parameter names of a function-like macro.
    params: Vec<Vec<u8>>,
    /// Supplied from outside (e.g. a profile), so function-like uses are expanded. Macros that the
    /// source defines itself are only blanked when trivially safe.
    external: bool,
    body: Vec<u8>,
    /// Replacement is blanking (`Some(b)` with `b.len() <= name.len()` is padded) or none.
    subst: Option<Vec<u8>>,
}

struct Frame {
    parent_active: bool,
    taken: bool,
    active: bool,
}

// Byte classes produced by `classify`.
const CODE: u8 = 0;
const COMMENT: u8 = 1;
const DELIM: u8 = 2;
const LITERAL: u8 = 3;

/// Classifies every byte of one physical line; `in_block` carries `/* */` state across lines.
fn classify(line: &[u8], in_block: &mut bool, out: &mut Vec<u8>) {
    out.clear();
    out.resize(line.len(), CODE);
    let mut i = 0;
    while i < line.len() {
        if *in_block {
            if line[i] == b'*' && i + 1 < line.len() && line[i + 1] == b'/' {
                out[i] = DELIM;
                out[i + 1] = DELIM;
                *in_block = false;
                i += 2;
            } else {
                out[i] = COMMENT;
                i += 1;
            }
            continue;
        }
        match line[i] {
            b'/' if i + 1 < line.len() && line[i + 1] == b'*' => {
                out[i] = DELIM;
                out[i + 1] = DELIM;
                *in_block = true;
                i += 2;
            }
            b'/' if i + 1 < line.len() && line[i + 1] == b'/' => {
                for b in &mut out[i..] {
                    *b = COMMENT;
                }
                return;
            }
            q @ (b'"' | b'\'') => {
                out[i] = LITERAL;
                i += 1;
                while i < line.len() {
                    out[i] = LITERAL;
                    if line[i] == b'\\' && i + 1 < line.len() {
                        out[i + 1] = LITERAL;
                        i += 2;
                        continue;
                    }
                    if line[i] == q {
                        i += 1;
                        break;
                    }
                    i += 1;
                }
            }
            _ => i += 1,
        }
    }
}

fn is_ident_start(b: u8) -> bool {
    b.is_ascii_alphabetic() || b == b'_'
}
fn is_ident(b: u8) -> bool {
    b.is_ascii_alphanumeric() || b == b'_'
}

const KEYWORDS: &[&str] = &[
    "static", "inline", "extern", "const", "volatile", "register", "int", "char", "void", "long",
    "short", "unsigned", "signed", "float", "double",
];

fn make_macro(name: &[u8], rest: &[u8]) -> Macro {
    let func_like = rest.first() == Some(&b'(');
    let body: Vec<u8> = if func_like {
        // body after the closing parenthesis of the parameter list
        match rest.iter().position(|&b| b == b')') {
            Some(p) => rest[p + 1..].trim_ascii().to_vec(),
            None => Vec::new(),
        }
    } else {
        rest.trim_ascii().to_vec()
    };
    let subst = if func_like {
        None
    } else if body.is_empty()
        || body.starts_with(b"__attribute__")
        || body.starts_with(b"__declspec")
        || body.starts_with(b"__extension__")
    {
        Some(Vec::new())
    } else if body.len() <= name.len()
        && KEYWORDS.iter().any(|k| k.as_bytes() == body.as_slice())
    {
        Some(body.clone())
    } else {
        None
    };
    Macro { func_like, params: Vec::new(), external: false, body, subst }
}

/// Builds a macro from a profile-style entry: `NAME` or `NAME(a,b)` mapped to the body.
fn external_macro(key: &str, body: &str) -> (Vec<u8>, Macro) {
    match key.split_once('(') {
        Some((name, params)) => {
            let params = params.trim_end_matches(')');
            let params: Vec<Vec<u8>> = params
                .split(',')
                .map(|p| p.trim().as_bytes().to_vec())
                .filter(|p| !p.is_empty())
                .collect();
            let m = Macro {
                func_like: true,
                params,
                external: true,
                body: body.trim().as_bytes().to_vec(),
                subst: None,
            };
            (name.trim().as_bytes().to_vec(), m)
        }
        None => {
            let mut m = make_macro(key.as_bytes(), body.as_bytes());
            m.external = true;
            (key.as_bytes().to_vec(), m)
        }
    }
}

// ---------------------------------------------------------------------------------------------
// #if expression evaluation

#[derive(Debug, Clone, PartialEq)]
enum Tok {
    Num(i64),
    Id(Vec<u8>),
    Op(&'static str),
}

fn tokenize(expr: &[u8], macros: &HashMap<Vec<u8>, Macro>) -> Result<Vec<Tok>, ()> {
    const OPS: &[&str] = &[
        "<<", ">>", "<=", ">=", "==", "!=", "&&", "||", "(", ")", "!", "~", "*", "/", "%", "+",
        "-", "<", ">", "&", "^", "|", "?", ":", ",",
    ];
    let mut toks = Vec::new();
    let mut i = 0;
    while i < expr.len() {
        let c = expr[i];
        if c.is_ascii_whitespace() {
            i += 1;
        } else if c.is_ascii_digit() {
            let s = i;
            while i < expr.len() && (is_ident(expr[i]) || expr[i] == b'.') {
                i += 1;
            }
            let t = std::str::from_utf8(&expr[s..i]).map_err(|_| ())?;
            let t = t.trim_end_matches(|c| matches!(c, 'u' | 'U' | 'l' | 'L'));
            let v = if let Some(h) = t.strip_prefix("0x").or_else(|| t.strip_prefix("0X")) {
                i64::from_str_radix(h, 16)
            } else if t.len() > 1 && t.starts_with('0') {
                i64::from_str_radix(&t[1..], 8)
            } else {
                t.parse::<i64>()
            };
            toks.push(Tok::Num(v.map_err(|_| ())?));
        } else if c == b'\'' {
            // character constant: only simple forms
            let s = i + 1;
            let mut j = s;
            while j < expr.len() && expr[j] != b'\'' {
                j += if expr[j] == b'\\' { 2 } else { 1 };
            }
            let inner = expr.get(s..j).ok_or(())?;
            let v = match inner {
                [b'\\', b'n'] => 10,
                [b'\\', b'0'] => 0,
                [b'\\', x] => *x as i64,
                [x] => *x as i64,
                _ => return Err(()),
            };
            toks.push(Tok::Num(v));
            i = j + 1;
        } else if is_ident_start(c) {
            let s = i;
            while i < expr.len() && is_ident(expr[i]) {
                i += 1;
            }
            let id = &expr[s..i];
            if id == b"defined" {
                let mut j = i;
                while j < expr.len() && expr[j].is_ascii_whitespace() {
                    j += 1;
                }
                let paren = expr.get(j) == Some(&b'(');
                if paren {
                    j += 1;
                    while j < expr.len() && expr[j].is_ascii_whitespace() {
                        j += 1;
                    }
                }
                let s2 = j;
                while j < expr.len() && is_ident(expr[j]) {
                    j += 1;
                }
                let name = &expr[s2..j];
                if name.is_empty() {
                    return Err(());
                }
                if paren {
                    while j < expr.len() && expr[j].is_ascii_whitespace() {
                        j += 1;
                    }
                    if expr.get(j) != Some(&b')') {
                        return Err(());
                    }
                    j += 1;
                }
                toks.push(Tok::Num(macros.contains_key(name) as i64));
                i = j;
            } else if id.starts_with(b"__has_") && expr[i..].trim_ascii_start().first() == Some(&b'(')
            {
                // __has_include(<x.h>), __has_feature(x), ...: arguments are not expressions
                let mut j = i + expr[i..].iter().position(|&b| b == b'(').unwrap();
                let mut depth = 0;
                while j < expr.len() {
                    match expr[j] {
                        b'(' => depth += 1,
                        b')' => depth -= 1,
                        _ => {}
                    }
                    j += 1;
                    if depth == 0 {
                        break;
                    }
                }
                if depth != 0 {
                    return Err(());
                }
                toks.push(Tok::Num(0));
                i = j;
            } else {
                toks.push(Tok::Id(id.to_vec()));
            }
        } else {
            let rest = &expr[i..];
            match OPS.iter().find(|o| rest.starts_with(o.as_bytes())) {
                Some(o) => {
                    toks.push(Tok::Op(o));
                    i += o.len();
                }
                None => return Err(()),
            }
        }
    }
    Ok(toks)
}

struct Eval<'a> {
    toks: Vec<Tok>,
    pos: usize,
    macros: &'a HashMap<Vec<u8>, Macro>,
    depth: u32,
}

impl Eval<'_> {
    fn peek_op(&self) -> Option<&'static str> {
        match self.toks.get(self.pos) {
            Some(Tok::Op(o)) => Some(o),
            _ => None,
        }
    }
    fn eat(&mut self, op: &str) -> bool {
        if self.peek_op() == Some(op) {
            self.pos += 1;
            true
        } else {
            false
        }
    }

    fn ternary(&mut self) -> Result<i64, ()> {
        let c = self.binary(0)?;
        if self.eat("?") {
            let a = self.ternary()?;
            if !self.eat(":") {
                return Err(());
            }
            let b = self.ternary()?;
            return Ok(if c != 0 { a } else { b });
        }
        Ok(c)
    }

    fn level_ops(level: usize) -> &'static [&'static str] {
        const L: [&[&str]; 9] = [
            &["||"],
            &["&&"],
            &["|"],
            &["^"],
            &["&"],
            &["==", "!="],
            &["<", ">", "<=", ">="],
            &["<<", ">>"],
            &["+", "-"],
        ];
        if level < L.len() { L[level] } else { &["*", "/", "%"] }
    }

    fn binary(&mut self, level: usize) -> Result<i64, ()> {
        if level > 9 {
            return self.unary();
        }
        let mut l = self.binary(level + 1)?;
        while let Some(op) = self.peek_op() {
            if !Self::level_ops(level).contains(&op) {
                break;
            }
            self.pos += 1;
            let r = self.binary(level + 1)?;
            l = match op {
                "||" => (l != 0 || r != 0) as i64,
                "&&" => (l != 0 && r != 0) as i64,
                "|" => l | r,
                "^" => l ^ r,
                "&" => l & r,
                "==" => (l == r) as i64,
                "!=" => (l != r) as i64,
                "<" => (l < r) as i64,
                ">" => (l > r) as i64,
                "<=" => (l <= r) as i64,
                ">=" => (l >= r) as i64,
                "<<" => l.checked_shl(r as u32).ok_or(())?,
                ">>" => l.checked_shr(r as u32).ok_or(())?,
                "+" => l.wrapping_add(r),
                "-" => l.wrapping_sub(r),
                "*" => l.wrapping_mul(r),
                "/" => l.checked_div(r).ok_or(())?,
                "%" => l.checked_rem(r).ok_or(())?,
                _ => return Err(()),
            };
        }
        Ok(l)
    }

    fn unary(&mut self) -> Result<i64, ()> {
        if self.eat("!") {
            return Ok((self.unary()? == 0) as i64);
        }
        if self.eat("-") {
            return Ok(self.unary()?.wrapping_neg());
        }
        if self.eat("+") {
            return self.unary();
        }
        if self.eat("~") {
            return Ok(!self.unary()?);
        }
        self.primary()
    }

    fn primary(&mut self) -> Result<i64, ()> {
        match self.toks.get(self.pos).cloned() {
            Some(Tok::Num(n)) => {
                self.pos += 1;
                Ok(n)
            }
            Some(Tok::Op("(")) => {
                self.pos += 1;
                let v = self.ternary()?;
                if !self.eat(")") {
                    return Err(());
                }
                Ok(v)
            }
            Some(Tok::Id(id)) => {
                self.pos += 1;
                // function-like use, e.g. `__has_include(<x>)`: skip the arguments, value 0
                if self.peek_op() == Some("(") {
                    let mut depth = 0;
                    loop {
                        match self.toks.get(self.pos) {
                            Some(Tok::Op("(")) => depth += 1,
                            Some(Tok::Op(")")) => depth -= 1,
                            None => return Err(()),
                            _ => {}
                        }
                        self.pos += 1;
                        if depth == 0 {
                            break;
                        }
                    }
                    return Ok(0);
                }
                match self.macros.get(&id) {
                    Some(m) if !m.func_like && self.depth < 8 => {
                        if m.body.is_empty() {
                            return Err(()); // `#if EMPTY_MACRO` is a hard error in C
                        }
                        eval(&m.body, self.macros, self.depth + 1)
                    }
                    _ => Ok(0), // undefined identifiers are 0
                }
            }
            _ => Err(()),
        }
    }
}

fn eval(expr: &[u8], macros: &HashMap<Vec<u8>, Macro>, depth: u32) -> Result<i64, ()> {
    let toks = tokenize(expr, macros)?;
    let mut e = Eval { toks, pos: 0, macros, depth };
    let v = e.ternary()?;
    if e.pos != e.toks.len() {
        return Err(());
    }
    Ok(v)
}

// ---------------------------------------------------------------------------------------------

/// Preprocesses `src` into a buffer of the same length. `defines` use the format of a CPG
/// `Profile.symbols()` map: `NAME` or `NAME(a,b)` (function-like) mapped to the body.
pub fn preprocess(src: &[u8], defines: &[(&str, &str)]) -> (Vec<u8>, PpStats) {
    let mut out = src.to_vec();
    let mut stats = PpStats::default();
    let mut macros: HashMap<Vec<u8>, Macro> = HashMap::new();
    for (k, b) in defines {
        let (name, m) = external_macro(k, b);
        macros.insert(name, m);
    }
    let mut frames: Vec<Frame> = Vec::new();
    let mut in_block = false;
    let mut mask: Vec<u8> = Vec::new();
    let mut pos = 0;

    while pos < src.len() {
        let line_start = pos;
        let mut line_end = src[pos..].iter().position(|&b| b == b'\n').map_or(src.len(), |p| pos + p);
        let active = frames.last().is_none_or(|f| f.active);

        let started_in_block = in_block;
        classify(&src[line_start..line_end], &mut in_block, &mut mask);
        let first = src[line_start..line_end]
            .iter()
            .zip(mask.iter())
            .position(|(b, &m)| !(b.is_ascii_whitespace() && m == CODE));
        let is_directive = !started_in_block
            && first.is_some_and(|f| src[line_start + f] == b'#' && mask[f] == CODE);

        if !is_directive {
            if active {
                substitute(&mut out, src, line_start, line_end, &mask, &macros, &mut stats);
            } else {
                blank(&mut out, src, line_start, line_end, Some(&mask), &mut stats);
            }
            pos = line_end + 1;
            continue;
        }

        // Gather the logical directive text across backslash-continued physical lines,
        // with comments removed.
        let mut text: Vec<u8> = Vec::new();
        let mut cur_start = line_start;
        loop {
            let slice = &src[cur_start..line_end];
            for (i, &b) in slice.iter().enumerate() {
                text.push(if mask[i] == CODE || mask[i] == LITERAL { b } else { b' ' });
            }
            let continued = slice.last() == Some(&b'\\') && mask.last() == Some(&CODE);
            if continued && line_end < src.len() {
                text.pop();
                cur_start = line_end + 1;
                line_end = src[cur_start..].iter().position(|&b| b == b'\n').map_or(src.len(), |p| cur_start + p);
                classify(&src[cur_start..line_end], &mut in_block, &mut mask);
            } else {
                break;
            }
        }
        let hash = text.iter().position(|&b| b == b'#').unwrap();
        let body = text[hash + 1..].trim_ascii_start();
        let dend = body.iter().position(|&b| !is_ident(b)).unwrap_or(body.len());
        let (dir, rest) = (&body[..dend], body[dend..].trim_ascii());

        let mut conditional = true;
        match dir {
            b"if" | b"ifdef" | b"ifndef" => {
                stats.conditionals += 1;
                let cond = if !active {
                    false
                } else {
                    match dir {
                        b"ifdef" => macros.contains_key(rest_name(rest)),
                        b"ifndef" => !macros.contains_key(rest_name(rest)),
                        _ => eval_cond(rest, &macros, &mut stats),
                    }
                };
                frames.push(Frame { parent_active: active, taken: cond || !active, active: cond && active });
            }
            b"elif" | b"else" => {
                if let Some(f) = frames.last_mut() {
                    let cond = if f.parent_active && !f.taken {
                        if dir == b"else" { true } else { eval_cond(rest, &macros, &mut stats) }
                    } else {
                        false
                    };
                    f.active = cond && f.parent_active;
                    f.taken |= cond;
                }
            }
            b"endif" => {
                frames.pop();
            }
            b"define" if active => {
                conditional = false;
                let n = rest.iter().position(|&b| !is_ident(b)).unwrap_or(rest.len());
                if n > 0 {
                    macros.insert(rest[..n].to_vec(), make_macro(&rest[..n], &rest[n..]));
                }
            }
            b"undef" if active => {
                conditional = false;
                macros.remove(rest_name(rest));
            }
            _ => conditional = false,
        }

        // Conditional directives are resolved, so remove them; everything in an inactive region
        // goes too. Active #define/#include/... stay for the parser.
        if conditional || !active {
            blank(&mut out, src, line_start, line_end, None, &mut stats);
        }
        pos = line_end + 1;
    }
    (out, stats)
}

fn rest_name(rest: &[u8]) -> &[u8] {
    let n = rest.iter().position(|&b| !is_ident(b)).unwrap_or(rest.len());
    &rest[..n]
}

/// Undecidable conditions are taken (true), and counted.
fn eval_cond(expr: &[u8], macros: &HashMap<Vec<u8>, Macro>, stats: &mut PpStats) -> bool {
    match eval(expr, macros, 0) {
        Ok(v) => v != 0,
        Err(()) => {
            stats.undecided += 1;
            true
        }
    }
}

/// Replaces `src[start..end]` by spaces in `out`, keeping newlines. With a mask, comment delimiters
/// survive so that a comment that opened in blanked text still closes consistently.
fn blank(out: &mut [u8], src: &[u8], start: usize, end: usize, mask: Option<&[u8]>, stats: &mut PpStats) {
    for i in start..end.min(src.len()) {
        let keep = src[i] == b'\n' || mask.is_some_and(|m| m[i - start] == DELIM);
        if !keep {
            if out[i] != b' ' {
                stats.blanked_bytes += 1;
            }
            out[i] = b' ';
        }
    }
}

/// Expands a call of the function-like macro `m` whose name ends at `name_end`. Returns the
/// expansion and the end of the call, or `None` if the call is not well-formed on this line, the
/// argument count does not match, or the body uses `#`/`##` (not supported).
fn expand_call(line: &[u8], mask: &[u8], name_end: usize, m: &Macro) -> Option<(Vec<u8>, usize)> {
    if m.body.contains(&b'#') {
        return None;
    }
    let mut j = name_end;
    while j < line.len() && line[j].is_ascii_whitespace() {
        j += 1;
    }
    if line.get(j) != Some(&b'(') || mask[j] != CODE {
        return None;
    }
    let mut args: Vec<&[u8]> = Vec::new();
    let (mut depth, mut arg_start) = (0usize, j + 1);
    let mut k = j;
    let call_end = loop {
        if k >= line.len() {
            return None;
        }
        if mask[k] == CODE {
            match line[k] {
                b'(' | b'[' | b'{' => depth += 1,
                b')' | b']' | b'}' => {
                    depth -= 1;
                    if depth == 0 {
                        args.push(line[arg_start..k].trim_ascii());
                        break k + 1;
                    }
                }
                b',' if depth == 1 => {
                    args.push(line[arg_start..k].trim_ascii());
                    arg_start = k + 1;
                }
                _ => {}
            }
        }
        k += 1;
    };
    if m.params.is_empty() && args == [b"".as_slice()] {
        args.clear();
    }
    if args.len() != m.params.len() {
        return None;
    }

    // Substitute the parameters in the body; no rescanning of the result.
    let body = &m.body;
    let mut text = Vec::with_capacity(body.len());
    let mut i = 0;
    while i < body.len() {
        if body[i].is_ascii_digit() {
            while i < body.len() && (is_ident(body[i]) || body[i] == b'.') {
                text.push(body[i]);
                i += 1;
            }
        } else if is_ident_start(body[i]) {
            let s = i;
            while i < body.len() && is_ident(body[i]) {
                i += 1;
            }
            match m.params.iter().position(|p| p == &body[s..i]) {
                Some(n) => text.extend_from_slice(args[n]),
                None => text.extend_from_slice(&body[s..i]),
            }
        } else {
            text.push(body[i]);
            i += 1;
        }
    }
    Some((text, call_end))
}

/// Expands the object-like macros in `body` (recursively, up to a depth), leaving everything else
/// alone. `None` if the expansion is too deep (a cycle).
fn expand_object(
    body: &[u8],
    macros: &HashMap<Vec<u8>, Macro>,
    depth: u32,
    self_name: &[u8],
) -> Option<Vec<u8>> {
    if depth > 8 {
        return None;
    }
    let mut out = Vec::with_capacity(body.len());
    let mut i = 0;
    while i < body.len() {
        let c = body[i];
        if c == b'"' || c == b'\'' {
            let s = i;
            i += 1;
            while i < body.len() && body[i] != c {
                i += if body[i] == b'\\' { 2 } else { 1 };
            }
            i = (i + 1).min(body.len());
            out.extend_from_slice(&body[s..i]);
        } else if c.is_ascii_digit() {
            while i < body.len() && (is_ident(body[i]) || body[i] == b'.') {
                out.push(body[i]);
                i += 1;
            }
        } else if is_ident_start(c) {
            let s = i;
            while i < body.len() && is_ident(body[i]) {
                i += 1;
            }
            let id = &body[s..i];
            match macros.get(id) {
                Some(m) if !m.func_like && id != self_name => {
                    out.extend_from_slice(&expand_object(&m.body, macros, depth + 1, id)?);
                }
                _ => out.extend_from_slice(id),
            }
        } else {
            out.push(c);
            i += 1;
        }
    }
    Some(out)
}

/// Clamps the byte range of a node to the original call if it starts or ends inside an expansion.
pub fn clamp_to_spans(spans: &[(u32, u32)], start: u32, end: u32) -> (u32, u32) {
    let (mut s, mut e) = (start, end);
    let k = spans.partition_point(|&(a, _)| a <= s);
    if k > 0 && s < spans[k - 1].1 {
        s = spans[k - 1].0;
    }
    let k = spans.partition_point(|&(a, _)| a < e);
    if k > 0 && e <= spans[k - 1].1 {
        e = spans[k - 1].1;
    }
    (s, e)
}

fn substitute(
    out: &mut [u8],
    src: &[u8],
    start: usize,
    end: usize,
    mask: &[u8],
    macros: &HashMap<Vec<u8>, Macro>,
    stats: &mut PpStats,
) {
    let line = &src[start..end];
    let mut i = 0;
    while i < line.len() {
        if mask[i] != CODE {
            i += 1;
        } else if line[i].is_ascii_digit() {
            while i < line.len() && (is_ident(line[i]) || line[i] == b'.') && mask[i] == CODE {
                i += 1;
            }
        } else if is_ident_start(line[i]) {
            let s = i;
            while i < line.len() && is_ident(line[i]) && mask[i] == CODE {
                i += 1;
            }
            match macros.get(&line[s..i]) {
                Some(Macro { subst: Some(rep), .. }) => {
                    let dst = &mut out[start + s..start + i];
                    dst.fill(b' ');
                    dst[..rep.len()].copy_from_slice(rep);
                    stats.substitutions += 1;
                }
                Some(m) if !m.func_like && !m.body.is_empty() => {
                    // an object-like macro expands in place if the expansion fits (padded), and
                    // all nodes inside it later get the range of the macro use
                    let name = &line[s..i];
                    if let Some(text) = expand_object(&m.body, macros, 0, name) {
                        if text.len() <= i - s {
                            let dst = &mut out[start + s..start + i];
                            dst.fill(b' ');
                            dst[..text.len()].copy_from_slice(&text);
                            stats.expansions += 1;
                            stats.spans.push(((start + s) as u32, (start + i) as u32));
                        }
                    }
                }
                Some(m) if m.func_like && m.external => {
                    if let Some((text, call_end)) = expand_call(line, mask, i, m) {
                        if text.len() <= call_end - s {
                            let dst = &mut out[start + s..start + call_end];
                            dst.fill(b' ');
                            dst[..text.len()].copy_from_slice(&text);
                            stats.expansions += 1;
                            stats.spans.push(((start + s) as u32, (start + call_end) as u32));
                            i = call_end;
                        }
                    }
                }
                _ => {}
            }
        } else {
            i += 1;
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    fn pp(s: &str) -> String {
        let (o, _) = preprocess(s.as_bytes(), &[]);
        assert_eq!(o.len(), s.len(), "length must be preserved");
        String::from_utf8(o).unwrap()
    }

    #[test]
    fn blanks_inactive_branch_and_keeps_offsets() {
        let out = pp("#define A 1\n#if A\nint x;\n#else\nint y;\n#endif\n");
        assert!(out.contains("int x;"));
        assert!(!out.contains("int y;"));
        assert!(!out.contains("#if") && !out.contains("#endif"));
        assert!(out.starts_with("#define A 1\n"));
    }

    #[test]
    fn elif_chain_takes_first_true_only() {
        let out = pp("#if 0\na\n#elif 1\nb\n#elif 1\nc\n#else\nd\n#endif\n");
        let words: Vec<_> = out.split_whitespace().collect();
        assert_eq!(words, vec!["b"]);
    }

    #[test]
    fn nested_inactive_stays_inactive() {
        let out = pp("#if 0\n#if 1\nx\n#else\ny\n#endif\n#endif\nz\n");
        let words: Vec<_> = out.split_whitespace().collect();
        assert_eq!(words, vec!["z"]);
    }

    #[test]
    fn defined_and_arithmetic() {
        let out = pp("#define V 3\n#if defined(V) && V >= 3 && !defined X && (V*2 == 6)\nok\n#endif\n");
        assert!(out.contains("ok"));
    }

    #[test]
    fn unknown_identifier_is_zero_and_has_include_skipped() {
        let out = pp("#if FOO\nno\n#endif\n#if __has_include(<x.h>)\nno2\n#endif\nyes\n");
        assert_eq!(out.split_whitespace().collect::<Vec<_>>(), vec!["yes"]);
    }

    #[test]
    fn safe_macros_are_blanked_unsafe_are_not() {
        let out = pp("#define API\n#define LOCALLY static\n#define BIG (1+2)\nAPI int f(void); LOCALLY int g; BIG;\n");
        assert!(out.contains("    int f(void);"));
        let code = out.lines().last().unwrap();
        assert!(!code.contains("LOCALLY") && code.contains("static  int g;"));
        assert!(code.contains("BIG;"));
    }

    fn pp_with(s: &str, defs: &[(&str, &str)]) -> (String, PpStats) {
        let (o, st) = preprocess(s.as_bytes(), defs);
        assert_eq!(o.len(), s.len(), "length must be preserved");
        (String::from_utf8(o).unwrap(), st)
    }

    #[test]
    fn expands_external_function_like_macro_in_place() {
        let (out, st) = pp_with("x = va_arg(ap, int*) + 1;\n", &[("va_arg(a,t)", "((t)0)")]);
        assert_eq!(out, "x = ((int*)0)        + 1;\n");
        assert_eq!(st.expansions, 1);
        assert_eq!(st.spans, vec![(4, 20)]);
    }

    #[test]
    fn nested_parens_in_arguments_and_wrong_arity() {
        let defs = [("va_arg(a,t)", "((t)0)")];
        let (out, _) = pp_with("v = va_arg(f(a, b), struct S *);\n", &defs);
        assert!(out.contains("((struct S *)0)"));
        let (out, st) = pp_with("v = va_arg(ap);\n", &defs);
        assert_eq!(st.expansions, 0);
        assert!(out.contains("va_arg(ap)"));
    }

    #[test]
    fn expansion_that_does_not_fit_is_left_alone() {
        let (out, st) = pp_with("y = F(1);\n", &[("F(a)", "((a) + (a) + (a) + (a))")]);
        assert_eq!(st.expansions, 0);
        assert!(out.contains("F(1)"));
    }

    #[test]
    fn source_defined_function_like_macros_are_not_expanded() {
        let (out, st) = pp_with("#define ID(a) a\nz = ID(1);\n", &[]);
        assert_eq!(st.expansions, 0);
        assert!(out.contains("ID(1)"));
    }

    #[test]
    fn spans_clamp_node_ranges_to_the_call() {
        let spans = [(10u32, 30u32)];
        assert_eq!(clamp_to_spans(&spans, 12, 20), (10, 30));
        assert_eq!(clamp_to_spans(&spans, 0, 20), (0, 30));
        assert_eq!(clamp_to_spans(&spans, 0, 5), (0, 5));
        assert_eq!(clamp_to_spans(&spans, 0, 40), (0, 40));
        assert_eq!(clamp_to_spans(&spans, 30, 40), (30, 40));
    }

    #[test]
    fn strings_and_comments_untouched() {
        let out = pp("#define API\nchar *s = \"API\"; /* API */ API int x;\n");
        assert!(out.contains("\"API\"") && out.contains("/* API */"));
    }

    #[test]
    fn inactive_comment_delimiters_stay_balanced() {
        let out = pp("#if 0\n/* open\n#endif\nstill comment */\n#endif\nint x;\n");
        assert!(out.contains("int x;"));
    }

    #[test]
    fn continued_directive_lines_are_blanked() {
        let out = pp("#if 0 \\\n  && 1\nx\n#endif\ny\n");
        assert_eq!(out.split_whitespace().collect::<Vec<_>>(), vec!["y"]);
    }
}
