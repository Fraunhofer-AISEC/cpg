//! Passes on the graph (no parser dependency).

mod access;
mod dfg;
mod eog;
mod numeric;
mod resolve;
mod typing;

pub use access::access;
pub use dfg::dfg;
pub use eog::eog;
pub use resolve::resolve;
pub use typing::typing;
