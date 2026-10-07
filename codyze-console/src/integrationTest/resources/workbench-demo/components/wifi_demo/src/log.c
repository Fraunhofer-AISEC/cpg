#include "../include/log.h"

/* printf is not declared anywhere: the analysis does not know the code behind it (external) */
void log_debug(const char *msg) {
    printf("[debug] %s\n", msg);
}

void log_error(const char *msg) {
    printf("[error] %s\n", msg);
}
