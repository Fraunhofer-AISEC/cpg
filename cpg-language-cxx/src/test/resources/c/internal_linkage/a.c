// `secret` has internal linkage (file-scope `static`), so it is private to this translation unit.
// The identically-named `secret` in b.c must remain invisible from here.
static int secret = 1;

int readSecretA() { return secret; }

// `shared` has external linkage and can therefore be called from other translation units.
int shared() { return secret; }
