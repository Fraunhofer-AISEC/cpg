struct node {
  struct node *next;
  int key;
  int value;
};

struct table {
  int num_buckets;
  struct node *buckets[8];
};

// The pointer pp is set to the address of a field of the object it currently points to, so it
// reaches a new field address in every iteration of the inner loop.
int lookup(struct table *t, int key, int *out) {
  int found = 0;
  struct node *hit = 0;

  if (out == 0) {
    return -1;
  }
  for (int i = 0; i < t->num_buckets; i++) {
    struct node **pp = &t->buckets[i];
    while (*pp != 0) {
      if ((*pp)->key == key) {
        found = 1;
        hit = *pp;
        break;
      }
      pp = &(*pp)->next;
    }
  }
  if (found) {
    *out = hit->value;
    return 0;
  }
  return 1;
}
