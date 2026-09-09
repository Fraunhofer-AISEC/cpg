#include <stdlib.h>

/* Fixed-size stack buffer — size lives in the ArrayConstruction's dimension. */
void fixed_buffer(void) {
    char buf[8];
}

/* Heap allocation with a constant size — malloc-with-constant shortcut. */
void heap_alloc(void) {
    char *p = malloc(64);
}

/* String literal — size is the length of the underlying string. */
void string_literal(void) {
    char *s = "hello";
}

/* Allocation size depends on a runtime branch: the size of `buf` is the
 * join of the two branches, [16, 64]. */
void bounded_alloc(int small) {
    char *buf;
    if (small) {
        buf = malloc(16);
    } else {
        buf = malloc(64);
    }
}
