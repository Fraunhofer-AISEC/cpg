// A second, unrelated `secret` with internal linkage, private to this translation unit.
static int secret = 2;

int readSecretB() { return secret; }

// A second, unrelated `helper` with internal linkage, private to this translation unit. The call
// below must resolve to this `helper`, never to the identically-named one in a.c.
static int helper() { return secret; }

int callHelperB() { return helper(); }

// Calls the externally-linked `shared` defined in a.c, i.e. across translation units.
int useShared() { return shared(); }
