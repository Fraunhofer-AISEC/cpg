#include "crypto.h"

int encrypt(const char *key, char *buf, int len) {
    for (int i = 0; i < len; i++) {
        buf[i] = buf[i] ^ key[i % 8];
    }
    return len;
}

void copy_plain(const char *key, char *buf, int len) {
    for (int i = 0; i < len; i++) {
        buf[i] = key[i % 8];
    }
}
