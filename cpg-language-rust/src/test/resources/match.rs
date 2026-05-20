#[derive(Debug, Clone)]
pub enum Wrap {
    One(i32),
    Pair(i32, i32),
    Nested(Option<Result<i32, i32>>),
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum Event {
    Value(i32),
}

pub trait Sink {
    fn push(&mut self, e: Event);
}

#[derive(Default)]
pub struct VecSink {
    pub events: Vec<Event>,
}

impl Sink for VecSink {
    fn push(&mut self, e: Event) {
        self.events.push(e);
    }
}

pub fn send<S: Sink>(sink: &mut S, value: i32) {
    sink.push(Event::Value(value));
}

/// Match with bindings + literals + wildcard
pub fn handle_wrap<S: Sink>(sink: &mut S) {
    let input = Wrap::Pair(1, 2); // constructed literals: 1,2

    let out = match input {
        Wrap::One(x) => x,
        // uses: x (could be any constructed literal from One)

        Wrap::Pair(a, 2) => a,
        // uses: 2 (pattern), a (binding → could be 1)

        Wrap::Pair(_, b) => b,
        // uses: wildcard ignores first, b (binding → could be 2)

        Wrap::Nested(Some(Ok(v))) => v,
        // uses: v (binding)

        Wrap::Nested(Some(Err(e))) => e,
        // uses: e (binding)

        Wrap::Nested(None) => 3,
        // uses: literal 3
    };
    // possible results: 1,2,3 (depending on path)

    send(sink, out);
}

/// Tuple match with mixed patterns
pub fn handle_tuple<S: Sink>(sink: &mut S) {
    let value = (4, 5); // constructed literals: 4,5

    let out = match value {
        (4, x) => x,
        // uses: 4 (pattern), x (binding → 5)

        (y, 6) => y,
        // uses: 6 (pattern), y (binding)

        (_, z) => z,
        // uses: wildcard, z (binding → 5)
    };
    // possible results: 5 or any y if second arm matched

    send(sink, out);
}

/// Tuple match with mixed patterns
pub fn handle_alternative<S: Sink>(sink: &mut S) {
    let value = (4, 5); // constructed literals: 4,5

    let out = match value {
        (4, x) | (x, 3)=> x,
        // uses: 4 (pattern), x (binding → 5) or 3 (pattern), x (binding → 4)
        (_, z) => z,
        // uses: wildcard, z (binding → 5)
    };
    // possible results: 4 or 5

    send(sink, out);
}

/// Deep nesting with real decomposition
pub fn handle_deep<S: Sink>(sink: &mut S) {
    let value = Some(Wrap::Nested(Some(Ok(7))));
    // constructed literals: 7

    let out = match value {
        None => 8,
        // uses: literal 8

        Some(Wrap::One(x)) => x,
        // uses: x

        Some(Wrap::Pair(a, b)) => a + b,
        // uses: a,b (could be any literals)

        Some(Wrap::Nested(inner)) => match inner {
            Some(Ok(v)) => v,
            // uses: v (7)

            Some(Err(e)) => e,
            // uses: e

            None => 9,
            // uses: literal 9
        },
        // possible inner results: 7,9 or e
    };
    // possible results: 7,8,9 or sums from Pair

    send(sink, out);
}

/// Sequence with constructed inputs inside function
pub fn process_all<S: Sink>(sink: &mut S) {
    let items = [
        Wrap::One(10),                    // literal 10
        Wrap::Pair(11, 12),               // literals 11,12
        Wrap::Nested(Some(Ok(13))),       // literal 13
        Wrap::Nested(Some(Err(14))),      // literal 14
        Wrap::Nested(None),               // no literal
    ];

    for item in items {
        let out = match item {
            Wrap::One(x) => x,
            // uses: x (10)

            Wrap::Pair(a, _) => a,
            // uses: a (11), wildcard ignores 12

            Wrap::Nested(Some(Ok(v))) => v,
            // uses: v (13)

            Wrap::Nested(Some(Err(e))) => e,
            // uses: e (14)

            Wrap::Nested(None) => 15,
            // uses: literal 15
        };
        // possible results: 10,11,13,14,15

        send(sink, out);
    }
}

fn main() {
    let mut sink = VecSink::default();

    handle_wrap(&mut sink);
    handle_tuple(&mut sink);
    handle_deep(&mut sink);
    process_all(&mut sink);

    let _ = sink;
}