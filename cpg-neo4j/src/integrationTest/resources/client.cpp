#include <iostream>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <openssl/ssl.h>
#include <openssl/err.h>

SSL *ssl;
std::string bad_ciphers = "MD5";

int callMeBack(int preverify_ok, X509_STORE_CTX *x509_ctx);

int connectTo(std::string ip, int test) {
  int s = socket(AF_INET, SOCK_STREAM, 0);

  if(!s) {
    printf("Error creating socket.\n");
    return -1;
  }

  std::cerr << "Connecting to " << ip << "..." << std::endl;

  struct sockaddr_in sa;
  memset(&sa, 0, sizeof(sa));
  sa.sin_family = AF_INET;
  sa.sin_addr.s_addr = inet_addr(ip.c_str());
  sa.sin_port = htons(443);
  socklen_t socklen = sizeof(sa);

  if(connect(s, (struct sockaddr*)&sa, sizeof(sa))) {
    std::cerr << "Error connecting to server." << std::endl;
    return -1;
  }

  return s;
}

void failDisableVerification(SSL_CTX* ctx) {
  SSL_CTX_set_verify(ctx, SSL_VERIFY_PEER, callMeBack);
}

void failSetInsecureCiphers(SSL_CTX* ctx) {
  char ciphers[] = "ALL:!ADH";

  SSL_CTX_set_cipher_list(ctx, ciphers);
}

void failSetInsecureCiphersLiteral(SSL_CTX* ctx) {
  SSL_CTX_set_cipher_list(ctx, "ALL:!ADH");
}

void failSetInsecureCiphersSTL(SSL_CTX* ctx) {
  std::string ciphers = "ALL:!ADH";

  SSL_CTX_set_cipher_list(ctx, ciphers.c_str());
}

void failSetInsecureCiphersGlobal(SSL_CTX* ctx) {
  SSL_CTX_set_cipher_list(ctx, bad_ciphers.c_str());
}

void failDisableVerificationLambda(SSL_CTX* ctx) {
  // lambdas do not work yet
  /*SSL_CTX_set_verify(ctx, SSL_VERIFY_PEER, [](int preverify_ok, X509_STORE_CTX *x509_ctx) {
    return 1;
  });*/
}

SSL_CTX* initTLSContext() {
  SSL_library_init();
  SSL_load_error_strings();
  SSL_CTX* ctx = SSL_CTX_new(TLSv1_2_client_method());

  // set insecure cipher
  failSetInsecureCiphers(ctx);
  failSetInsecureCiphersLiteral(ctx);
  failSetInsecureCiphersSTL(ctx);
  failSetInsecureCiphersGlobal(ctx);

  // enable verification
  SSL_CTX_set_verify(ctx, SSL_VERIFY_PEER, nullptr);

  // disable verification
  failDisableVerification(ctx);

  return ctx;
}

int main(int argc, char** argv) {
  int s = connectTo("172.217.18.99", 2);
  if(s < 0) {
    return -1;
  }

  SSL_CTX* ctx = initTLSContext();

  ssl = SSL_new(ctx);

  if(!ssl) {
    std::cerr << "Error creating SSL." << std::endl;
    return -1;
  }

  SSL_set_fd(ssl, s);

  int err = SSL_connect(ssl);
  // this one confuses neo4j ogm
  if(err <= 0) {
    int sslerr = ERR_get_error();

    std::cerr << "Error creating SSL connection. Error Code: " << ERR_error_string(sslerr, nullptr) << std::endl;
    return -1;
  }

  if(err <= 0) {
    std::cerr << "Error creating SSL connection. Error Code: " << ERR_error_string(ERR_get_error(), nullptr) << std::endl;
    return -1;
  }

  if (SSL_get_verify_result(ssl) == X509_V_OK) {
    std::cout << "Call to SSL_get_verify_result is ok" << std::endl;
  }

  std::cout << "SSL communication established using " << SSL_get_cipher(ssl) << std::endl;

  return 0;
}

int callMeBack(int preverify_ok, X509_STORE_CTX *x509_ctx) {
  return 1;
}