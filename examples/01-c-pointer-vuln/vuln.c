/*
 * Intentional security vulnerabilities for the CPG live demo.
 * Do NOT compile and ship — every function below is buggy on purpose.
 */
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

/* CWE-416 — use-after-free in the simplest possible form. */
void uaf_simple(void) {
    char *p = malloc(64);
    strcpy(p, "secret");
    free(p);
    printf("%s\n", p);          /* BUG: p was just freed */
}

/* CWE-416 — use-after-free hidden behind a pointer alias.
 * The CPG's points-to analysis sees that q aliases p, so dereferencing q
 * after free(p) is still a UAF. */
void uaf_aliased(void) {
    char *p = malloc(64);
    char *q = p;                /* q now aliases p */
    strcpy(p, "secret");
    free(p);
    printf("%s\n", q);          /* BUG: same buffer, different name */
}

/* CWE-415 — double free. */
void double_free(void) {
    char *p = malloc(64);
    free(p);
    free(p);                    /* BUG: second free */
}

/* CWE-120 — stack buffer overflow via strcpy.
 * The CPG knows `buf` is 8 bytes; if the abstract-interval evaluator
 * can put a bound on the length of `user_input`, it can decide whether
 * the strcpy is safe. */
void buffer_overflow(const char *user_input) {
    char buf[8];
    strcpy(buf, user_input);    /* BUG: no length check */
    printf("%s\n", buf);
}

/* CWE-401 — memory leak. The CPG can find allocations whose pointer
 * never reaches a free along any execution path. */
void leak(void) {
    char *p = malloc(64);
    strcpy(p, "leaked");
    /* p never freed before returning */
}

/* A function that does the right thing — useful as a control:
 * the same queries should NOT flag this one. */
void safe(void) {
    char *p = malloc(64);
    strcpy(p, "ok");
    printf("%s\n", p);
    free(p);
}

/* The allocation size depends on a runtime branch, so the abstract
 * interval analyzer can only narrow `buf`'s size to the union of both
 * branches: [16, 64]. The strcpy of a 5-byte literal ("hello") is
 * still provably safe because 5 ≤ 16 (the worst-case dest size). */
void bounded_alloc(int small) {
    char *buf;
    if (small) {
        buf = malloc(16);
    } else {
        buf = malloc(64);
    }
    strcpy(buf, "hello");        /* OK: fits even the smaller branch */
    free(buf);
}

int main(int argc, char **argv) {
    uaf_simple();
    uaf_aliased();
    double_free();
    if (argc > 1) buffer_overflow(argv[1]);
    leak();
    safe();
    bounded_alloc(argc > 2);
    return 0;
}
