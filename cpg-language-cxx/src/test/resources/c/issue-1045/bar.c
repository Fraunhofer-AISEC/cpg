#include "foo.h"

int main(int argc, char **argv)
{
  foo(argc);         // Infers a new function.
  foo((size_t)argc); // References the right function in the graph.
}