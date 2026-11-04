//! The numeric types of C as the Kotlin `CLanguage` defines them (LP64), and the arithmetic type
//! propagation of `Language.arithmeticOpTypePropagation`.

/// `(is_floating_point, bit_width)` of a numeric C type, by its type name.
pub fn numeric(name: &str) -> Option<(bool, u32)> {
    Some(match name {
        "char" | "signed char" | "unsigned char" | "int8_t" | "uint8_t" => (false, 8),
        "short int" | "unsigned short int" | "int16_t" | "uint16_t" => (false, 16),
        "int" | "unsigned int" | "int32_t" | "uint32_t" | "wchar_t" => (false, 32),
        "long int" | "unsigned long int" | "long long int" | "unsigned long long int"
        | "int64_t" | "uint64_t" => (false, 64),
        // from the stdlib profile (64-bit platform)
        "size_t" | "ssize_t" | "ptrdiff_t" | "intptr_t" | "uintptr_t" => (false, 64),
        "__int128" => (false, 128),
        "bool" => (false, 1),
        "float" => (true, 32),
        "double" => (true, 64),
        "long double" => (true, 128),
        _ => return None,
    })
}

/// Type of `lhs OP rhs` for the arithmetic operators, or `None` for the unknown type.
pub fn arithmetic(lhs: &str, rhs: &str) -> Option<String> {
    let (l, r) = (numeric(lhs)?, numeric(rhs)?);
    match (l.0, r.0) {
        (true, false) => Some(lhs.to_string()),
        (false, true) => Some(rhs.to_string()),
        // both floating point or both integer: the one with the bigger bit width
        _ => Some(if l.1 >= r.1 { lhs } else { rhs }.to_string()),
    }
}

/// Type of a binary operation in C (`Language.propagateTypeOfBinaryOperation`).
pub fn binary(op: &str, lhs: &str, rhs: &str) -> String {
    let unknown = || "UNKNOWN".to_string();
    match op {
        // note: the Kotlin list has "=<" (sic) and misses ">=" and "!="
        "<" | "=<" | ">" | "<=" | "==" | "===" => "bool".to_string(),
        "+" | "-" | "*" | "/" | "%" | "&" | "&&" | "|" | "||" | "^" => {
            arithmetic(lhs, rhs).unwrap_or_else(unknown)
        }
        "<<" | ">>" | ">>>" => {
            if numeric(lhs).is_some() && numeric(rhs).is_some() { lhs.to_string() } else { unknown() }
        }
        _ => unknown(),
    }
}

/// The return type of a function type name like `(int, int*)void`.
pub fn return_type(function_type: &str) -> Option<&str> {
    if !function_type.starts_with('(') {
        return None;
    }
    let mut depth = 0;
    for (i, c) in function_type.char_indices() {
        match c {
            '(' => depth += 1,
            ')' => {
                depth -= 1;
                if depth == 0 {
                    return Some(&function_type[i + 1..]);
                }
            }
            _ => {}
        }
    }
    None
}
