#!/usr/bin/env bash
# Builds the native library (libcpgverified.dylib/.so) that the JVM loads via JNA. It statically
# links the compiled Lean code and the parts of the Lean runtime it needs, so that the library does
# not depend on a Lean installation at runtime.
set -euo pipefail

cd "$(dirname "$0")/.."

lake build CpgVerified:static

LEAN_PREFIX="$(lean --print-prefix)"
LEAN_LIB="$LEAN_PREFIX/lib/lean"
OUT=.lake/build/native
mkdir -p "$OUT"

# Platform-specific system libraries, following what Lake passes when linking executables
case "$(uname -s)" in
  Darwin)
    LIB="$OUT/libcpgverified.dylib"
    SYSTEM_LIBS=(-L "$LEAN_PREFIX/lib/libc" -lc++ -Wl,-dead_strip)
    ;;
  *)
    LIB="$OUT/libcpgverified.so"
    SYSTEM_LIBS=(
      -L "$LEAN_PREFIX/lib/glibc" -lc -lc_nonshared
      -Wl,--as-needed -l:ld.so -Wl,--no-as-needed -lpthread_nonshared
      -Wl,-Bstatic -lc++ -lc++abi -lunwind -Wl,-Bdynamic
      -Wl,--gc-sections
    )
    ;;
esac

# Compile the C interface with leanc, which knows the include paths of the Lean runtime
"$LEAN_PREFIX/bin/leanc" -c -fPIC -O2 -o "$OUT/cpg_verified.o" native/cpg_verified.c

# Link with Lean's bundled clang and sysroot, like Lake does for executables, but with the static
# archives given explicitly so that nothing refers to the shared libraries of the Lean installation.
"$LEAN_PREFIX/bin/clang" -shared -fPIC -o "$LIB" \
  --sysroot "$LEAN_PREFIX" -L "$LEAN_PREFIX/lib" -fuse-ld=lld \
  "$OUT/cpg_verified.o" \
  .lake/build/lib/libcpg_x2dverified_CpgVerified.a \
  "$LEAN_LIB/libInit.a" "$LEAN_LIB/libStd.a" "$LEAN_LIB/libleanrt.a" "$LEAN_LIB/libleancpp.a" \
  "$LEAN_PREFIX/lib/libgmp.a" "$LEAN_PREFIX/lib/libuv.a" \
  "${SYSTEM_LIBS[@]}"

echo "$LIB"
