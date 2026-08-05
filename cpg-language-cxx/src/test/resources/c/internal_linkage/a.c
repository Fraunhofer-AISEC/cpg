// `secret` has internal linkage (file-scope `static`), so it is private to this translation unit.
// The identically-named `secret` in b.c must remain invisible from here.
static int secret = 1;

int readSecretA() { return secret; }

// `helper` is a file-scope `static` function, so it also has internal linkage. The identically
// named `helper` in b.c is a completely distinct function; the call below must resolve to *this*
// one, not to b.c's.
static int helper() { return secret; }

int callHelperA() { return helper(); }

// `shared` has external linkage and can therefore be called from other translation units.
int shared() { return secret; }
