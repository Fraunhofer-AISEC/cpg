// Typedefs with the names of built-in types: the headers that define these names are not parsed,
// so the typedefs here are the only declarations, and they take precedence over the built-in types.
typedef int size_t;
typedef long int32_t;

size_t declared;
int32_t alsoDeclared;
// No typedef for this one, so it stays the built-in type
uint64_t untouched;

int call(size_t p) { return (size_t)(p); }
int plain(int v) { return (size_t) v; }
