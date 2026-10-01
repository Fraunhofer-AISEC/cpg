struct node {
  struct node *next;
  int val;
};

struct inner {
  int a;
  int b;
};

struct outer {
  struct inner in;
};

void sink(int v);

// A value which we write before walking a list is still what we read afterwards, if the loop does
// not run at all.
int read_after_walk(struct node *p, int n, int secret) {
  p->val = secret;
  for (int i = 0; i < n; i++) {
    p = p->next;
  }
  return p->val;
}

// Field accesses which are far from the depth limit stay distinct.
void shallow(struct outer *o, int a, int b) {
  o->in.a = a;
  o->in.b = b;
  sink(o->in.a);
}
