#include<stdio.h>

void f(char * data)
{
    printf("%d", data[0]);
}

void main()
{
    char * data;
    data = NULL;
    f(data);
}

