/-
Copyright (c) 2026, Fraunhofer AISEC. All rights reserved.
Licensed under the Apache License, Version 2.0.
-/
import CpgVerified

/-!
# `cpg-translate`

Reads translation requests (see `Wire.Codec`) from standard input and writes one translated CPG
expression per request to standard output, one per line. Does the same as the native library, as a
standalone process.
-/

partial def readAll (stream : IO.FS.Stream) (acc : ByteArray := .empty) : IO ByteArray := do
  let chunk ← stream.read 65536
  if chunk.isEmpty then return acc else readAll stream (acc ++ chunk)

def main : IO Unit := do
  let input ← readAll (← IO.getStdin)
  (← IO.getStdout).write (Wire.translateBytes input)
