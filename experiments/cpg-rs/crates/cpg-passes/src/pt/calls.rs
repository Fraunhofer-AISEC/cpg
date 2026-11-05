//! Calls of the points-to pass: function summaries, calling contexts and the effects of a call on
//! the state (`handleCall` and its helpers in `PointsToPass.kt`).

use super::*;

/// The key of a function summary: a parameter, a return, the function itself (a function without
/// returns), or the `dummy` literal of a function that has nothing to summarize.
#[derive(Clone, Copy, PartialEq, Eq, Debug)]
pub enum FKey {
    Node(N),
    Dummy,
}

#[derive(Clone, PartialEq, Eq, Debug)]
pub enum Src {
    None,
    Node(N),
    /// A name: a memory address the callee creates, made per call.
    Name(String),
}

/// `FSEntry`. Equality ignores the last writes (see `merge_fs_entry`).
#[derive(Clone, Debug)]
pub struct FsEntry {
    pub dst_depth: usize,
    pub src: Src,
    pub src_depth: usize,
    pub sub: String,
    pub lw: Set<Key>,
    pub short: Option<bool>,
    pub dummy: bool,
}

impl FsEntry {
    fn same(&self, o: &FsEntry) -> bool {
        self.dst_depth == o.dst_depth
            && self.src == o.src
            && self.src_depth == o.src_depth
            && self.sub == o.sub
            && self.short == o.short
            && self.dummy == o.dummy
    }
}

/// `MapDstToSrcEntry`
#[derive(Clone)]
struct MapEntry {
    param: FKey,
    src: Option<N>,
    lw: Set<Key>,
    props: Props,
    dst: Set<N>,
}

type DstMap = BTreeMap<N, Vec<MapEntry>>;

struct Work {
    param: FKey,
    argument: N,
    by_depth: [Vec<Pre>; 4],
}

/// `PreprocessedFSEntry`
#[derive(Clone)]
struct Pre {
    dst_depth: usize,
    sub: String,
    src: Option<N>,
    src_depth: usize,
    short: bool,
    props: Props,
    prev: Set<Key>,
}

impl<'g, 'a> Pt<'g, 'a> {
    pub(super) fn has_body(&self, f: N) -> bool {
        self.g.children(f).any(|c| self.g.nodes[c as usize].kind == Kind::Block)
    }

    pub(super) fn single_invoke_without_body(&self, call: N) -> bool {
        match self.invokes.get(&call) {
            Some(v) if v.len() == 1 => !self.has_body(v[0]),
            _ => false,
        }
    }

    fn params(&self, f: N) -> Vec<N> {
        self.g.children(f).filter(|&c| self.is(c, Kind::Param)).collect()
    }

    fn arguments(&self, call: N) -> Vec<N> {
        let mut k: Vec<N> = self.g.children(call).collect();
        k.pop();
        k
    }

    fn returns_of(&self, f: N) -> Vec<N> {
        let mut ret = Vec::new();
        let mut stack = vec![f];
        while let Some(x) = stack.pop() {
            if self.is(x, Kind::Return) {
                ret.push(x);
            }
            let mut k: Vec<N> = self.g.children(x).collect();
            k.reverse();
            stack.extend(k);
        }
        ret
    }

    fn is_array_type(&self, n: N) -> bool {
        n < SYNTH_BASE && self.ty(n).ends_with(']')
    }

    // ---------------------------------------------------------------- function summaries

    pub(super) fn summary_is_empty(&self, f: N) -> bool {
        self.fsum.get(&f).is_none_or(|v| v.is_empty())
    }

    /// `mergeFSEntry`
    pub(super) fn merge_fs_entry(&mut self, f: N, key: FKey, new: FsEntry) {
        let list = self.fsum.entry(f).or_default();
        let slot = match list.iter().position(|(k, _)| *k == key) {
            Some(i) => i,
            None => {
                list.push((key, Vec::new()));
                list.len() - 1
            }
        };
        let entries = &mut list[slot].1;
        match entries.iter_mut().find(|e| e.same(&new)) {
            Some(e) => e.lw.extend(new.lw),
            None => entries.push(new),
        }
    }

