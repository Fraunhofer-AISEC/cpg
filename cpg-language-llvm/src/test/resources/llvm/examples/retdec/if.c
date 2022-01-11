#include <stdlib.h>
#include <stdio.h>

int main() {
    int a = rand();
    int b;

    if(a == 1) {
        printf("it was 1!\n");
        b = 10;
    } else {
        printf("it was something else");
        b = 20;
    }

    return b;
}