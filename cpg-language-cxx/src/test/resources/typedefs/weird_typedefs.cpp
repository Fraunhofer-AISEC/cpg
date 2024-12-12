// typedef can be used anywhere in the decl-specifier-seq
// more conventionally spelled "typedef unsigned long long int ullong;"
unsigned long typedef long int ullong;

// usage of type that is identical to typedef
unsigned long long int someUllong1;
// usage of typedef
ullong someUllong2;

// also possible with structs
struct bar {
  int a;
  int b;
} typedef baz;

// just some type that contains a typedef for more confusion
struct foo {
  typedef const int a;
};