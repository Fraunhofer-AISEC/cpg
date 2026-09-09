// C has no access control: `struct`/`union` members are always publicly accessible. The frontend
// must therefore not attach any restricting visibility to them, even though it records a default
// `public` specifier internally while walking the members.
struct Point {
  int x;
  int y;
};