    /// `addDummyFunctionSummary`: the dataflows such a summary implies.
    pub(super) fn add_dummy_function_summary(&mut self, f: N) -> Set<Key> {
        let mut prev = Set::new();
        let mut entries = vec![FsEntry {
            dst_depth: 0,
            src: Src::Node(f),
            src_depth: 1,
            sub: String::new(),
            lw: Set::new(),
            short: None,
            dummy: true,
        }];
        for (i, p) in self.params(f).into_iter().enumerate() {
            entries.push(FsEntry {
                dst_depth: 0,
                src: Src::None,
                src_depth: 1,
                sub: String::new(),
                lw: Set::from([key(p, Props { idx: Some(i as u32), ..Props::default() })]),
                short: Some(true),
                dummy: true,
            });
            prev.insert(key(p, Props::default()));
        }
        let rets = self.returns_of(f);
        let keys: Vec<N> = if rets.is_empty() { vec![f] } else { rets };
        let list = self.fsum.entry(f).or_default();
        for k in keys {
            list.push((FKey::Node(k), entries.clone()));
        }
        prev
    }

    /// `addDummyFS` of `calculateFunctionSummaries`
    fn add_dummy_fs(&mut self, f: N) {
        let entries: Vec<FsEntry> = self
            .params(f)
            .into_iter()
            .map(|p| FsEntry {
                dst_depth: 0,
                src: Src::Node(p),
                src_depth: 1,
                sub: String::new(),
                lw: Set::new(),
                short: None,
                dummy: true,
            })
            .collect();
        let rets = self.returns_of(f);
        let keys: Vec<N> = if rets.is_empty() { vec![f] } else { rets };
        let list = self.fsum.entry(f).or_default();
        for k in keys {
            list.push((FKey::Node(k), entries.clone()));
        }
    }

    /// `handleEmptyFunction` for a function without a body, and `acceptInternal` around it.
    pub(super) fn analyze_empty(&mut self, f: N) {
        let mut st = State::default();
        let addrs = self.get_addresses(&mut st, f, f);
        st.push_decl(f, DEntry { addrs, ..DEntry::default() });
        let mut st = self.transfer(f, st);
        if self.summary_is_empty(f) {
            let prev = self.add_dummy_function_summary(f);
            st.push(f, GEntry { lw: prev, ..GEntry::default() });
        }
        self.draw(&st);
    }

    /// `calculateFunctionSummaries`
    fn calculate_function_summaries(&mut self, invoke: N) {
        if self.summary_is_empty(invoke) {
            if self.has_body(invoke) {
                if !self.chain.contains(&invoke) {
                    self.analyze(invoke);
                } else {
                    self.add_dummy_fs(invoke);
                }
            } else {
                self.add_dummy_fs(invoke);
            }
        }
    }

    // ---------------------------------------------------------------- nested values

    /// `getNestedValues`: depth 0 the address, 1 the value, 2 the dereference, ..., -1 the node.
    pub(super) fn get_nested_values(
        &mut self,
        st: &mut State,
        node: N,
        depth: i32,
        fetch_fields: bool,
        only_existing: bool,
        excl_short: bool,
    ) -> Set<(N, bool)> {
        if depth == -1 {
            return Set::from([(node, false)]);
        }
        if depth == 0 {
            return self.get_addresses(st, node, node).into_iter().map(|a| (a, false)).collect();
        }
        let skip_initial = if !self.is(node, Kind::PointerRef) && only_existing {
            let addrs = self.get_addresses(st, node, node);
            if addrs.iter().any(|&a| st.has_decl_value(a, excl_short)) {
                false
            } else {
                let mut has_base = false;
                for (base, _) in self.bases_and_offsets(node) {
                    if has_base {
                        break;
                    }
                    let ba = self.get_addresses(st, base, base);
                    if ba.iter().any(|&a| st.has_decl_value(a, excl_short)) {
                        has_base = true;
                    }
                }
                !has_base
            }
        } else {
            false
        };
        let mut ret: Set<(N, bool)> = Set::new();
        if !skip_initial {
            for v in self.get_values(st, node, node) {
                if !excl_short || !v.1 {
                    ret.insert(v);
                }
            }
        }
        for _ in 1..depth {
            let mut next = Set::new();
            for &(v, _) in &ret.clone() {
                if !only_existing || st.has_decl_value(v, excl_short) {
                    for f in self.fetch_decl_value(st, v, fetch_fields, excl_short) {
                        next.insert((f.value, f.short));
                    }
                }
            }
            ret = next;
        }
        ret
    }

