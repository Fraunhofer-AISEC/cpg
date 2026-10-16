/-
Copyright (c) 2026, Fraunhofer AISEC. All rights reserved.
Licensed under the Apache License, Version 2.0.
-/
import CpgVerified.Wire.Codec

/-!
# Entry point for the native library

`translateBytes` is exported with a C symbol so that it can be called from the JVM (see
`native/cpg_verified.c`): it takes a sequence of translation requests and returns one translated
CPG expression (or error) per request, each followed by a newline.
-/

namespace Wire

/-- Translates all requests in `input`. -/
@[export cpg_verified_translate]
def translateBytes (input : ByteArray) : ByteArray :=
  match parseAll input with
  | .error msg => ((Sexp.list [.atom "error", .atom msg]).encode ++ "\n").toUTF8
  | .ok records => (String.join (records.map fun r => (handleRecord r).encode ++ "\n")).toUTF8

end Wire
