//! Passes on the graph (no parser dependency).

mod access;
mod eog;
mod numeric;
mod resolve;
mod typing;

pub use access::access;
pub use eog::eog;
pub use resolve::resolve;
pub use typing::typing;
