#include <iostream>
#include <string>

template<class Type1, class Type2, int N>
class Pair
{
    public:
        Type1 first;
        Type2 second;
        int n = N;
};

int main()
{
    Pair<int,int, 3> point1;

    point1.first = 10;
    point1.second = 20;
}