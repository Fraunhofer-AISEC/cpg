//! Port of the Kotlin `PointsToPass` on the same state model (general state + declaration state).
//!
//! The state is a pair of maps:
//! - the *general state*: node -> (addresses, values, last writes), what the analysis knows about the
//!   expression as it is evaluated; the last writes of a node are the sources of its incoming data
//!   flow edges in the end,
//! - the *declaration state*: address -> (field addresses, values, last writes), what is stored at a
//!   memory address.
//!
//! Both are powerset lattices. Synthetic nodes (`MemoryAddress`, `UnknownMemoryValue`,
//! `ParameterMemoryValue`) live in `Graph::synth`. Everything below mirrors the function of the same
//! name in `PointsToPass.kt`; see `oracle/POINTSTO_NOTES.md` for the map.
//!
//! Not ported yet: calls and function summaries, `new`/`delete`, for-each, the record-aware
//! initializer lists.

use std::collections::{BTreeMap, BTreeSet, VecDeque};

use cpg_core::{Access, Granularity, Graph, Kind, NONE, SYNTH_BASE};
use rustc_hash::FxHashMap;

use crate::numeric::numeric;

type N = u32;

/// The limits of the analysis; the defaults are those of `PointsToPass.Configuration`.
#[derive(Clone, Debug)]
pub struct Config {
    pub address_length: u32,
    pub max_array_initializers: Option<usize>,
    pub max_field_access_path_depth: Option<usize>,
    pub max_synthetic_chain_depth: Option<usize>,
    pub draw_current_deref_dfg: bool,
}

impl Default for Config {
    fn default() -> Self {
        Config {
            address_length: 64,
            max_array_initializers: Some(200),
            max_field_access_path_depth: Some(6),
            max_synthetic_chain_depth: Some(6),
            draw_current_deref_dfg: true,
        }
    }
}

/// What a partial dataflow granularity points to.
#[derive(Clone, PartialEq, Eq, PartialOrd, Ord, Debug)]
enum Off {
    Node(N),
    Str(String),
}

/// The properties of a `NodeWithPropertiesKey`: a set in Kotlin, a record of the possible members here.
#[derive(Clone, PartialEq, Eq, PartialOrd, Ord, Debug, Default)]
struct Props {
    /// A Boolean: whether this is a short function summary entry / function summary edge.
    fs: Option<bool>,
    /// A `PartialDataflowGranularity`.
    off: Option<Off>,
    /// A `PointerDataflowGranularity` (1: current deref value, 2: current deref deref value).
    deref: u8,
}

impl Props {
    fn fs(b: bool) -> Props {
        Props { fs: Some(b), ..Props::default() }
    }
    fn contains_all(&self, o: &Props) -> bool {
        (o.fs.is_none() || self.fs == o.fs)
            && (o.off.is_none() || self.off == o.off)
            && (o.deref == 0 || self.deref == o.deref)
    }
}

/// `NodeWithPropertiesKey`.
#[derive(Clone, PartialEq, Eq, PartialOrd, Ord, Debug)]
struct Key {
    n: N,
    p: Props,
}

fn key(n: N, p: Props) -> Key {
    Key { n, p }
}

type Set<T> = BTreeSet<T>;

#[derive(Clone, Default, PartialEq)]
struct GEntry {
    addrs: Set<N>,
    vals: Set<Key>,
    lw: Set<Key>,
}

#[derive(Clone, Default, PartialEq)]
struct DEntry {
    addrs: Set<N>,
    /// (value, short function summary)
    vals: Set<(N, bool)>,
    lw: Set<Key>,
}

#[derive(Clone, Default, PartialEq)]
struct State {
    gs: BTreeMap<N, GEntry>,
    decl: BTreeMap<N, DEntry>,
}

impl State {
    /// Least upper bound into `self`; returns whether `self` changed.
    fn join(&mut self, o: &State) -> bool {
        let mut ch = false;
        for (k, e) in &o.gs {
            let s = self.gs.entry(*k).or_default();
            ch |= union(&mut s.addrs, &e.addrs);
            ch |= union(&mut s.vals, &e.vals);
            ch |= union(&mut s.lw, &e.lw);
        }
        for (k, e) in &o.decl {
            let s = self.decl.entry(*k).or_default();
            ch |= union(&mut s.addrs, &e.addrs);
            ch |= union(&mut s.vals, &e.vals);
            ch |= union(&mut s.lw, &e.lw);
        }
        ch
    }

    /// `push`: adds to what the general state holds for `n`.
    fn push(&mut self, n: N, e: GEntry) {
        let s = self.gs.entry(n).or_default();
        s.addrs.extend(e.addrs);
        s.vals.extend(e.vals);
        s.lw.extend(e.lw);
    }

    /// `pushToDeclarationsState`.
    fn push_decl(&mut self, n: N, e: DEntry) {
        let s = self.decl.entry(n).or_default();
        s.addrs.extend(e.addrs);
        s.vals.extend(e.vals);
        s.lw.extend(e.lw);
    }

    fn has_decl_value(&self, n: N, excl_short: bool) -> bool {
        match self.decl.get(&n) {
            Some(e) => {
                if excl_short {
                    e.vals.iter().any(|v| !v.1)
                } else {
                    !e.vals.is_empty()
                }
            }
            None => false,
        }
    }
}

fn union<T: Ord + Clone>(into: &mut Set<T>, from: &Set<T>) -> bool {
    let mut ch = false;
    for x in from {
        if !into.contains(x) {
            into.insert(x.clone());
            ch = true;
        }
    }
    ch
}

#[derive(Clone, Copy, PartialEq, Eq, Debug)]
enum SClass {
    Memory,
    Unknown,
    Param,
}

struct Synth {
    class: SClass,
    local: String,
    full: String,
    global: bool,
}

#[derive(Clone, Copy)]
struct Origin {
    root: N,
    depth: usize,
    summary: bool,
}

/// A name: the local name and the full (qualified) name.
#[derive(Clone, PartialEq, Eq, Hash, Debug)]
struct Name {
    local: String,
    full: String,
}

impl Name {
    fn plain(s: &str) -> Name {
        Name { local: local_of(s).to_string(), full: s.to_string() }
    }
    /// `Name(local, parent)`
    fn child(local: &str, parent: &Name) -> Name {
        Name { local: local.to_string(), full: format!("{}.{}", parent.full, local) }
    }
}

fn local_of(full: &str) -> &str {
    let a = full.rfind("::").map(|i| i + 2);
    let b = full.rfind('.').map(|i| i + 1);
    &full[a.max(b).unwrap_or(0)..]
}

/// A fetched value of the declaration state.
struct Fetched {
    value: N,
    short: bool,
}

