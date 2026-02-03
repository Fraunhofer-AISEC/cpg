// These headers are just here so that we could compile it, if we want it,
// to check for errors with clang. We will not parse them.
#include <cstddef>
#include <cstdint>

typedef int size_t;

int main() {
	// this cast could be mistaken for a call expression
	size_t count = (size_t)(42);

	// this cast could be mistaken for a binary operation
	int64_t addr = (int64_t) &count;

	// this is a binary operation, even though it has parenthesis.
	// The added parenthesis are just for more confusion
	addr = ((addr)) &count;

	// this is the same as in line 11, just with a different type
	addr = (long) &count;

    // this is a complex combination of cast and binary operation
	addr = (long) &addr + 1;

	// finally, a more complex example of unary operators and casts
	char* outptr, key;
	*(int64_t *)outptr = *(int64_t *)&key;

	return 0;
}