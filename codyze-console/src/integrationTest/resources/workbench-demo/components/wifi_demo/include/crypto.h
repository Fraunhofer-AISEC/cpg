#ifndef CRYPTO_H
#define CRYPTO_H

/* Encrypts buf in place with the key */
int encrypt(const char *key, char *buf, int len);

/* Copies the key into buf, without any protection */
void copy_plain(const char *key, char *buf, int len);

#endif
