#include <stdlib.h>

void malloc_constant(void) {
    char *p = malloc(64);
}

void malloc_via_assign(void) {
    char *p;
    p = malloc(128);
}

void calloc_constant(void) {
    int *p = calloc(8, sizeof(int));
}

void realloc_constant(void) {
    char *p = malloc(16);
    p = realloc(p, 32);
}

void malloc_unknown_size(int n) {
    char *p = malloc(n);
}

void non_allocator_call(void) {
    (void)rand();
}