struct Pt<'g, 'a> {
    g: &'g mut Graph<'a>,
    cfg: Config,
    parent: Vec<N>,
    ref_of: Vec<N>,
    succ: Vec<Vec<N>>,
    synth: Vec<Synth>,
    /// `node.memoryAddresses`
    mem_addrs: FxHashMap<N, Vec<N>>,
    /// `param.memoryValues` (the `ParameterMemoryValue`s)
    param_mvs: FxHashMap<N, Vec<N>>,
    /// `nodesCreatingUnknownValues`
    unknowns: FxHashMap<(N, String), N>,
    origins: FxHashMap<N, Origin>,
    summaries: FxHashMap<(N, String, bool), N>,
    /// `globalDerefs`
    global_derefs: FxHashMap<N, Set<(N, bool)>>,
    /// the temporary subscriptions of `getAddresses` of a dereference of `ptr + literal`: (array, index)
    fn_of: Vec<N>,
}

pub fn pointsto(g: &mut Graph) {
    pointsto_with(g, Config::default())
}

pub fn pointsto_with(g: &mut Graph, cfg: Config) {
    let n = g.nodes.len();
    let mut ref_of = vec![NONE; n];
    for &(r, d) in &g.refers_to {
        ref_of[r as usize] = d;
    }
    let mut succ: Vec<Vec<u32>> = vec![Vec::new(); n];
    for e in &g.eog {
        succ[e.from as usize].push(e.to);
    }
    let mut parent = vec![NONE; n];
    for i in 0..n as u32 {
        let mut c = g.nodes[i as usize].first_child;
        while c != NONE {
            parent[c as usize] = i;
            c = g.nodes[c as usize].next;
        }
    }
    let mut fn_of = vec![NONE; n];
    for f in 0..n as u32 {
        if g.nodes[f as usize].kind == Kind::Function {
            let mut stack = vec![f];
            while let Some(x) = stack.pop() {
                fn_of[x as usize] = f;
                stack.extend(g.children(x));
            }
        }
    }
    let mut p = Pt {
        g,
        cfg,
        parent,
        ref_of,
        succ,
        synth: Vec::new(),
        mem_addrs: FxHashMap::default(),
        param_mvs: FxHashMap::default(),
        unknowns: FxHashMap::default(),
        origins: FxHashMap::default(),
        summaries: FxHashMap::default(),
        global_derefs: FxHashMap::default(),
        fn_of,
    };
    for f in 0..n as u32 {
        if p.g.nodes[f as usize].kind == Kind::Function && !p.succ[f as usize].is_empty() {
            p.analyze(f);
        }
        // the implicit constructor of a record returns its `this`
        if p.g.nodes[f as usize].kind == Kind::Constructor {
            let this: Vec<u32> = p
                .g
                .children(f)
                .filter(|&c| p.g.nodes[c as usize].kind == Kind::Variable && p.g.name_str(c) == "this")
                .collect();
            for t in this {
                p.g.add_dfg(t, f, Granularity::Full);
            }
        }
    }
}

impl<'g, 'a> Pt<'g, 'a> {
    // ---------------------------------------------------------------- node helpers

    fn kind(&self, n: N) -> Option<Kind> {
        if n >= SYNTH_BASE {
            None
        } else {
            Some(self.g.nodes[n as usize].kind)
        }
    }
    fn is(&self, n: N, k: Kind) -> bool {
        self.kind(n) == Some(k)
    }
    fn child(&self, n: N, i: usize) -> Option<N> {
        self.g.children(n).nth(i)
    }
    fn first(&self, n: N) -> N {
        self.g.nodes[n as usize].first_child
    }
    fn synth_of(&self, n: N) -> Option<&Synth> {
        if n >= SYNTH_BASE {
            Some(&self.synth[(n - SYNTH_BASE) as usize])
        } else {
            None
        }
    }
    fn is_memory(&self, n: N) -> bool {
        n >= SYNTH_BASE
    }
    fn is_pmv(&self, n: N) -> bool {
        self.synth_of(n).is_some_and(|s| s.class == SClass::Param)
    }
    fn is_unknown(&self, n: N) -> bool {
        self.synth_of(n).is_some_and(|s| s.class == SClass::Unknown)
    }
    /// A node with a memory address (`ValueDeclaration`).
    fn is_value_decl(&self, n: N) -> bool {
        matches!(
            self.kind(n),
            Some(Kind::Variable | Kind::Param | Kind::Field | Kind::Function | Kind::Enumerator)
        )
    }
    /// `Reference` and its subclasses.
    fn is_reference(&self, n: N) -> bool {
        matches!(
            self.kind(n),
            Some(Kind::Ref | Kind::PointerRef | Kind::PointerDeref | Kind::Member)
        )
    }

    fn new_synth(&mut self, class: SClass, name: &Name, global: bool) -> N {
        self.synth.push(Synth { class, local: name.local.clone(), full: name.full.clone(), global });
        let cls = match class {
            SClass::Memory => "MemoryAddress",
            SClass::Unknown => "UnknownMemoryValue",
            SClass::Param => "ParameterMemoryValue",
        };
        self.g.synth.push((cls, name.full.clone()));
        SYNTH_BASE + (self.synth.len() - 1) as u32
    }

    /// The name of any node.
    fn name_of(&self, n: N) -> Name {
        if let Some(s) = self.synth_of(n) {
            return Name { local: s.local.clone(), full: s.full.clone() };
        }
        Name::plain(self.g.name_str(n))
    }

    /// `getNodeName`
    fn node_name(&self, n: N) -> Name {
        if n == NONE {
            return Name::plain("");
        }
        if let Some(s) = self.synth_of(n) {
            if s.class == SClass::Unknown {
                let local = s.local.clone();
                return Name { full: format!("UnknownMemoryValue.{local}"), local };
            }
            return Name { local: s.local.clone(), full: s.full.clone() };
        }
        match self.g.nodes[n as usize].kind {
            Kind::Literal => {
                let v = self.literal_value(n);
                Name { local: v.clone(), full: v }
            }
            Kind::Field => {
                let l = local_of(self.g.name_str(n)).to_string();
                Name { local: l.clone(), full: l }
            }
            Kind::Binary => {
                let l = self.child(n, 0).map_or(NONE, |x| x);
                let r = self.child(n, 1).map_or(NONE, |x| x);
                let s = format!(
                    "{} {} {}",
                    self.node_name(l).local,
                    self.g.name_str(n),
                    self.node_name(r).local
                );
                Name { local: s.clone(), full: s }
            }
            _ => self.name_of(n),
        }
    }

    /// The `value` of a literal, as Kotlin prints it.
    fn literal_value(&self, n: N) -> String {
        let nd = self.g.nodes[n as usize];
        let t = String::from_utf8_lossy(&self.g.src[nd.start as usize..nd.end as usize]).into_owned();
        let digits: String = t.trim_end_matches(|c: char| matches!(c, 'u' | 'U' | 'l' | 'L')).to_string();
        if let Ok(v) = digits.parse::<i64>() {
            return v.to_string();
        }
        if let Some(h) = digits.strip_prefix("0x").or_else(|| digits.strip_prefix("0X")) {
            if let Ok(v) = i64::from_str_radix(h, 16) {
                return v.to_string();
            }
        }
        if digits.len() > 1 && digits.starts_with('0') {
            if let Ok(v) = i64::from_str_radix(&digits[1..], 8) {
                return v.to_string();
            }
        }
        if t.len() >= 2 && (t.starts_with('"') || t.starts_with('\'')) {
            return t[1..t.len() - 1].to_string();
        }
        t
    }

