#include <stdio.h>

typedef struct inner_struct {
  int l;
} is;

struct tls_context {
  int i;
  int j;
  int k;
  is session;
};

void inner_renegotiate(struct tls_context *c) {
  c->j = 6;
  c->session.l = 7;
}

void renegotiate(struct tls_context *ctx) {
  ctx->i = 5;

  inner_renegotiate(ctx);
}

void mbedtls_ssl_read(struct tls_context *ctx) {
  ctx->i = 1;
  ctx->j = 2;
  ctx->k = 3;
  ctx->session.l = 4;
  printf("Read tls context: ctx.i: %d, ctx.j: %d, ctx.k: %d, ctx.s.l: %d\n", ctx->i, ctx->j, ctx->k, ctx->session.l);
}

void mbedtls_ssl_write(struct tls_context *ctx) {
  printf("Writing tls context: ctx.i: %d, ctx.j: %d, ctx.k: %d, ctx.s.l: %d\n", ctx->i, ctx->j, ctx->k, ctx->session.l);
}

int main() {
  // let's set up our context here
  struct tls_context ctx;
  struct tls_context* p=&ctx;
  // print context before
  mbedtls_ssl_read(&ctx);
  printf("%d %d\n", ctx, *p);
  // let's simulate a TLS re-negotiation
  renegotiate(&ctx);

  // print context afterwards
  mbedtls_ssl_write(&ctx);
}