    // ---------------------------------------------------------------- handleCall

    /// `handleCall`
    pub(super) fn handle_call(&mut self, call: N, st: &mut State) {
        let mut map: DstMap = BTreeMap::new();
        let mut short_prev: Set<Key> = Set::new();
        let all = self.invokes.get(&call).cloned().unwrap_or_default();
        // a declaration is ignored if the same function (name and type) is also there with a body
        let invokes: Vec<N> = all
            .iter()
            .copied()
            .filter(|&inv| {
                !(!self.has_body(inv)
                    && all.iter().any(|&o| {
                        o != inv
                            && self.g.name_str(o) == self.g.name_str(inv)
                            && self.g.type_str(o) == self.g.type_str(inv)
                    }))
            })
            .collect();
        for inv in invokes {
            self.calculate_function_summaries(inv);
            self.incoming_contexts(inv, call, st);
            let entries_by_key = self.fsum.get(&inv).cloned().unwrap_or_default();
            let args = self.arguments(call);
            let mut work: Vec<Work> = Vec::new();
            for (fk, entries) in &entries_by_key {
                let dst = match fk {
                    FKey::Node(k) if self.is(*k, Kind::Param) => {
                        let idx = self.params(inv).iter().position(|p| p == k);
                        idx.and_then(|i| args.get(i).copied())
                    }
                    FKey::Node(k) if self.is(*k, Kind::Return) || *k == inv => Some(call),
                    _ => None,
                };
                let Some(dst) = dst else { continue };
                let mut buckets: [Vec<Pre>; 4] = Default::default();
                for e in entries {
                    if e.dst_depth > 3 {
                        continue;
                    }
                    let short = e.short == Some(true);
                    let props = Props { fs: e.short, ..Props::default() };
                    let src = match &e.src {
                        Src::Node(n) if self.is(*n, Kind::Function) => Some(call),
                        Src::Node(n) => Some(*n),
                        Src::Name(name) => Some(self.call_memory_address(call, name)),
                        Src::None => None,
                    };
                    let prev = self.calculate_prev_dfgs(&e.lw, short, call, inv, &e.src, e.dst_depth, dst, st);
                    buckets[e.dst_depth].push(Pre {
                        dst_depth: e.dst_depth,
                        sub: e.sub.clone(),
                        src,
                        src_depth: e.src_depth,
                        short,
                        props,
                        prev,
                    });
                }
                let mut by_depth: [Vec<Pre>; 4] = Default::default();
                for d in 0..4 {
                    by_depth[d] = dedup_pre(std::mem::take(&mut buckets[d]));
                }
                work.push(Work { param: *fk, argument: dst, by_depth });
            }
            if work.is_empty() {
                continue;
            }
            for depth in 0..4 {
                let snapshot = map.clone();
                for w in &work {
                    for e in &w.by_depth[depth] {
                        let (dest_addrs, dests) = self.calculate_call_destinations(
                            st, &snapshot, e.dst_depth, &e.sub, w.argument, &e.props, w.param,
                        );
                        self.add_entry_to_map(
                            st, &mut map, &dest_addrs, &dests, e.src, e.short, e.src_depth, &e.props, call,
                            &e.prev, w.param, &mut short_prev, inv,
                        );
                    }
                }
            }
        }
        let out = vec![call];
        for (dst_addr, values) in &map {
            self.write_map_entries(st, *dst_addr, values, &out);
        }
        if !short_prev.is_empty() {
            st.push(call, GEntry { lw: short_prev, ..GEntry::default() });
        }
    }

    /// The memory address a callee creates per call (`CallToMemAddrMap`).
    fn call_memory_address(&mut self, call: N, name: &str) -> N {
        let k = (call, format!("callmem:{name}"));
        if let Some(&n) = self.unknowns.get(&k) {
            return n;
        }
        let n = self.new_synth(SClass::Memory, &Name::plain(name), false);
        self.unknowns.insert(k, n);
        n
    }

