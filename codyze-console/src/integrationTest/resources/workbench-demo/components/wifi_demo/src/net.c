#include "net.h"

/* socket and send are not declared anywhere: external code */
int net_open(void) {
    int sock = socket(2, 1, 0);
    return sock;
}

int net_send(int sock, const char *buf, int len) {
    return send(sock, buf, len, 0);
}
