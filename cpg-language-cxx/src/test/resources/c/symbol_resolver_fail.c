// this macro contains two literals. Because this is a macro, both
// literals will contain the same location and thus, the same hash-code.
// this confuses the EOG walker
#define SOME_MACRO (1u<<1)

int symbol_resolver_fail()
{
  char *address;
  if (SOME_MACRO) {
    do_call(address);
  }
}
