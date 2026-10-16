#ifndef NET_H
#define NET_H

int net_open(void);
int net_send(int sock, const char *buf, int len);

#endif