    fn ty(&self, n: N) -> &str {
        self.g.type_str(n).unwrap_or("UNKNOWN")
    }

    /// `isGlobal`
    fn is_global(&self, n: N) -> bool {
        if n >= SYNTH_BASE {
            return self.synth_of(n).is_some_and(|s| s.global);
        }
        match self.g.nodes[n as usize].kind {
            Kind::Variable => self.global_variable(n),
            Kind::Member => self.first(n) != NONE && self.is_global(self.first(n)),
            Kind::PointerDeref | Kind::PointerRef => {
                let input = self.first(n);
                input != NONE && self.is_global(self.remove_casts(input))
            }
            Kind::Ref => {
                let d = self.ref_of[n as usize];
                d != NONE
                    && (self.is(d, Kind::Function) || (self.is(d, Kind::Variable) && self.global_variable(d)))
            }
            Kind::Function => true,
            _ => false,
        }
    }

    fn global_variable(&self, v: N) -> bool {
        let mut p = self.parent[v as usize];
        while p != NONE {
            match self.g.nodes[p as usize].kind {
                Kind::Function | Kind::Constructor => return false,
                Kind::Record => return false,
                _ => {}
            }
            p = self.parent[p as usize];
        }
        true
    }

    fn remove_casts(&self, mut n: N) -> N {
        while self.is(n, Kind::Cast) {
            n = self.first(n);
        }
        n
    }

    /// `resolveMemberAccess`: the base that is not a member access, and the path of the field.
    fn resolve_member_access(&self, node: N) -> (N, Name) {
        let mut base = node;
        let mut path = String::new();
        while self.is(base, Kind::Member) {
            let name = self.g.name_str(base);
            let parts: Vec<&str> = name.split("::").flat_map(|p| p.split('.')).collect();
            let tmp = if parts.len() > 1 { parts[parts.len() - 1].to_string() } else { String::new() };
            path = if path.is_empty() { tmp } else { format!("{tmp}.{path}") };
            base = self.first(base);
        }
        let base_name = self.name_of(base);
        (base, Name::child(&path, &base_name))
    }

    /// `collectBasesAndOffsets`
    fn bases_and_offsets(&self, node: N) -> Vec<(N, Off)> {
        let mut ret = Vec::new();
        let mut n = node;
        loop {
            match self.kind(n) {
                Some(Kind::Member) => {
                    let base = self.first(n);
                    let d = self.ref_of[n as usize];
                    let off = if d != NONE {
                        Off::Node(d)
                    } else {
                        Off::Str(local_of(self.g.name_str(n)).to_string())
                    };
                    ret.push((base, off));
                    n = base;
                }
                Some(Kind::Subscript) => {
                    let arr = self.first(n);
                    let idx = self.child(n, 1).unwrap_or(NONE);
                    ret.push((arr, Off::Node(idx)));
                    n = arr;
                }
                Some(Kind::Cast) => n = self.first(n),
                _ => break,
            }
        }
        ret
    }

    // ---------------------------------------------------------------- synthetic nodes

    fn is_summary_node(&self, n: N) -> bool {
        self.origins.get(&n).is_some_and(|o| o.summary)
    }

    /// `syntheticNodeFor`
    fn synthetic_for(&mut self, base: N, name: &Name, is_value: bool, class: SClass, global: bool) -> N {
        let origin = self.origins.get(&base).copied();
        let root = origin.map_or(base, |o| o.root);
        let depth = origin.map_or(0, |o| o.depth) + 1;
        let within = self.cfg.max_synthetic_chain_depth.is_none_or(|m| depth <= m);
        if within {
            if let Some(&n) = self.unknowns.get(&(base, name.full.clone())) {
                return n;
            }
            let n = self.new_synth(class, name, global);
            self.unknowns.insert((base, name.full.clone()), n);
            self.origins.insert(n, Origin { root, depth, summary: false });
            return n;
        }
        let k = (root, name.full.clone(), is_value);
        if let Some(&n) = self.summaries.get(&k) {
            return n;
        }
        let n = self.new_synth(class, name, global);
        self.summaries.insert(k, n);
        self.origins.insert(
            n,
            Origin { root, depth: self.cfg.max_synthetic_chain_depth.unwrap_or(0), summary: true },
        );
        n
    }

    /// `nodesCreatingUnknownValues.computeIfAbsent(UnknownValueKey(node, name))`
    fn unknown_value(&mut self, node: N, name: &Name) -> N {
        if let Some(&n) = self.unknowns.get(&(node, name.full.clone())) {
            return n;
        }
        let n = self.new_synth(SClass::Unknown, name, false);
        self.unknowns.insert((node, name.full.clone()), n);
        n
    }

    // ---------------------------------------------------------------- the state fetchers

    /// The memory addresses of a value declaration or binary operator (created on demand).
    fn memory_addresses(&mut self, n: N) -> Vec<N> {
        if self.mem_addrs.get(&n).is_none_or(|v| v.is_empty()) {
            let name = self.name_of(n);
            let global = self.is_global(n);
            let a = self.new_synth(SClass::Memory, &name, global);
            self.mem_addrs.entry(n).or_default().push(a);
        }
        self.mem_addrs[&n].clone()
    }

    /// `fetchFieldAddresses`
    fn fetch_field_addresses(&mut self, st: &mut State, bases: &Set<N>, name: &Name) -> Set<N> {
        let mut ret = Set::new();
        let name = self.normalize_path(name);
        for &addr in bases {
            let mut found = false;
            if let Some(e) = st.decl.get(&addr) {
                for &c in &e.addrs {
                    if self.name_of(c).local == name.local {
                        ret.insert(c);
                        found = true;
                    }
                }
            }
            if !found {
                let global = self.is_global(addr);
                let new = self.synthetic_for(addr, &name, false, SClass::Memory, global);
                if !self.is_unknown(addr) {
                    let e = st.decl.entry(addr).or_insert_with(|| DEntry {
                        addrs: Set::from([addr]),
                        ..DEntry::default()
                    });
                    e.addrs.insert(new);
                }
                ret.insert(new);
            }
        }
        ret
    }

    /// `normalizeFieldAccessPath`: widens very deep access paths to a summary segment.
    fn normalize_path(&self, name: &Name) -> Name {
        let Some(max) = self.cfg.max_field_access_path_depth else { return name.clone() };
        let segs: Vec<&str> = name.full.split('.').collect();
        if segs.len() <= max {
            return name.clone();
        }
        let keep = max.saturating_sub(1).max(1);
        let kept = segs[..keep.min(segs.len())].join(".");
        Name { local: "<summary>".to_string(), full: format!("{kept}.<summary>") }
    }

