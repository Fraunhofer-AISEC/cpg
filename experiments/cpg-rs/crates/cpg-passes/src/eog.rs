//! The evaluation order graph, ported from the Kotlin `EvaluationOrderGraphPass` (including its
//! quirks). Handlers mirror the Kotlin `handleXXX` functions; the child roles of the Kotlin nodes
//! (condition, body, ...) are read from the child order the C frontend produces.

use cpg_core::{EogEdge, Graph, Kind, NONE};
use rustc_hash::{FxHashMap, FxHashSet};

struct Eog<'g, 'a> {
    g: &'g mut Graph<'a>,
    parent: Vec<u32>,
    /// The EOG exits of what was handled so far (`currentPredecessors`).
    preds: Vec<u32>,
    /// The branch property of the next edges (`nextEdgeBranch`).
    branch: Option<bool>,
    /// Breaks and continues, by the loop or switch they belong to.
    breaks: FxHashMap<u32, Vec<u32>>,
    /// Nodes entered since the last attached node (`intermediateNodes`).
    intermediate: Vec<u32>,
    /// The first node attached for a label (`processedMapping`).
    processed: FxHashMap<u32, u32>,
    /// One goto waiting per label: a later goto to the same label replaces it, like the
    /// `objectListeners` map of the Kotlin `ProcessedListener`.
    listeners: FxHashMap<u32, u32>,
    /// Incoming and outgoing edges per node.
    inc: Vec<Vec<u32>>,
    out: Vec<Vec<(u32, Option<bool>)>>,
}

pub fn eog(g: &mut Graph) {
    let n = g.nodes.len();
    let mut parent = vec![NONE; n];
    for id in 0..n as u32 {
        let kids: Vec<u32> = g.children(id).collect();
        for k in kids {
            parent[k as usize] = id;
        }
    }
    g.eog.clear();
    let mut p = Eog {
        g,
        parent,
        preds: Vec::new(),
        branch: None,
        breaks: FxHashMap::default(),
        intermediate: Vec::new(),
        processed: FxHashMap::default(),
        listeners: FxHashMap::default(),
        inc: vec![Vec::new(); n],
        out: vec![Vec::new(); n],
    };
    p.handle_unit(0);
    p.remove_unreachable();
    // what the symbol resolver can reach: from the top-level declarations, the unit itself and
    // every function, along EOG edges
    let mut visited = vec![false; n];
    let mut stack: Vec<u32> = vec![0];
    stack.extend(p.g.children(0));
    for id in 0..n as u32 {
        if matches!(p.kind(id), Kind::Function | Kind::Constructor | Kind::Field) {
            stack.push(id);
        }
    }
    while let Some(x) = stack.pop() {
        if std::mem::replace(&mut visited[x as usize], true) {
            continue;
        }
        stack.extend(p.out[x as usize].iter().map(|&(to, _)| to));
    }
    p.g.eog_visited = visited;
    // flatten, per source node in creation order
    for from in 0..n as u32 {
        for &(to, branch) in &p.out[from as usize] {
            p.g.eog.push(EogEdge { from, to, branch, unreachable: false });
        }
    }
}

impl Eog<'_, '_> {
    fn kind(&self, id: u32) -> Kind {
        self.g.nodes[id as usize].kind
    }

    fn kids(&self, id: u32) -> Vec<u32> {
        self.g.children(id).collect()
    }

    fn kid(&self, id: u32, i: usize) -> u32 {
        self.g.children(id).nth(i).unwrap_or(NONE)
    }

    // ---- edges

    fn add_edge(&mut self, from: u32, to: u32) {
        self.out[from as usize].push((to, self.branch));
        self.inc[to as usize].push(from);
    }

    fn attach(&mut self, node: u32) {
        for i in std::mem::take(&mut self.intermediate) {
            self.process(i, node);
        }
        for p in std::mem::take(&mut self.preds) {
            self.add_edge(p, node);
        }
        self.branch = None;
        self.preds.push(node);
    }

    /// The `ProcessedListener`: connects a goto to the first node attached for its label.
    fn process(&mut self, from: u32, to: u32) {
        if self.kind(from) == Kind::Label {
            self.processed.insert(from, to);
        }
        if let Some(goto) = self.listeners.remove(&from) {
            self.add_edge(goto, to);
        }
    }

