//! tree-sitter-c exposed as a flat, pre-order AST buffer over a C ABI (for the FFM experiment).
//!
//! Record layout (16 bytes, little endian), one per node in pre-order:
//!   u16 kind_id | u16 flags | u32 start_byte | u32 end_byte | u32 subtree_size (incl. self)
//! flags: 1 = named, 2 = error, 4 = missing, 8 = extra

use std::ffi::c_char;
use tree_sitter::Tree;

pub const RECORD: usize = 16;

pub fn flatten(tree: &Tree) -> Vec<u8> {
    flatten_with(tree, &[])
}

/// Like `flatten`, but clamps ranges that start or end inside a macro expansion `spans` (see
/// `pp::PpStats::spans`) to the original call.
pub fn flatten_with(tree: &Tree, spans: &[(u32, u32)]) -> Vec<u8> {
    let mut out: Vec<u8> = Vec::with_capacity(1 << 20);
    let mut cursor = tree.walk();
    // stack of (record offset) for nodes whose subtree size is not yet known
    let mut stack: Vec<usize> = Vec::new();
    loop {
        let n = cursor.node();
        let mut flags = 0u16;
        if n.is_named() { flags |= 1 }
        if n.is_error() { flags |= 2 }
        if n.is_missing() { flags |= 4 }
        if n.is_extra() { flags |= 8 }
        let off = out.len();
        out.extend_from_slice(&n.kind_id().to_le_bytes());
        out.extend_from_slice(&flags.to_le_bytes());
        let (sb, eb) = cpg_pp::clamp_to_spans(spans, n.start_byte() as u32, n.end_byte() as u32);
        out.extend_from_slice(&sb.to_le_bytes());
        out.extend_from_slice(&eb.to_le_bytes());
        out.extend_from_slice(&0u32.to_le_bytes());
        stack.push(off);
        if cursor.goto_first_child() { continue; }
        loop {
            let off = stack.pop().unwrap();
            let size = ((out.len() - off) / RECORD) as u32;
            out[off + 12..off + 16].copy_from_slice(&size.to_le_bytes());
            if cursor.goto_next_sibling() { break; }
            if !cursor.goto_parent() { return out; }
        }
    }
}

/// Parses `len` bytes at `src` and returns a malloc'd buffer of records; its length is written to
/// `out_len`. Free with `cparse_free`.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn cparse_parse(src: *const u8, len: usize, out_len: *mut usize) -> *mut u8 {
    let bytes = unsafe { std::slice::from_raw_parts(src, len) };
    let buf = flatten(&cpg_c::parse(bytes)).into_boxed_slice();
    unsafe { *out_len = buf.len() };
    Box::into_raw(buf) as *mut u8
}

#[unsafe(no_mangle)]
pub unsafe extern "C" fn cparse_free(ptr: *mut u8, len: usize) {
    drop(unsafe { Box::from_raw(std::ptr::slice_from_raw_parts_mut(ptr, len)) });
}

/// Number of node kinds, so the caller can build a kind-id -> name table.
#[unsafe(no_mangle)]
pub extern "C" fn cparse_kind_count() -> u32 {
    let l: tree_sitter::Language = tree_sitter_c::LANGUAGE.into();
    l.node_kind_count() as u32
}

/// NUL-terminated, static name of a kind id (null if unknown).
#[unsafe(no_mangle)]
pub extern "C" fn cparse_kind_name(id: u16) -> *const c_char {
    use std::sync::OnceLock;
    static NAMES: OnceLock<Vec<std::ffi::CString>> = OnceLock::new();
    let names = NAMES.get_or_init(|| {
        let l: tree_sitter::Language = tree_sitter_c::LANGUAGE.into();
        (0..l.node_kind_count() as u16)
            .map(|i| std::ffi::CString::new(l.node_kind_for_id(i).unwrap_or("")).unwrap())
            .collect()
    });
    names.get(id as usize).map_or(std::ptr::null(), |c| c.as_ptr())
}
