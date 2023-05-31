#include <iostream>
using namespace std;
class Base
{
public:
    int calc(int i)
    {
        cout << "int: ";
        return i+6;
    }
};
class Overload : public Base
{
public:
    double calc(double d)
    {
        cout << "double: ";
        return d+9.9;
    }
};
int main()
{
    Overload overloaded;
    cout << overloaded.calc(1) << '\n';
    cout << overloaded.calc(1.1) << '\n';
    return 0;
}