    #[allow(clippy::too_many_arguments)]
    fn calculate_prev_dfgs(
        &mut self,
        lws: &Set<Key>,
        short: bool,
        call: N,
        invoke: N,
        src: &Src,
        dst_depth: usize,
        _dst: N,
        _st: &mut State,
    ) -> Set<Key> {
        let mut ret = Set::new();
        if lws.is_empty() {
            ret.insert(key(invoke, Props::default()));
        }
        let args = self.arguments(call);
        for k in lws {
            let prev = match (src, self.kind(k.n)) {
                (Src::Node(s), Some(Kind::Param)) if self.is(*s, Kind::Param) && dst_depth > 1 => *s,
                _ => k.n,
            };
            if short {
                match self.kind(prev) {
                    Some(Kind::Function) => {
                        ret.insert(key(call, k.p.clone()));
                    }
                    Some(Kind::Param) => match k.p.idx {
                        Some(i) if (i as usize) < args.len() => {
                            ret.insert(key(args[i as usize], k.p.clone()));
                        }
                        _ => {
                            ret.insert(key(prev, k.p.clone()));
                        }
                    },
                    _ => {
                        ret.insert(key(prev, k.p.clone()));
                    }
                }
            } else {
                ret.insert(key(prev, k.p.clone()));
            }
        }
        ret
    }

    /// `calculateIncomingCallingContexts`
    fn incoming_contexts(&mut self, function: N, call: N, st: &mut State) {
        let ctx_in = Ctx::In(vec![call]);
        let params = self.params(function);
        for (i, arg) in self.arguments(call).into_iter().enumerate() {
            let Some(&p) = params.get(i) else { continue };
            let arr = self.is_array_type(arg);
            let arg_vals: Set<(N, bool)> = if arr {
                Set::from([(arg, true)])
            } else {
                self.get_nested_values(st, arg, 1, false, true, true)
            };
            let pmvs = self.param_mvs.get(&p).cloned().unwrap_or_default();
            let deref: Vec<N> = pmvs.iter().copied().filter(|&m| self.synth_of(m).unwrap().local == "derefvalue").collect();
            let derefderef: Vec<N> =
                pmvs.iter().copied().filter(|&m| self.synth_of(m).unwrap().local == "derefderefvalue").collect();
            for &(arg_val, _) in &arg_vals {
                self.inner_incoming(arg, st, arg_val, &ctx_in, &deref, &derefderef);
            }
            st.push(
                p,
                GEntry {
                    addrs: Set::new(),
                    vals: Set::from([key(arg, Props::default())]),
                    lw: Set::from([key(arg, Props { ctx: vec![ctx_in.clone()], ..Props::default() })]),
                },
            );
        }
    }

    fn inner_incoming(
        &mut self,
        arg: N,
        st: &mut State,
        arg_val: N,
        ctx_in: &Ctx,
        deref: &[N],
        derefderef: &[N],
    ) {
        let new_props = |k: &Key| -> Props {
            let mut chain: Vec<Ctx> = k.p.ctx.iter().filter(|c| *c != ctx_in).cloned().collect();
            chain.push(ctx_in.clone());
            Props { ctx: chain, fs: Some(k.p.fs == Some(true)), ..Props::default() }
        };
        let arr = self.is_array_type(arg);
        let arg_deref_vals: Set<N> = if deref.is_empty() {
            Set::new()
        } else if arr {
            Set::from([arg])
        } else {
            self.get_nested_values(st, arg_val, 1, false, true, true).into_iter().map(|v| v.0).collect()
        };
        let last_deref_writes: Set<Key> = if deref.is_empty() {
            Set::new()
        } else if arr {
            Set::from([key(arg, Props { ctx: vec![ctx_in.clone()], fs: Some(false), ..Props::default() })])
        } else if let Some(e) = st.decl.get(&arg_val) {
            e.lw.iter().map(|k| key(k.n, new_props(k))).collect()
        } else if self.is(arg, Kind::PointerRef) {
            let input = self.first(arg);
            self.get_last_writes(st, input).iter().map(|k| key(k.n, new_props(k))).collect()
        } else {
            Set::new()
        };
        let arg_deref_vals_el: Set<Key> = arg_deref_vals.iter().map(|&n| key(n, Props::default())).collect();
        let mut dd: Set<N> = Set::new();
        for &v in &arg_deref_vals {
            for x in self.get_nested_values(st, v, 1, false, true, true) {
                dd.insert(x.0);
            }
        }
        let deref_deref_el: Set<Key> = dd.iter().map(|&n| key(n, Props::default())).collect();
        let last_dd_writes: Set<Key> = if arr {
            arg_deref_vals
                .iter()
                .map(|&v| key(v, Props { ctx: vec![ctx_in.clone()], fs: Some(false), ..Props::default() }))
                .collect()
        } else {
            let mut s = Set::new();
            for &v in &arg_deref_vals {
                if let Some(e) = st.decl.get(&v) {
                    s.extend(e.lw.iter().map(|k| key(k.n, new_props(k))));
                }
            }
            s
        };
        for &dp in deref {
            st.push(dp, GEntry { addrs: Set::new(), vals: arg_deref_vals_el.clone(), lw: last_deref_writes.clone() });
            for &ddp in derefderef {
                st.push(
                    ddp,
                    GEntry { addrs: Set::from([dp]), vals: deref_deref_el.clone(), lw: last_dd_writes.clone() },
                );
            }
        }
    }

