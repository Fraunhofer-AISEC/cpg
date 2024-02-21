#include <stdio.h>

struct tls_context {
  int i;
  int j;
};

void inner_renegotiate(struct tls_context *c) {
  c->j = 4;
}

void renegotiate(struct tls_context *ctx) {
  ctx->i = 3;

  inner_renegotiate(ctx);
}

int main() {
  // let's set up our context here
  struct tls_context ctx;
  ctx.i = 1;
  ctx.j = 2;

  // print context before
  printf("ctx.i: %d, ctx.j: %d\n", ctx.i, ctx.j);

  // let's simulate a TLS re-negotiation
  renegotiate(&ctx);

  // print context afterwards
  printf("ctx.i: %d, ctx.j: %d\n", ctx.i, ctx.j);
}