    /// `fetchValueFromDeclarationState`
    fn fetch_decl_value(&mut self, st: &mut State, node: N, fetch_fields: bool, excl_short: bool) -> Vec<Fetched> {
        let mut ret: Vec<Fetched> = Vec::new();
        let mut seen = Set::new();
        let mut add = |ret: &mut Vec<Fetched>, v: N, short: bool| {
            if seen.insert((v, short)) {
                ret.push(Fetched { value: v, short });
            }
        };
        if self.is_global(node) {
            if self.is(node, Kind::Function) {
                add(&mut ret, node, false);
                return ret;
            }
            if let Some(el) = self.global_derefs.get(&node) {
                for &(v, _) in el {
                    add(&mut ret, v, false);
                }
            } else if self.is_unknown(node) {
                add(&mut ret, node, false);
            } else {
                let name = self.node_name(node);
                let global = true;
                let e = self.synthetic_for(node, &name, true, SClass::Unknown, global);
                self.global_derefs.insert(node, Set::from([(e, false)]));
                add(&mut ret, e, false);
            }
            return ret;
        }
        let entry = st.decl.get(&node);
        let usable = entry.is_some_and(|e| e.vals.iter().any(|p| !excl_short || !p.1));
        if !usable {
            if self.is_unknown(node) {
                add(&mut ret, node, false);
            } else {
                let name = self.node_name(node);
                let e = self.synthetic_for(node, &name, true, SClass::Unknown, false);
                let d = st.decl.entry(node).or_insert_with(|| DEntry {
                    addrs: Set::from([node]),
                    ..DEntry::default()
                });
                d.vals.insert((e, false));
                add(&mut ret, e, false);
            }
        } else {
            for &(v, s) in &entry.unwrap().vals.clone() {
                if excl_short && s {
                    continue;
                }
                add(&mut ret, v, s);
            }
        }
        if fetch_fields {
            let node_addrs = self.get_addresses(st, node, node);
            let fields: Vec<N> = st.decl.get(&node).map(|e| e.addrs.iter().copied().collect()).unwrap_or_default();
            for f in fields {
                if f == node || node_addrs.contains(&f) {
                    continue;
                }
                if let Some(e) = st.decl.get(&f) {
                    for &(v, s) in &e.vals.clone() {
                        if (excl_short && !s) || !excl_short {
                            add(&mut ret, v, s);
                        }
                    }
                }
            }
        }
        ret
    }

    fn collect_last_writes(&self, st: &State, addrs: &Set<N>) -> Set<Key> {
        let mut ret = Set::new();
        for a in addrs {
            if let Some(e) = st.decl.get(a) {
                ret.extend(e.lw.iter().cloned());
            }
        }
        ret
    }

    /// `getLastWrites`
    fn get_last_writes(&mut self, st: &mut State, node: N) -> Set<Key> {
        use Kind::*;
        if self.is(node, PointerRef) {
            let input = self.first(node);
            return match self.kind(input) {
                Some(Subscript | Member) => {
                    let (base, field_name) = if self.is(input, Member) {
                        (self.first(input), self.node_name(self.ref_of[input as usize]))
                    } else {
                        (self.first(input), self.node_name(self.child(input, 1).unwrap_or(NONE)))
                    };
                    let fa = self.fetch_field_addresses(st, &Set::from([base]), &field_name);
                    let addrs = if fa.is_empty() || fa.iter().all(|a| !st.decl.contains_key(a)) {
                        self.get_addresses(st, base, base)
                    } else {
                        fa
                    };
                    let mut ret = Set::new();
                    for a in addrs {
                        if let Some(e) = st.decl.get(&a) {
                            ret.extend(e.lw.iter().cloned());
                        }
                    }
                    ret
                }
                Some(Ref | PointerRef | PointerDeref) => {
                    self.get_addresses(st, input, input).into_iter().map(|a| key(a, Props::default())).collect()
                }
                _ => Set::new(),
            };
        }
        if self.is_global(node) {
            return match self.kind(node) {
                Some(Member) => {
                    let (base, _) = self.resolve_member_access(node);
                    let b = if self.is(base, Ref) && self.ref_of[base as usize] != NONE {
                        self.ref_of[base as usize]
                    } else {
                        base
                    };
                    Set::from([key(b, Props::default())])
                }
                Some(Ref) => {
                    let d = self.ref_of[node as usize];
                    Set::from([key(if d != NONE { d } else { node }, Props::default())])
                }
                _ => Set::from([key(node, Props::default())]),
            };
        }
        match self.kind(node) {
            Some(PointerDeref) => {
                let mut ret = Set::new();
                for addr in self.get_addresses(st, node, node) {
                    if self.is_global(addr) {
                        if let Some(el) = self.global_derefs.get(&addr) {
                            for &(v, s) in el {
                                ret.insert(key(v, Props::fs(s)));
                            }
                        }
                    } else {
                        let lw = st.decl.get(&addr).map(|e| e.lw.clone()).unwrap_or_default();
                        if !lw.is_empty() {
                            ret.extend(lw);
                        } else {
                            let name = Name { local: format!("{}.derefvalue", self.node_name(addr).local), full: format!("{}.derefvalue", self.node_name(addr).local) };
                            let u = self.synthetic_for(addr, &name, true, SClass::Unknown, false);
                            ret.insert(key(u, Props::default()));
                        }
                    }
                }
                ret
            }
            Some(Subscript) => {
                let partial = self.node_name(self.child(node, 1).unwrap_or(NONE)).local;
                let addrs = self.get_addresses(st, node, node);
                let mut ret = Set::new();
                for a in addrs {
                    if let Some(e) = st.decl.get(&a) {
                        for k in &e.lw {
                            let mut p = k.p.clone();
                            if let Some(Off::Node(t)) = &p.off {
                                if self.is(*t, Field) && local_of(self.g.name_str(*t)) == partial {
                                    p.off = None;
                                }
                            }
                            ret.insert(key(k.n, p));
                        }
                    }
                }
                ret
            }
            Some(Member) => {
                let addrs = self.get_addresses(st, node, node);
                let lw = self.collect_last_writes(st, &addrs);
                if lw.is_empty() {
                    let d = self.ref_of[node as usize];
                    if d != NONE {
                        Set::from([key(d, Props::default())])
                    } else {
                        lw
                    }
                } else {
                    lw
                }
            }
            _ if self.is_pmv(node) => {
                let entries = st.decl.get(&node).map(|e| e.lw.clone()).unwrap_or_default();
                if !entries.is_empty() {
                    entries
                } else {
                    let want = format!("deref{}", self.synth_of(node).unwrap().local);
                    self.pmv_values(node)
                        .into_iter()
                        .filter(|&v| self.synth_of(v).is_some_and(|s| s.local == want))
                        .map(|v| key(v, Props::default()))
                        .collect()
                }
            }
            _ => {
                let addrs = self.get_addresses(st, node, node);
                self.collect_last_writes(st, &addrs)
            }
        }
    }

    /// `node.memoryValues` of a `ParameterMemoryValue`: nothing outside of its function.
    fn pmv_values(&self, _pmv: N) -> Vec<N> {
        Vec::new()
    }