    /// EOG entries of a subtree: its nodes with an EOG predecessor outside of the subtree.
    fn entries(&self, root: u32) -> Vec<u32> {
        let mut nodes = Vec::new();
        let mut stack = vec![root];
        while let Some(n) = stack.pop() {
            nodes.push(n);
            let mut k = self.kids(n);
            k.reverse();
            stack.extend(k);
        }
        let in_eog: FxHashSet<u32> = nodes
            .iter()
            .copied()
            .filter(|&n| !self.inc[n as usize].is_empty() || !self.out[n as usize].is_empty())
            .collect();
        nodes
            .into_iter()
            .filter(|n| in_eog.contains(n) && self.inc[*n as usize].iter().any(|p| !in_eog.contains(p)))
            .collect()
    }

    // ---- dispatch (`handleEOG`)

    fn handle(&mut self, id: u32) {
        if id == NONE {
            return;
        }
        self.intermediate.push(id);
        match self.kind(id) {
            Kind::Unit => self.handle_unit(id),
            Kind::Record => self.handle_record(id),
            Kind::Function | Kind::Constructor => self.handle_function(id),
            Kind::Variable | Kind::Field => {
                self.attach(id);
                let init = self.kid(id, 0);
                self.handle(init);
            }
            Kind::Call => {
                let kids = self.kids(id);
                if let Some((&callee, args)) = kids.split_last() {
                    self.handle(callee);
                    for &a in args {
                        self.handle(a);
                    }
                }
                self.attach(id);
            }
            Kind::DeclStmt => self.handle_declaration_statement(id),
            Kind::Return => {
                for k in self.kids(id) {
                    self.handle(k);
                }
                self.attach(id);
                self.preds.clear();
            }
            Kind::Binary => {
                let (l, r) = (self.kid(id, 0), self.kid(id, 1));
                self.handle(l);
                self.handle(r);
                self.attach(id);
            }
            Kind::ShortCircuit => self.handle_short_circuit(id),
            Kind::Assign => {
                let (l, r) = (self.kid(id, 0), self.kid(id, 1));
                self.handle(l);
                self.handle(r);
                self.attach(id);
            }
            Kind::Block => {
                for k in self.kids(id) {
                    self.handle(k);
                }
                self.attach(id);
            }
            // nodes that evaluate their children (in order) and then themselves
            Kind::Unary | Kind::PointerRef | Kind::PointerDeref | Kind::Member | Kind::Subscript
            | Kind::ArrayConstruction | Kind::Range | Kind::Cast | Kind::InitList
            | Kind::ExprList => {
                for k in self.kids(id) {
                    self.handle(k);
                }
                self.attach(id);
            }
            Kind::Conditional => self.handle_conditional(id),
            Kind::If => self.handle_if(id),
            Kind::While => self.handle_while(id),
            Kind::DoWhile => self.handle_do_while(id),
            Kind::For => self.handle_for(id),
            Kind::Switch => self.handle_switch(id),
            Kind::Case => {
                for k in self.kids(id) {
                    self.handle(k);
                }
                self.attach(id);
            }
            Kind::Break | Kind::Continue => self.handle_break_continue(id),
            Kind::Label => {
                let sub = self.kid(id, 0);
                self.handle(sub);
            }
            Kind::Goto => self.handle_goto(id),
            // attached, but their children are not handled
            Kind::Literal | Kind::Ref | Kind::TypeReference | Kind::Empty | Kind::Default => {
                self.attach(id)
            }
            // not part of the EOG (typedefs, enums, problems, ...)
            _ => {}
        }
    }

    // ---- declarations

    fn handle_unit(&mut self, id: u32) {
        // the translation unit is a statement holder: attached, but nothing follows
        self.attach(id);
        self.preds.clear();
        self.attach(id);
        self.preds.clear();
        for c in self.kids(id) {
            self.preds.clear();
            self.handle(c);
        }
        self.processed.clear();
        self.listeners.clear();
    }

    fn handle_record(&mut self, id: u32) {
        self.attach(id);
        self.preds.clear();
        self.attach(id);
        self.preds.clear();
        self.preds.clear();
        // constructors, then fields, then nested records
        for kind in [Kind::Constructor, Kind::Field, Kind::Record] {
            let of_kind: Vec<u32> = self.kids(id).into_iter().filter(|&c| self.kind(c) == kind).collect();
            for c in of_kind {
                self.handle(c);
            }
        }
    }

    fn handle_function(&mut self, id: u32) {
        self.preds.clear();
        self.attach(id);
        // the Kotlin CPG lists the body before the parameters
        let body = self.kid(id, 0);
        if body != NONE && self.kind(body) == Kind::Block {
            self.handle(body);
        }
        self.preds.clear();
    }

