#include <time.h>
#include <stdlib.h>

int main() {
    srand(time(NULL));
    int b = 1;
    if(rand() < 10) {
        b = b+1;
    }
    println(b); // {1, 2}

    if(rand() > 5) {
        b = b-1;
    }
    println(b); // {0, 1, 2}

    if(rand() > 3) {
        b = b*2;
    }
    println(b); // {0, 1, 2, 4}

    if(rand() < 4) {
        b = -b;
    }
    println(b); // {-4, -2, -1, 0, 1, 2, 4}

    int a = b < 2 ? 3 : 5++;
    println(a); // {3, 6}
    return 0;
}