    /// `getAddresses`
    fn get_addresses(&mut self, st: &mut State, node: N, start: N) -> Set<N> {
        use Kind::*;
        if self.is_pmv(node) {
            let r: Set<N> = self.mem_addrs.get(&node).map(|v| v.iter().copied().collect()).unwrap_or_default();
            if !r.is_empty() {
                return r;
            }
            return st.decl.get(&node).map(|e| e.addrs.clone()).unwrap_or_default();
        }
        if self.is_memory(node) {
            return Set::from([node]);
        }
        if self.is_value_decl(node) {
            return self.memory_addresses(node).into_iter().collect();
        }
        match self.g.nodes[node as usize].kind {
            PointerRef => Set::new(),
            PointerDeref => {
                let input = self.first(node);
                let vals: Vec<N> = if input != start && node != start {
                    st.gs.get(&input).map(|e| e.vals.iter().map(|k| k.n).collect()).unwrap_or_default()
                } else {
                    self.get_values(st, input, start).into_iter().map(|v| v.0).collect()
                };
                let mut ret = Set::new();
                for v in vals {
                    if self.is(v, Binary)
                        && self.g.name_str(v) == "+"
                        && matches!(self.kind(self.child(v, 0).unwrap_or(NONE)), Some(Ref | Cast))
                        && self.is(self.child(v, 1).unwrap_or(NONE), Literal)
                    {
                        let arr = self.remove_casts(self.child(v, 0).unwrap());
                        let idx = self.child(v, 1).unwrap();
                        ret.extend(self.subscript_addresses(st, arr, idx, node, start));
                    } else {
                        ret.insert(v);
                    }
                }
                ret
            }
            Member => {
                let (base, name) = self.resolve_member_access(node);
                let ba = self.get_addresses(st, base, start);
                self.fetch_field_addresses(st, &ba, &name)
            }
            Ref => {
                let d = self.ref_of[node as usize];
                if d == NONE {
                    return Set::new();
                }
                if self.is(d, Function) {
                    self.global_derefs.insert(d, Set::from([(d, false)]));
                    return Set::from([d]);
                }
                if self.is_value_decl(d) {
                    if self.mem_addrs.get(&d).is_none_or(|v| v.is_empty()) {
                        let name = self.name_of(node);
                        let global = self.is_global(node);
                        let a = self.new_synth(SClass::Memory, &name, global);
                        self.mem_addrs.entry(d).or_default().push(a);
                    }
                    return self.mem_addrs[&d].iter().copied().collect();
                }
                Set::new()
            }
            Cast => {
                let e = self.first(node);
                self.get_addresses(st, e, start)
            }
            Subscript => {
                let arr = self.first(node);
                let idx = self.child(node, 1).unwrap_or(NONE);
                self.subscript_addresses(st, arr, idx, node, start)
            }
            Binary => self.memory_addresses(node).into_iter().collect(),
            _ => Set::from([node]),
        }
    }

    /// The addresses of the subscription `arr[idx]` (`node` is the subscription, or the node standing
    /// in for it).
    fn subscript_addresses(&mut self, st: &mut State, arr: N, idx: N, node: N, start: N) -> Set<N> {
        let local = self.node_name(idx).local;
        if local == "0" {
            return self.get_values(st, arr, start).into_iter().map(|v| v.0).collect();
        }
        let base_values: Set<N> = if start != node {
            st.gs.get(&arr).map(|e| e.vals.iter().map(|k| k.n).collect()).unwrap_or_default()
        } else {
            self.get_values(st, arr, start).into_iter().map(|v| v.0).collect()
        };
        let mut ret = Set::new();
        for v in base_values {
            let parent_name = self.node_name(v);
            let r = self.fetch_field_addresses(st, &Set::from([v]), &Name::child(&local, &parent_name));
            ret.extend(r);
        }
        ret
    }

    /// `getValues`
    fn get_values(&mut self, st: &mut State, node: N, start: N) -> Set<(N, bool)> {
        use Kind::*;
        if self.is_value_decl(node) {
            let addrs = self.memory_addresses(node);
            let mut ret = Set::new();
            for a in addrs {
                for f in self.fetch_decl_value(st, a, false, false) {
                    ret.insert((f.value, false));
                }
            }
            return ret;
        }
        if self.is_memory(node) {
            return self.fetch_decl_value(st, node, false, false).into_iter().map(|f| (f.value, f.short)).collect();
        }
        match self.g.nodes[node as usize].kind {
            PointerRef => {
                let input = self.first(node);
                self.get_addresses(st, input, input).into_iter().map(|a| (a, false)).collect()
            }
            PointerDeref => {
                let input = self.first(node);
                let input_vals: Vec<N> = if self.is(input, Ref) || self.is_reference(input) && !self.is(input, Member) {
                    st.gs.get(&input).map(|e| e.vals.iter().map(|k| k.n).collect()).unwrap_or_default()
                } else {
                    self.get_values(st, input, input).into_iter().map(|v| v.0).collect()
                };
                let mut ret = Set::new();
                let child_of_start = self.parent[node as usize] == start && start != NONE;
                let _ = child_of_start;
                if node != start && !self.is_ast_child(start, node) {
                    for v in input_vals {
                        if let Some(e) = st.gs.get(&v) {
                            for k in &e.vals {
                                ret.insert((k.n, k.p.fs == Some(true)));
                            }
                        }
                    }
                } else {
                    for v in input_vals {
                        for f in self.fetch_decl_value(st, v, false, true) {
                            ret.insert((f.value, f.short));
                        }
                    }
                }
                ret
            }
            Call => self.fetch_decl_value(st, node, false, false).into_iter().map(|f| (f.value, f.short)).collect(),
            Member => {
                let (base, name) = self.resolve_member_access(node);
                let ba = self.get_addresses(st, base, start);
                let fa = self.fetch_field_addresses(st, &ba, &name);
                if !fa.is_empty() {
                    self.field_value_or_base(st, &fa, &ba, node, &name)
                } else {
                    let nn = Name::child(&self.node_name(node).local, &self.name_of(base));
                    let u = self.synthetic_for(node, &nn, true, SClass::Unknown, false);
                    Set::from([(u, false)])
                }
            }
            Ref => {
                if node != start && !self.is_ast_child(start, node) {
                    return st
                        .gs
                        .get(&node)
                        .map(|e| e.vals.iter().map(|k| (k.n, k.p.fs == Some(true))).collect())
                        .unwrap_or_default();
                }
                let mut ret = Set::new();
                for addr in self.get_addresses(st, node, start) {
                    if self.is_global(node) {
                        for f in self.fetch_decl_value(st, addr, false, false) {
                            ret.insert((f.value, false));
                        }
                    } else {
                        for v in self.get_values(st, addr, start) {
                            // skip values that come from the assignment this reference is part of
                            let mut p = v.0;
                            while p != NONE && p < SYNTH_BASE && !self.is(p, Assign) {
                                p = self.parent[p as usize];
                            }
                            if p == NONE || p >= SYNTH_BASE || !self.is_ast_child_or_self(p, node) {
                                ret.insert(v);
                            }
                        }
                    }
                }
                ret
            }
            Cast => {
                let e = self.first(node);
                self.get_values(st, e, start)
            }
            Subscript => {
                let arr = self.first(node);
                let idx = self.child(node, 1).unwrap_or(NONE);
                let offset = self.node_name(idx);
                let base_addrs: Set<N> =
                    self.get_values(st, arr, start).into_iter().filter(|v| !v.1).map(|v| v.0).collect();
                let fa = self.get_addresses(st, node, start);
                self.field_value_or_base(st, &fa, &base_addrs, node, &offset)
            }
            _ => {
                if st.has_decl_value(node, true) {
                    self.fetch_decl_value(st, node, false, false).into_iter().map(|f| (f.value, f.short)).collect()
                } else {
                    Set::from([(node, false)])
                }
            }
        }
    }