    /// `calculateCallDestinations`: where the call writes to (addresses, with a partial write name)
    /// and the destination nodes of the general state.
    #[allow(clippy::too_many_arguments)]
    fn calculate_call_destinations(
        &mut self,
        st: &mut State,
        map: &DstMap,
        dst_depth: usize,
        sub: &str,
        argument: N,
        props: &Props,
        param: FKey,
    ) -> (Vec<(N, Option<String>)>, Set<N>) {
        let destination: Set<N> = if self.is(argument, Kind::Call) {
            if dst_depth == 0 { Set::from([argument]) } else { Set::new() }
        } else if (self.is(argument, Kind::PointerRef) || (self.is(argument, Kind::Ref) && self.is_array_type(argument)))
            && self.is_global(argument)
            && self.ref_of[argument as usize] != NONE
        {
            Set::from([self.ref_of[argument as usize]])
        } else {
            Set::new()
        };
        let dest_addr_depth = dst_depth as i32 - 1;
        if self.is(argument, Kind::Call) && dst_depth > 1 {
            let mut updated: Vec<(N, Option<String>)> = Vec::new();
            if let Some(es) = map.get(&argument) {
                for e in es {
                    if e.param == param {
                        if let Some(s) = e.src {
                            if !updated.contains(&(s, None)) {
                                updated.push((s, None));
                            }
                        }
                    }
                }
            }
            if !updated.is_empty() {
                return (updated, destination);
            }
        } else if dst_depth > 2 {
            let vals: Set<N> = self.get_values(st, argument, argument).into_iter().map(|v| v.0).collect();
            let mut updated: Vec<(N, Option<String>)> = Vec::new();
            for (k, es) in map {
                if vals.contains(k) {
                    for e in es {
                        if let Some(s) = e.src {
                            if !updated.contains(&(s, None)) {
                                updated.push((s, None));
                            }
                        }
                    }
                }
            }
            if !updated.is_empty() {
                return (updated, destination);
            }
        }
        let partial = props.off.clone();
        if !sub.is_empty() || partial.is_some() {
            let mut fa: Vec<(N, Option<String>)> = Vec::new();
            let vals = self.get_nested_values(st, argument, dest_addr_depth, true, false, false);
            for (v, _) in vals {
                let e = (v, Some(sub.to_string()));
                if !fa.contains(&e) {
                    fa.push(e);
                }
                let parent = self.node_name(v);
                let partial_string = if sub.is_empty() {
                    match &partial {
                        Some(Off::Str(s)) => s.clone(),
                        _ => String::new(),
                    }
                } else {
                    sub.to_string()
                };
                let new_name = Name::child(&partial_string, &parent);
                for a in self.fetch_field_addresses(st, &Set::from([v]), &new_name) {
                    let e = (a, None);
                    if !fa.contains(&e) {
                        fa.push(e);
                    }
                }
            }
            (fa, destination)
        } else {
            let mut da: Vec<(N, Option<String>)> = Vec::new();
            for (v, _) in self.get_nested_values(st, argument, dest_addr_depth, false, false, false) {
                if !da.contains(&(v, None)) {
                    da.push((v, None));
                }
            }
            let mut offset = String::new();
            let mut bo = self.bases_and_offsets(argument);
            bo.reverse();
            for (base, off) in bo {
                if !offset.is_empty() {
                    offset.push('.');
                }
                offset.push_str(&match off {
                    Off::Str(s) => s,
                    Off::Node(f) if self.is(f, Kind::Field) => self.g.name_str(f).to_string(),
                    _ => "null".to_string(),
                });
                for (value, short) in self.get_nested_values(st, base, dest_addr_depth, false, false, false) {
                    if !short {
                        let e = (value, Some(offset.clone()));
                        if !da.contains(&e) {
                            da.push(e);
                        }
                    }
                }
            }
            (da, destination)
        }
    }

