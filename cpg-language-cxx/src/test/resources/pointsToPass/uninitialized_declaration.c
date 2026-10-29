void use(int v);

// Uninitialized on the path where the condition is false.
void maybe(int c) {
  int x;
  if (c) {
    x = 1;
  }
  use(x);
}

// Uninitialized on every path.
void always(void) {
  int y;
  use(y);
}

// Initialized on both paths.
void both(int c) {
  int z;
  if (c) {
    z = 1;
  } else {
    z = 2;
  }
  use(z);
}

// A full write removes the unknown value again.
void overwritten(void) {
  int u;
  u = 3;
  use(u);
}

// Initialized at the declaration.
void declared(int c) {
  int w = 0;
  if (c) {
    w = 1;
  }
  use(w);
}