    /// `node in startNode.astChildren` (direct children; Kotlin `astChildren` is only the direct ones).
    fn is_ast_child(&self, start: N, node: N) -> bool {
        start < SYNTH_BASE && start != NONE && node < SYNTH_BASE && self.parent[node as usize] == start
    }

    /// `node in SubgraphWalker.flattenAST(root)`
    fn is_ast_child_or_self(&self, root: N, node: N) -> bool {
        let mut n = node;
        while n != NONE {
            if n == root {
                return true;
            }
            n = self.parent[n as usize];
        }
        false
    }

    fn field_value_or_base(
        &mut self,
        st: &mut State,
        field_addrs: &Set<N>,
        base_addrs: &Set<N>,
        node: N,
        field_name: &Name,
    ) -> Set<(N, bool)> {
        let mut ret = Set::new();
        for &fa in field_addrs {
            if st.has_decl_value(fa, true) {
                for f in self.fetch_decl_value(st, fa, false, false) {
                    ret.insert((f.value, f.short));
                }
            } else {
                for &ba in base_addrs {
                    if st.has_decl_value(ba, true) {
                        for f in self.fetch_decl_value(st, ba, false, false) {
                            ret.insert((f.value, f.short));
                        }
                    }
                }
                let umv = self.unknown_value(node, field_name);
                ret.insert((umv, false));
            }
        }
        ret
    }

    // ---------------------------------------------------------------- the handlers

    fn analyze(&mut self, f: N) {
        let mut st = State::default();
        let addrs = self.get_addresses(&mut st, f, f);
        st.push_decl(f, DEntry { addrs, ..DEntry::default() });
        let st = self.transfer(f, st);

        // fixpoint over the EOG
        let n = self.g.nodes.len();
        let mut inn: FxHashMap<N, State> = FxHashMap::default();
        let mut outs: FxHashMap<N, State> = FxHashMap::default();
        let mut queue: VecDeque<N> = VecDeque::new();
        let mut queued = vec![false; n];
        for &s in &self.succ[f as usize].clone() {
            inn.insert(s, st.clone());
            queue.push_back(s);
            queued[s as usize] = true;
        }
        while let Some(node) = queue.pop_front() {
            queued[node as usize] = false;
            let state = inn[&node].clone();
            let out = self.transfer(node, state);
            outs.insert(node, out.clone());
            for &s in &self.succ[node as usize].clone() {
                let ch = match inn.get_mut(&s) {
                    Some(slot) => slot.join(&out),
                    None => {
                        inn.insert(s, out.clone());
                        true
                    }
                };
                if ch && !queued[s as usize] {
                    queued[s as usize] = true;
                    queue.push_back(s);
                }
            }
        }
        let mut fin = State::default();
        let mut any = false;
        for (&node, out) in &outs {
            if self.succ[node as usize].is_empty() {
                fin.join(out);
                any = true;
            }
        }
        if !any {
            fin = st;
            for out in outs.values() {
                fin.join(out);
            }
        }
        self.draw(&fin);
    }

    /// `drawDataflowEdges`
    fn draw(&mut self, fin: &State) {
        for (&key, e) in &fin.gs {
            if key >= SYNTH_BASE {
                continue;
            }
            if matches!(self.g.nodes[key as usize].kind, Kind::Variable | Kind::Param | Kind::Field | Kind::Binary)
                && !(self.is(key, Kind::Variable) && self.global_variable(key))
            {
                let add: Vec<N> = e.addrs.iter().copied().filter(|&a| self.is_memory(a)).collect();
                let v = self.mem_addrs.entry(key).or_default();
                for a in add {
                    if !v.contains(&a) {
                        v.push(a);
                    }
                }
            }
            for k in &e.lw {
                let gran = match &k.p.off {
                    Some(Off::Str(s)) => Granularity::Partial(s.clone()),
                    Some(Off::Node(t)) => Granularity::Partial(self.partial_detail(*t)),
                    None => Granularity::Full,
                };
                self.g.add_dfg(k.n, key, gran);
            }
        }
    }

    fn partial_detail(&self, t: N) -> String {
        if t >= SYNTH_BASE {
            return "node".to_string();
        }
        let cls = self.g.nodes[t as usize].kind.class_name();
        let name = if self.is(t, Kind::Literal) { String::new() } else { self.g.name_str(t).to_string() };
        format!("{cls}:{name}")
    }

    /// `transferInternal`
    fn transfer(&mut self, node: N, mut st: State) -> State {
        let existing = st.decl.get(&node).cloned().unwrap_or_default();
        st.push_decl(node, existing);
        match self.g.nodes[node as usize].kind {
            Kind::Function => self.handle_function(node, &mut st),
            Kind::Literal => {}
            Kind::Variable => self.handle_declaration(node, &mut st),
            Kind::Assign => self.handle_assign(node, &mut st),
            Kind::Unary => self.handle_unary(node, &mut st),
            Kind::Call => {}
            Kind::Return => self.handle_return(node, &mut st),
            Kind::Ref
            | Kind::PointerRef
            | Kind::PointerDeref
            | Kind::Member
            | Kind::Binary
            | Kind::Subscript
            | Kind::Cast
            | Kind::ShortCircuit
            | Kind::Conditional
            | Kind::Sizeof
            | Kind::InitList => self.handle_expression(node, &mut st),
            _ => {}
        }
        st
    }

    fn handle_function(&mut self, f: N, st: &mut State) {
        let params: Vec<N> = self.g.children(f).filter(|&c| self.is(c, Kind::Param)).collect();
        for p in params {
            if self.param_mvs.get(&p).is_none_or(|v| v.is_empty()) {
                self.initialize_parameter(f, p, st);
            }
        }
    }

    fn is_pointer_like(&self, ty: &str) -> bool {
        ty.ends_with('*') || ty.ends_with(']')
    }

