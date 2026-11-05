//! The code property graph of the Rust port: node kinds, nodes and an arena-based graph.
//! No parser dependency; frontends build it, passes work on it.

mod dump;
mod graph;
mod kind;

pub use dump::dump;
pub use graph::{Access, EogEdge, Graph, Node, NONE};
pub use kind::Kind;