    /// `createPropertySet`
    fn create_property_set(&self, props: &Props, short: bool, partial_write: Option<&str>) -> Props {
        let has_short = props.fs == Some(short);
        match partial_write {
            None => {
                if has_short {
                    props.clone()
                } else {
                    Props { fs: Some(short), ..props.clone() }
                }
            }
            Some(pw) => {
                let matching = match &props.off {
                    Some(Off::Field(n)) => n == pw,
                    Some(Off::Node(f)) if self.is(*f, Kind::Field) => local_of(self.g.name_str(*f)) == pw,
                    _ => false,
                };
                if has_short && matching {
                    props.clone()
                } else {
                    Props { fs: Some(short), off: Some(Off::Field(pw.to_string())), ..props.clone() }
                }
            }
        }
    }

    /// `addEntryToMap`
    #[allow(clippy::too_many_arguments)]
    fn add_entry_to_map(
        &mut self,
        st: &mut State,
        map: &mut DstMap,
        dest_addrs: &[(N, Option<String>)],
        destinations: &Set<N>,
        src: Option<N>,
        short: bool,
        src_depth: usize,
        props: &Props,
        call: N,
        last_writes: &Set<Key>,
        param: FKey,
        short_prev: &mut Set<Key>,
        _inv: N,
    ) {
        if dest_addrs.is_empty() {
            return;
        }
        let contexts: Vec<(N, Props)> = dest_addrs
            .iter()
            .map(|(d, pw)| (*d, self.create_property_set(props, short, pw.as_deref())))
            .collect();
        let mut insert = |map: &mut DstMap, value: Option<N>| {
            for (d, ps) in &contexts {
                let list = map.entry(*d).or_default();
                match list.iter_mut().find(|e| {
                    e.param == param && e.src == value && e.props == *ps && e.dst == *destinations
                }) {
                    Some(e) => e.lw.extend(last_writes.iter().cloned()),
                    None => list.push(MapEntry {
                        param,
                        src: value,
                        lw: last_writes.clone(),
                        props: ps.clone(),
                        dst: destinations.clone(),
                    }),
                }
            }
        };
        match src.map(|s| (s, self.kind(s))) {
            Some((s, Some(Kind::Param))) => {
                let args = self.arguments(call);
                // the index of the parameter in its function
                let f = self.fn_param_owner(s);
                let idx = self.params(f).iter().position(|&p| p == s).unwrap_or(usize::MAX);
                if let Some(&arg) = args.get(idx) {
                    if short {
                        short_prev.insert(key(arg, Props::fs(true)));
                    }
                    let values: Set<N> = if !short {
                        self.get_nested_values(st, arg, src_depth as i32, true, false, true)
                            .into_iter()
                            .map(|v| v.0)
                            .collect()
                    } else {
                        Set::from([arg])
                    };
                    if values.is_empty() {
                        return;
                    }
                    for v in values {
                        insert(map, Some(v));
                    }
                }
            }
            Some((s, None)) if self.is_pmv(s) => {
                let full = self.synth_of(s).unwrap().full.clone();
                let segs: Vec<&str> = full.split('.').collect();
                if segs.len() < 2 {
                    return;
                }
                let pname = segs[segs.len() - 2].to_string();
                let mut pvals: Set<N> = Set::new();
                for inv in self.invokes.get(&call).cloned().unwrap_or_default() {
                    for (i, p) in self.params(inv).into_iter().enumerate() {
                        if local_of(self.g.name_str(p)) != pname {
                            continue;
                        }
                        if let Some(&arg) = self.arguments(call).get(i) {
                            for (v, _) in self.get_nested_values(st, arg, src_depth as i32, false, false, false) {
                                pvals.insert(v);
                            }
                        }
                    }
                }
                if pvals.is_empty() {
                    return;
                }
                for v in pvals {
                    insert(map, Some(v));
                }
            }
            Some((s, None)) => {
                insert(map, Some(s));
            }
            other => {
                let new_sources: Set<Option<N>> = if src_depth == 0 {
                    Set::from([other.map(|o| o.0)])
                } else if let Some((s, _)) = other {
                    let v = self.get_nested_values(st, s, src_depth as i32, false, false, false);
                    v.into_iter().map(|x| Some(x.0)).collect()
                } else {
                    Set::from([None])
                };
                if new_sources.is_empty() {
                    return;
                }
                for v in new_sources {
                    insert(map, v);
                }
            }
        }
    }