    /// `initializeParameter`
    fn initialize_parameter(&mut self, f: N, param: N, st: &mut State) {
        let ty = self.ty(param).to_string();
        let depth = 2usize;
        let param_depth = if self.is_pointer_like(&ty)
            || ty == "UNKNOWN"
            || numeric(&ty).is_some_and(|(_, w)| w == self.cfg.address_length)
        {
            depth
        } else {
            0
        };
        let mut src: N = param;
        let mut addresses = self.get_addresses(st, src, src);
        let mut prev: Set<N> = Set::new();
        let fname = self.name_of(f);
        let pname = self.name_of(param);
        for pd in 0..=param_depth {
            let pmv_name = format!("{}value", "deref".repeat(pd));
            let full = format!("{}.{}.{}", fname.full, pname.local, pmv_name);
            let name = Name { local: pmv_name.clone(), full };
            let pmv = self.new_synth(SClass::Param, &name, false);
            if self.is_memory(src) {
                self.mem_addrs.insert(pmv, vec![src]);
            }
            if src == param {
                st.push(
                    src,
                    GEntry {
                        addrs: addresses.clone(),
                        vals: Set::from([key(pmv, Props::default())]),
                        lw: Set::new(),
                    },
                );
                self.param_mvs.entry(param).or_default().push(pmv);
            } else {
                for &a in &addresses {
                    if self.is_pmv(a) {
                        st.push(
                            a,
                            GEntry {
                                addrs: prev.clone(),
                                vals: Set::from([key(pmv, Props::default())]),
                                lw: Set::new(),
                            },
                        );
                    }
                }
                st.push(pmv, GEntry { addrs: addresses.clone(), ..GEntry::default() });
                st.push(
                    param,
                    GEntry {
                        vals: Set::from([key(
                            pmv,
                            Props { off: Some(Off::Str(pmv_name.clone())), ..Props::default() },
                        )]),
                        ..GEntry::default()
                    },
                );
                self.param_mvs.entry(param).or_default().push(pmv);
            }
            let el = if src == param {
                DEntry {
                    addrs: prev.clone(),
                    vals: Set::from([(pmv, false)]),
                    lw: Set::from([key(src, Props::default())]),
                }
            } else {
                DEntry {
                    addrs: addresses.clone(),
                    vals: Set::from([(pmv, false)]),
                    lw: Set::from([key(pmv, Props::default())]),
                }
            };
            for &a in &addresses {
                st.push_decl(a, el.clone());
            }
            prev = addresses;
            src = pmv;
            addresses = Set::from([pmv]);
        }
    }

    /// `handleReturn`: the function gets a data flow edge from the return
    fn handle_return(&mut self, ret: N, st: &mut State) {
        let f = self.fn_of[ret as usize];
        if f != NONE {
            st.push(f, GEntry { lw: Set::from([key(ret, Props::default())]), ..GEntry::default() });
        }
    }

    fn initializer_of(&self, var: N) -> Option<N> {
        self.g.children(var).find(|&c| {
            !matches!(self.g.nodes[c as usize].kind, Kind::TypeReference | Kind::Other)
        })
    }

    /// `handleDeclaration` for a variable
    fn handle_declaration(&mut self, var: N, st: &mut State) {
        let mut entries: BTreeMap<N, DEntry> = BTreeMap::new();
        let addresses = self.get_addresses(st, var, var);
        let mut values: Set<Key> = Set::new();
        let lw: Set<Key> = Set::from([key(var, Props::fs(false))]);
        for &a in &addresses {
            entries
                .entry(a)
                .or_insert_with(|| DEntry { addrs: addresses.clone(), vals: Set::new(), lw: lw.clone() });
        }
        let init = self.initializer_of(var);
        let is_static = false;
        if !is_static && init.is_none() && !self.global_variable(var) {
            let name = self.node_name(var);
            let unknown = self.unknown_value(var, &name);
            for &a in &addresses {
                if let Some(e) = entries.get_mut(&a) {
                    e.vals.insert((unknown, false));
                }
            }
        }
        if let Some(init) = init {
            if self.is(init, Kind::Literal) {
                values.insert(key(init, Props::default()));
                for e in entries.values_mut() {
                    e.vals.extend(values.iter().map(|k| (k.n, false)));
                }
                entries
                    .entry(init)
                    .or_insert_with(|| DEntry { addrs: addresses.clone(), ..DEntry::default() });
            } else {
                let ini = self.remove_casts(init);
                match self.kind(ini) {
                    Some(Kind::PointerRef | Kind::PointerDeref) => {
                        let input = self.first(ini);
                        self.handle_expression(input, st)
                    }
                    Some(Kind::Call) => {}
                    _ => self.handle_expression(ini, st),
                }
                if self.is(ini, Kind::InitList) {
                    self.init_list_declaration(var, ini, &addresses, &mut entries, st);
                } else {
                    for v in self.get_values(st, ini, ini) {
                        values.insert(key(v.0, Props::default()));
                    }
                    for e in entries.values_mut() {
                        e.vals.extend(values.iter().map(|k| (k.n, false)));
                    }
                }
            }
        }
        st.push(var, GEntry { addrs: addresses, vals: values, lw: Set::new() });
        for (k, e) in entries {
            st.push_decl(k, e);
        }
    }

    /// The initializer list branch of `handleDeclaration`.
    fn init_list_declaration(
        &mut self,
        var: N,
        ini: N,
        addresses: &Set<N>,
        entries: &mut BTreeMap<N, DEntry>,
        st: &mut State,
    ) {
        let inits: Vec<N> = self.g.children(ini).collect();
        if inits.iter().any(|&i| !self.is(i, Kind::Assign)) {
            let mut field_addrs: Set<N> = Set::new();
            let n = self.cfg.max_array_initializers.map_or(inits.len(), |m| inits.len().min(m));
            for (i, &field_val) in inits.iter().take(n).enumerate() {
                let parent_name = self.node_name(var);
                let name = Name::child(&i.to_string(), &parent_name);
                for fa in self.fetch_field_addresses(st, addresses, &name) {
                    field_addrs.insert(fa);
                    let e = entries.entry(fa).or_default();
                    e.addrs.insert(fa);
                    e.vals.insert((field_val, false));
                    e.lw.insert(key(field_val, Props::default()));
                }
            }
            for &a in addresses {
                if let Some(e) = entries.get_mut(&a) {
                    e.addrs.extend(field_addrs.iter().copied());
                    let zero: Vec<N> =
                        field_addrs.iter().copied().filter(|&f| self.name_of(f).local == "0").collect();
                    if zero.len() == 1 {
                        e.vals.insert((zero[0], false));
                    }
                }
            }
        } else {
            // initializers that are assigns were handled on the way; the record is not known here, so
            // all the field addresses of the declaration state are the values
            let mut fa: Set<N> = Set::new();
            for &a in addresses {
                if let Some(e) = st.decl.get(&a) {
                    fa.extend(e.addrs.iter().copied().filter(|&x| x != a));
                }
            }
            for &a in addresses {
                if let Some(e) = entries.get_mut(&a) {
                    e.vals.extend(fa.iter().map(|&x| (x, false)));
                }
            }
        }
    }