    fn handle_declaration_statement(&mut self, id: u32) {
        for d in self.kids(id) {
            match self.kind(d) {
                Kind::Variable => self.handle(d),
                Kind::Function => {
                    let old = self.preds.clone();
                    self.handle(d);
                    self.preds = old;
                }
                _ => {}
            }
        }
        self.attach(id);
    }

    // ---- expressions

    fn handle_short_circuit(&mut self, id: u32) {
        let (l, r) = (self.kid(id, 0), self.kid(id, 1));
        self.handle(l);
        let conjunctive = self.g.name_str(id) == "&&";
        let short = self.preds.clone();
        self.branch = Some(conjunctive);
        self.handle(r);
        self.attach(id);
        self.preds = short;
        self.branch = Some(!conjunctive);
        self.attach(id);
    }

    fn handle_conditional(&mut self, id: u32) {
        let (c, t, e) = (self.kid(id, 0), self.kid(id, 1), self.kid(id, 2));
        let mut open = Vec::new();
        self.handle(c);
        let cond_exits = self.preds.clone();
        self.branch = Some(true);
        self.handle(t);
        open.extend(self.preds.clone());
        self.preds = cond_exits;
        self.branch = Some(false);
        self.handle(e);
        open.extend(self.preds.clone());
        self.preds = open;
        self.attach(id);
    }

    // ---- statements

    fn handle_if(&mut self, id: u32) {
        let (c, t, e) = (self.kid(id, 0), self.kid(id, 1), self.kid(id, 2));
        let mut open = Vec::new();
        self.handle(c);
        let cond_exits = self.preds.clone();
        self.branch = Some(true);
        self.handle(t);
        open.extend(self.preds.clone());
        if e != NONE {
            self.preds = cond_exits;
            self.branch = Some(false);
            self.handle(e);
            open.extend(self.preds.clone());
        } else {
            self.preds = cond_exits;
            self.branch = Some(false);
            self.attach(id);
        }
        self.preds = open;
        self.attach(id);
    }

    /// Edges from the current exits to the start of a loop (`connectCurrentEOGToLoopStart`).
    fn connect_to_loop_start(&mut self, lp: u32) {
        let start = match self.kind(lp) {
            Kind::While => self.kid(lp, 1), // condition
            Kind::For => self.kid(lp, 2),   // condition
            _ => self.kid(lp, 0),           // do-while: the body
        };
        for s in self.entries(start) {
            for p in self.preds.clone() {
                self.add_edge(p, s);
            }
        }
    }

    /// Breaks leave the loop; continues jump to the condition (`handleContainedBreaksAndContinues`).
    fn contained_breaks_and_continues(&mut self, lp: u32) {
        let Some(list) = self.breaks.get(&lp).cloned() else { return };
        let breaks: Vec<u32> = list.iter().copied().filter(|&b| self.kind(b) == Kind::Break).collect();
        self.preds.extend(breaks);
        let continues: Vec<u32> = list.into_iter().filter(|&b| self.kind(b) == Kind::Continue).collect();
        if !continues.is_empty() {
            // note: for a `for` loop this is the condition, not the iteration expression
            let cond = if self.kind(lp) == Kind::For { self.kid(lp, 2) } else { self.kid(lp, 1) };
            for s in self.entries(cond) {
                for &c in &continues {
                    self.add_edge(c, s);
                }
            }
        }
    }

    fn handle_while(&mut self, id: u32) {
        let (body, cond) = (self.kid(id, 0), self.kid(id, 1));
        self.handle(cond);
        self.branch = Some(true);
        let tmp = self.preds.clone();
        self.handle(body);
        self.connect_to_loop_start(id);
        self.preds = tmp;
        self.branch = Some(false);
        self.contained_breaks_and_continues(id);
        self.attach(id);
    }

    fn handle_do_while(&mut self, id: u32) {
        let (body, cond) = (self.kid(id, 0), self.kid(id, 1));
        self.handle(body);
        self.handle(cond);
        // `do { ... } while (0)` has no back edge
        if !self.always_falsy(cond) {
            self.branch = Some(true);
            self.connect_to_loop_start(id);
        }
        self.branch = Some(false);
        self.contained_breaks_and_continues(id);
        self.attach(id);
    }

