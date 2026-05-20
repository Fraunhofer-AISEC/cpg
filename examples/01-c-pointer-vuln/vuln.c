/*
 * Intentional security vulnerabilities for the CPG live demo.
 * Do NOT compile and ship — every function below is buggy on purpose.
 */
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

/* CWE-416 — use-after-free: dereferencing freed pointer via printf's %s.
 * The %s format specifier dereferences the pointer to read a string. */
void uaf_simple(void) {
    char *p = malloc(64);
    strcpy(p, "secret");
    free(p);
    printf("%s\n", p);          /* BUG: p was just freed, %s dereferences it */
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

/* CWE-416 — use-after-free with explicit dereference (write).
 * This is the classic UAF: writing to memory that was already freed. */
void uaf_deref_write(void) {
    char *p = malloc(64);
    strcpy(p, "secret");
    free(p);
    p[0] = 'X';                 /* BUG: writing to freed memory */
}

/* CWE-416 — use-after-free via strcpy destination.
 * Using freed pointer as destination corrupts the heap. */
void uaf_strcpy_dest(void) {
    char *p = malloc(64);
    strcpy(p, "secret");
    free(p);
    strcpy(p, "overwrite");     /* BUG: writing to freed memory */
}

/* CWE-416 — use-after-free via strcpy source.
 * Reading from freed memory can leak contents of next allocation. */
void uaf_strcpy_src(void) {
    char *p = malloc(64);
    char buf[64];
    strcpy(p, "secret");
    free(p);
    strcpy(buf, p);             /* BUG: reading from freed memory */
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

/* Value evaluator demo: pointer dereference. */
void eval_pointer_deref(void) {
    int c = 0;
    int *pc = &c;
    *pc = 3;
    printf("eval_pointer_deref: c = %d\n", c);
}

/* Value evaluator demo: inter-procedural pointer modification. */
static void set3(int *p) {
    *p = 3;
}

void eval_inter_proc_pointer(void) {
    int x = 0;
    set3(&x);
    printf("eval_inter_proc_pointer: x = %d\n", x);
}

/* Value evaluator demo: chained pointer dereference (**pp). */
void eval_chained_pointer(void) {
    int c = 42;
    int *pc = &c;
    int **ppc = &pc;
    **ppc = 99;
    printf("eval_chained_pointer: c = %d\n", c);
}

/* Value evaluator demo: struct member access. */
struct Point {
    int x;
    int y;
};

/* Value evaluator demo: struct member access with pointers. */
static void set_point_x(struct Point *pp, int val) {
    pp->x = val;
}

void eval_struct_member(void) {
    struct Point p;
    p.x = 10;
    p.y = 20;
    /* Pointer to struct, then dereference */
    struct Point *pp = &p;
    (*pp).y = 25;
    /* Inter-procedural: pass pointer to struct */
    set_point_x(&p, 99);
    printf("eval_struct_member: %d %d\n", p.x, p.y);
}

/* Value evaluator demo: struct via pointer. */
void eval_struct_pointer(void) {
    struct Point p;
    struct Point *pp = &p;
    pp->x = 30;
    pp->y = 40;
    printf("eval_struct_pointer: %d %d\n", p.x, p.y);
}

int main(int argc, char **argv) {
    uaf_simple();
    uaf_aliased();
    uaf_deref_write();
    uaf_strcpy_dest();
    uaf_strcpy_src();
    double_free();
    if (argc > 1) buffer_overflow(argv[1]);
    leak();
    safe();
    bounded_alloc(argc > 2);
    eval_pointer_deref();
    eval_inter_proc_pointer();
    eval_chained_pointer();
    eval_struct_member();
    eval_struct_pointer();
    return 0;
}
