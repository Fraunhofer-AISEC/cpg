//! Passes on the graph (no parser dependency).

mod access;
mod dfg;
mod eog;
mod numeric;
mod pointsto;
mod pt;
mod resolve;
mod typing;

pub use access::access;
pub use dfg::dfg;
pub use pointsto::pointsto;
pub use pt::{pointsto as pointsto2, pointsto_with as pointsto2_with, Config as PtConfig};
pub use eog::eog;
pub use resolve::resolve;
pub use typing::typing;