    /// `handleUnaryOperator`
    fn handle_unary(&mut self, node: N, st: &mut State) {
        let op = self.g.name_str(node).to_string();
        let input = self.first(node);
        if (op == "++" || op == "--") && self.is_reference(input) {
            for addr in self.get_addresses(st, input, input) {
                let new_value = (node, false);
                let new_lw = key(input, Props::fs(false));
                st.decl.insert(
                    addr,
                    DEntry {
                        addrs: Set::from([addr]),
                        vals: Set::from([new_value]),
                        lw: Set::from([new_lw]),
                    },
                );
            }
        }
        let addrs = self.get_addresses(st, node, node);
        let vals: Set<Key> =
            self.get_values(st, node, node).into_iter().map(|v| key(v.0, Props::default())).collect();
        st.push(node, GEntry { addrs, vals, lw: Set::new() });
    }

    /// `handleAssign`
    fn handle_assign(&mut self, node: N, st: &mut State) {
        let kids: Vec<N> = self.g.children(node).collect();
        let (Some(&lhs), Some(&rhs)) = (kids.first(), kids.get(1)) else { return };
        let mut sources: Set<(N, bool)> = Set::new();
        for v in self.get_values(st, rhs, rhs) {
            sources.insert(v);
        }
        let mut destinations: Vec<N> = Vec::new();
        if self.is(lhs, Kind::InitList) {
            destinations.extend(self.g.children(lhs));
        } else {
            destinations.push(lhs);
        }
        let mut dest_addrs: Set<N> = Set::new();
        for &d in &destinations {
            dest_addrs.extend(self.get_addresses(st, d, d));
        }
        let last_writes: Set<Key> = destinations.iter().map(|&d| key(d, Props::fs(false))).collect();
        if self.is(rhs, Kind::Literal) {
            st.push_decl(rhs, DEntry { addrs: dest_addrs.clone(), ..DEntry::default() });
        }
        let compound = self.g.name_str(node) != "=";
        let dest_lws: Option<Vec<(N, Set<Key>)>> = if compound {
            Some(vec![(lhs, self.get_last_writes(st, lhs))])
        } else {
            None
        };
        self.update_base_last_writes(st, &destinations);
        self.update_values(st, &sources, &destinations, &dest_addrs, &last_writes);
        if let Some(l) = dest_lws {
            for (d, lw) in l {
                st.push(d, GEntry { lw, ..GEntry::default() });
            }
        }
        if self.parent[node as usize] != NONE && self.is(self.parent[node as usize], Kind::Binary) {
            st.push(node, GEntry { lw: Set::from([key(rhs, Props::default())]), ..GEntry::default() });
        }
    }

    /// `updateBaseLastWrites`
    fn update_base_last_writes(&mut self, st: &mut State, destinations: &[N]) {
        let mut all = Vec::new();
        for &d in destinations {
            all.extend(self.bases_and_offsets(d));
        }
        for (base, off) in all {
            for ba in self.get_addresses(st, base, base) {
                let e = st
                    .decl
                    .entry(ba)
                    .or_insert_with(|| DEntry { addrs: Set::from([ba]), ..DEntry::default() });
                let np = Props { fs: Some(false), off: Some(off.clone()), deref: 0 };
                e.lw.retain(|k| !k.p.contains_all(&np));
                e.lw.insert(key(base, np));
            }
        }
    }

    /// `updateValues` (for assignments: all sources are full writes)
    fn update_values(
        &mut self,
        st: &mut State,
        sources: &Set<(N, bool)>,
        destinations: &[N],
        dest_addrs: &Set<N>,
        last_writes: &Set<Key>,
    ) {
        let mapped: Set<Key> = sources.iter().map(|&(n, s)| key(n, Props::fs(s))).collect();
        for &da in dest_addrs {
            if !self.is_global(da) {
                let current = st.decl.get(&da).map(|e| e.addrs.clone()).unwrap_or_else(|| Set::from([da]));
                let is_summary = self.is_summary_node(da);
                if !is_summary {
                    st.decl.insert(da, DEntry { addrs: current, vals: sources.clone(), lw: last_writes.clone() });
                } else {
                    st.push_decl(da, DEntry { addrs: current, vals: sources.clone(), lw: last_writes.clone() });
                }
                let mut new_lw = last_writes.clone();
                new_lw.retain(|lw| {
                    !(sources.iter().any(|s| s.0 == lw.n && lw.p.fs == Some(s.1))
                        || destinations.contains(&lw.n))
                });
                for &d in destinations {
                    st.gs.insert(d, GEntry { addrs: dest_addrs.clone(), vals: mapped.clone(), lw: new_lw.clone() });
                }
            } else {
                for &d in destinations {
                    let e = st.gs.entry(d).or_default();
                    e.lw.extend(mapped.iter().cloned());
                }
            }
        }
    }

    /// `handleExpression`
    fn handle_expression(&mut self, node: N, st: &mut State) {
        let access = match self.g.nodes[node as usize].kind {
            Kind::Ref | Kind::PointerRef | Kind::PointerDeref | Kind::Member | Kind::Binary => {
                Some(self.g.nodes[node as usize].access)
            }
            Kind::Subscript => {
                let arr = self.first(node);
                if self.is_reference(arr) {
                    Some(self.g.nodes[arr as usize].access)
                } else {
                    None
                }
            }
            _ => None,
        };
        if matches!(access, Some(Access::Read | Access::ReadWrite)) {
            let addrs = self.get_addresses(st, node, node);
            let values: Vec<N> =
                self.get_values(st, node, node).into_iter().filter(|v| !v.1).map(|v| v.0).collect();
            let mut prev = self.get_last_writes(st, node);
            let parent_write = self.parent[node as usize] != NONE
                && self.is(self.parent[node as usize], Kind::PointerDeref)
                && self.g.nodes[self.parent[node as usize] as usize].access == Access::Write;
            if self.cfg.draw_current_deref_dfg && !parent_write {
                let mut processed: Set<N> = Set::new();
                for &value in &values {
                    if st.has_decl_value(value, true) {
                        for k in self.get_last_writes(st, value) {
                            if k.p.fs == Some(true) {
                                continue;
                            }
                            prev.insert(key(k.n, Props { deref: 1, ..k.p.clone() }));
                        }
                        for f in self.fetch_decl_value(st, value, false, true) {
                            let dv = f.value;
                            if !processed.insert(dv) {
                                continue;
                            }
                            if !st.has_decl_value(dv, true) {
                                continue;
                            }
                            for k in self.get_last_writes(st, dv) {
                                if k.p.fs == Some(true) {
                                    continue;
                                }
                                prev.insert(key(k.n, Props { deref: 2, ..k.p.clone() }));
                            }
                        }
                    }
                }
            }
            st.push(
                node,
                GEntry {
                    addrs,
                    vals: values.into_iter().map(|v| key(v, Props::default())).collect(),
                    lw: prev,
                },
            );
        } else {
            let addrs = self.get_addresses(st, node, node);
            st.push(node, GEntry { addrs, ..GEntry::default() });
        }
    }
}
