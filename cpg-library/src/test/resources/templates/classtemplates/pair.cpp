#include <iostream>
#include <string>

template<class Type1, class Type2>
class Pair
{
    public:
        Type1 first;
        Type2 second;
};

int main()
{
	// Assume as Point struct
    Pair<int,int> point1;

    // Logically same as X and Y members
    point1.first = 10;
    point1.second = 20;
}