    fn always_falsy(&self, cond: u32) -> bool {
        if self.kind(cond) != Kind::Literal {
            return false;
        }
        let n = self.g.nodes[cond as usize];
        if n.start == NONE {
            return false;
        }
        let t = String::from_utf8_lossy(&self.g.src[n.start as usize..n.end as usize]).to_lowercase();
        let t = t.trim_end_matches(|c| c == 'u' || c == 'l');
        let v = if let Some(h) = t.strip_prefix("0x") {
            u128::from_str_radix(h, 16).ok()
        } else {
            t.parse::<u128>().ok()
        };
        v == Some(0)
    }

    fn handle_for(&mut self, id: u32) {
        let (body, init, cond, iter) = (self.kid(id, 0), self.kid(id, 1), self.kid(id, 2), self.kid(id, 3));
        self.handle(init);
        self.handle(cond);
        self.branch = Some(true);
        let tmp = self.preds.clone();
        self.handle(body);
        self.handle(iter);
        self.connect_to_loop_start(id);
        self.preds = tmp;
        self.branch = Some(false);
        self.contained_breaks_and_continues(id);
        self.attach(id);
    }

    fn handle_switch(&mut self, id: u32) {
        let (selector, compound) = (self.kid(id, 0), self.kid(id, 1));
        self.handle(selector);
        let tmp = self.preds.clone();
        self.preds = Vec::new();
        let stmts = self.kids(compound);
        for &s in &stmts {
            if matches!(self.kind(s), Kind::Case | Kind::Default) {
                self.preds.extend(tmp.iter().copied());
            }
            self.handle(s);
        }
        if !stmts.iter().any(|&s| self.kind(s) == Kind::Default) {
            self.preds.extend(tmp.iter().copied());
        }
        self.attach(compound);
        if let Some(b) = self.breaks.get(&id) {
            self.preds.extend(b.iter().copied());
        }
        self.attach(id);
    }

    fn handle_break_continue(&mut self, id: u32) {
        self.attach(id);
        let is_break = self.kind(id) == Kind::Break;
        let mut p = self.parent[id as usize];
        while p != NONE {
            let k = self.kind(p);
            let target = matches!(k, Kind::While | Kind::DoWhile | Kind::For)
                || (is_break && k == Kind::Switch);
            if target {
                self.breaks.entry(p).or_default().push(id);
                break;
            }
            p = self.parent[p as usize];
        }
        self.preds.clear();
    }

    fn handle_goto(&mut self, id: u32) {
        self.attach(id);
        // the target label: the label of that name in the enclosing function
        let name = self.g.name_str(id).to_string();
        let mut f = self.parent[id as usize];
        while f != NONE && !matches!(self.kind(f), Kind::Function | Kind::Constructor) {
            f = self.parent[f as usize];
        }
        let label = (0..self.g.nodes.len() as u32).find(|&l| {
            self.kind(l) == Kind::Label && self.g.name_str(l) == name && self.in_function(l, f)
        });
        if let Some(l) = label {
            if let Some(&to) = self.processed.get(&l) {
                self.add_edge(id, to);
            } else {
                self.listeners.insert(l, id);
            }
        }
        self.preds.clear();
    }

    fn in_function(&self, mut n: u32, f: u32) -> bool {
        while n != NONE {
            if n == f {
                return true;
            }
            n = self.parent[n as usize];
        }
        f == NONE
    }

    // ---- cleanup

    /// Removes the outgoing edges of nodes that are not reachable from a start
    /// (`removeUnreachableEOGEdges`).
    fn remove_unreachable(&mut self) {
        let n = self.g.nodes.len();
        let has_edges = |s: &Self, i: usize| !s.inc[i].is_empty() || !s.out[i].is_empty();
        let mut remaining: FxHashSet<u32> = (0..n as u32).filter(|&i| has_edges(self, i as usize)).collect();
        let mut frontier: Vec<u32> = remaining
            .iter()
            .copied()
            .filter(|&i| {
                matches!(
                    self.kind(i),
                    Kind::Unit | Kind::Function | Kind::Constructor | Kind::Record | Kind::Variable
                        | Kind::Field | Kind::Param
                )
            })
            .collect();
        let mut seen: FxHashSet<u32> = FxHashSet::default();
        while !frontier.is_empty() {
            for f in &frontier {
                remaining.remove(f);
            }
            let mut next = Vec::new();
            for &f in &frontier {
                for &(to, _) in &self.out[f as usize] {
                    if !seen.contains(&to) {
                        seen.insert(to);
                        next.push(to);
                    }
                }
            }
            frontier = next;
        }
        for u in remaining {
            self.out[u as usize].clear();
        }
    }
}