    fn fn_param_owner(&self, p: N) -> N {
        self.parent[p as usize]
    }

    /// `writeMapEntriesToState`
    fn write_map_entries(&mut self, st: &mut State, dst_addr: N, values: &[MapEntry], out: &[N]) {
        let mut sources: Vec<Source> = Vec::new();
        for v in values {
            let s = (v.src, v.props.fs == Some(true), v.props.off.clone());
            if !sources.contains(&s) {
                sources.push(s);
            }
        }
        let mut last_writes: Set<Key> = Set::new();
        let mut destinations: Vec<N> = Vec::new();
        for v in values {
            for lw in &v.lw {
                let mut p = v.props.clone();
                if v.props.fs != Some(true) {
                    let existing: Vec<Ctx> = lw.p.ctx.clone();
                    let mut seen: Vec<N> = Vec::new();
                    for c in &existing {
                        match c {
                            Ctx::In(x) | Ctx::Out(x) => seen.extend(x),
                        }
                    }
                    let mut new_calls: Vec<N> = Vec::new();
                    for &c in out {
                        if !seen.contains(&c) {
                            seen.push(c);
                            new_calls.push(c);
                        }
                    }
                    let chain: Vec<Ctx> = if new_calls.is_empty() {
                        existing
                    } else if let Some(Ctx::Out(l)) = existing.last() {
                        let mut e = existing.clone();
                        e.pop();
                        let mut calls = l.clone();
                        calls.extend(new_calls);
                        e.push(Ctx::Out(calls));
                        e
                    } else {
                        let mut e = existing;
                        e.push(Ctx::Out(new_calls));
                        e
                    };
                    if !chain.is_empty() {
                        p.ctx = chain;
                    }
                }
                // the other properties of the last write
                if lw.p.fs.is_some() {
                    p.fs = lw.p.fs;
                }
                if lw.p.off.is_some() {
                    p.off = lw.p.off.clone();
                }
                if lw.p.idx.is_some() {
                    p.idx = lw.p.idx;
                }
                if lw.p.deref != 0 {
                    p.deref = lw.p.deref;
                }
                last_writes.insert(key(lw.n, p));
            }
            for &d in &v.dst {
                if !destinations.contains(&d) {
                    destinations.push(d);
                }
            }
        }
        self.update_values_gen(st, &sources, &destinations, &Set::from([dst_addr]), &last_writes, false);
    }
}

fn dedup_pre(entries: Vec<Pre>) -> Vec<Pre> {
    let mut out: Vec<Pre> = Vec::new();
    for e in entries {
        match out.iter_mut().find(|o| {
            o.dst_depth == e.dst_depth
                && o.sub == e.sub
                && o.src == e.src
                && o.src_depth == e.src_depth
                && o.short == e.short
                && o.props == e.props
        }) {
            Some(o) => o.prev.extend(e.prev),
            None => out.push(e),
        }
    }
    out
}

fn string_to_depth(name: &str) -> usize {
    match name {
        "value" => 1,
        "derefvalue" => 2,
        "derefderefvalue" => 3,
        _ => 0,
    }
}

