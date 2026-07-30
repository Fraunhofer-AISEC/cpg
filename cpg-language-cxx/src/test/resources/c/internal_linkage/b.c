// A second, unrelated `secret` with internal linkage, private to this translation unit.
static int secret = 2;

int readSecretB() { return secret; }

// Calls the externally-linked `shared` defined in a.c, i.e. across translation units.
int useShared() { return shared(); }
