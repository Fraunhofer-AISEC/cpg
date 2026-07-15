#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <math.h>

int main(void) {
    char *buf = malloc(strlen("hi") + 1);
    strcpy(buf, "hi");
    printf("%s %d\n", buf, (int) sqrt(4.0));
    free(buf);
    return 0;
}