impl<'g, 'a> Pt<'g, 'a> {
    /// The summary part of `handleReturn`: what is returned, at depth 1 to 3.
    pub(super) fn return_summary(&mut self, f: N, ret: N, rv: N, st: &mut State) {
        let mut values: Set<N> =
            self.get_values(st, rv, rv).into_iter().filter(|v| !v.1).map(|v| v.0).collect();
        let mut addresses: Set<N> = Set::new();
        for depth in 1..=3usize {
            let depth_lw: Option<Set<Key>> = if depth == 1 {
                None
            } else {
                let mut s = Set::new();
                for &a in &addresses {
                    s.extend(self.get_last_writes(st, a));
                }
                Some(s)
            };
            for &v in &values {
                let lw = depth_lw.clone().unwrap_or_else(|| Set::from([key(f, Props::default())]));
                self.merge_fs_entry(
                    f,
                    FKey::Node(ret),
                    FsEntry {
                        dst_depth: depth,
                        src: Src::Node(v),
                        src_depth: 0,
                        sub: String::new(),
                        lw,
                        short: Some(false),
                        dummy: false,
                    },
                );
            }
            let mut deref: Set<N> = Set::new();
            for &v in &values {
                if st.has_decl_value(v, true) {
                    for x in self.get_values(st, v, v) {
                        if !x.1 {
                            deref.insert(x.0);
                        }
                    }
                }
            }
            if deref.is_empty() {
                break;
            }
            addresses = values;
            values = deref;
        }
    }

    /// `storeFunctionSummary` (without the detailed short function summary)
    pub(super) fn store_function_summary(&mut self, f: N, st: &mut State) {
        // clearFSDummies
        if let Some(l) = self.fsum.get_mut(&f) {
            l.retain(|(k, _)| *k != FKey::Dummy);
        }
        for param in self.params(f) {
            let mut indexes: Vec<(N, usize)> = Vec::new();
            let values: Set<N> = self.get_values(st, param, param).into_iter().map(|v| v.0).collect();
            for &value in &values {
                if st.has_decl_value(value, false) {
                    indexes.push((value, 2));
                    let deref: Set<N> = self.get_values(st, value, value).into_iter().map(|v| v.0).collect();
                    for v in deref {
                        if st.has_decl_value(v, false) && !indexes.contains(&(v, 3)) {
                            indexes.push((v, 3));
                        }
                    }
                }
            }
            let pname = self.name_of(param);
            for (idx, dst_depth) in indexes {
                let state_entries: Vec<Fetched> = self
                    .fetch_decl_value(st, idx, true, true)
                    .into_iter()
                    .filter(|e| self.name_of(e.value).full != pname.full)
                    .collect();
                let with_sub: Vec<(N, bool)> =
                    state_entries.iter().filter(|e| !e.sub.is_empty()).map(|e| (e.value, e.short)).collect();
                for e in state_entries {
                    if e.sub.is_empty() && with_sub.contains(&(e.value, e.short)) {
                        continue;
                    }
                    let vn = self.name_of(e.value);
                    let is_own_pmv = self.is_pmv(e.value) && vn.local.contains("derefvalue") && {
                        let segs: Vec<&str> = vn.full.split('.').collect();
                        segs.len() >= 2 && segs[segs.len() - 2] == pname.local
                    };
                    if vn.full != pname.full
                        && !is_own_pmv
                        && !(self.is_unknown(e.value) && e.last_writes.is_empty())
                    {
                        self.add_parameter_info_to_fs(f, param, dst_depth, e.value, e.short, &e.sub, &e.last_writes);
                    }
                }
            }
        }
        if self.summary_is_empty(f) {
            self.fsum.entry(f).or_default().push((FKey::Dummy, Vec::new()));
        }
    }

    fn add_parameter_info_to_fs(
        &mut self,
        f: N,
        param: N,
        dst_depth: usize,
        value: N,
        short: bool,
        sub: &str,
        lws: &Set<Key>,
    ) {
        let src_depth = string_to_depth(&self.name_of(value).local);
        let filtered: Set<Key> = lws.iter().filter(|k| k.p.fs == Some(short)).cloned().collect();
        self.merge_fs_entry(
            f,
            FKey::Node(param),
            FsEntry {
                dst_depth,
                src: Src::Node(value),
                src_depth,
                sub: sub.to_string(),
                lw: filtered,
                short: Some(short),
                dummy: false,
            },
        );
        self.merge_fs_entry(
            f,
            FKey::Node(param),
            FsEntry {
                dst_depth,
                src: Src::Node(f),
                src_depth: 0,
                sub: sub.to_string(),
                lw: Set::from([key(f, Props::default())]),
                short: Some(true),
                dummy: false,
            },
        );
        // TODO: the detailed short function summary (which parameter influences the value) needs the
        // backward DFG walker
    }
}
