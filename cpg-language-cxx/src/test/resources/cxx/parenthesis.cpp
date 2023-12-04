// These headers are just here so that we could compile it, if we want it,
// to check for errors with clang. We will not parse them.
#include <cstddef>
#include <cstdint>

int main() {
	// this cast could be mistaken for a call expression
	size_t count = (size_t)(42);

	// this cast could be mistaken for a binary operation
	int64_t addr = (int64_t) &count;

	// finally, a more complex example of unary operators and casts
	char* outptr, key;
	*(int64_t *)outptr = *(int64_t *)&key;

	return 0;
}