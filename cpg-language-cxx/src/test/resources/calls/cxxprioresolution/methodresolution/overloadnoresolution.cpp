#include <iostream>
using namespace std;
class Base
{
public:
    void calc(int i)
    {
        cout << "int: " << i;
    }
};
class Overloaded : public Base
{
public:
    void calc(int a, int b)
    {
        cout << "double: " << a+b;
    }
};
int main()
{
    Overloaded overload;
    cout << overload.calc(1) << '\n';
    return 0;
}