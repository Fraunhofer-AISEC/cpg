#include <iostream>
#include <string>

template<class Type1, class Type2 = Type1, int A=1, int B=A>
struct Pair
{
    Type1 first;
    Type2 second;
};

int main()
{
    Pair<int> point1;

    point1.first = 10;
    point1.second = 20;
}