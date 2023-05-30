#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <time.h>
#include <inttypes.h>

struct Point
{
    int x, y, z;
};

int main()
{
    // Examples of initialization using
    // designated initialization
    struct Point p1 = {.y = 0, .z = 1, .x = 2};
    struct Point p2 = {.x = 20};
    int foo2[10] = { [3] = 1, [5] = 2 };

    return 0;
}