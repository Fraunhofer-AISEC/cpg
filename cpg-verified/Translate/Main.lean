/-
Copyright (c) 2026, Fraunhofer AISEC. All rights reserved.
Licensed under the Apache License, Version 2.0.
-/
import CpgVerified

/-!
# `cpg-translate`

Reads translation requests (see `Wire.Codec`) from standard input and writes one translated CPG
expression per request to standard output, one per line.
-/

partial def readAll (stream : IO.FS.Stream) (acc : ByteArray := .empty) : IO ByteArray := do
  let chunk ← stream.read 65536
  if chunk.isEmpty then return acc else readAll stream (acc ++ chunk)

def main : IO UInt32 := do
  let input ← readAll (← IO.getStdin)
  match Wire.parseAll input with
  | .error msg =>
    IO.eprintln s!"cpg-translate: {msg}"
    return 1
  | .ok records =>
    let stdout ← IO.getStdout
    for record in records do
      stdout.putStrLn (Wire.handleRecord record).encode
    return 0
