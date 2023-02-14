int main() {
SSL_CTX_set_verify(ctx, SSL_VERIFY_PEER, [](int preverify_ok, X509_STORE_CTX *x509_ctx) {
    return 1;
  });
}