#include<stdio.h>

struct Point {
  int x;
  int y;
};

static void set_point_x(struct Point *pp, int val) {
    pp->x = val;
}

int main(void) {
    struct Point p;
    p.x = 10;
    p.y = 20;
    printf("eval_struct_member: %d %d\n", p.x, p.y);
    /* Pointer to struct, then dereference */
    struct Point *pp = &p;
    (*pp).y = 25;
    /* Inter-procedural: pass pointer to struct */
    set_point_x(&p, 99);
    printf("eval_struct_member: %d %d\n", p.x, p.y);
}
