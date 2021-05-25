#include <iostream>
using namespace std;

class MyClass {
  public:
    template <class T=int, int N=5>
    T fixed_multiply (T val)
    {
      auto x = val * N;
      return x;
    }
};

int main() {
  MyClass myObj;
  cout << myObj.fixed_multiply<int>(3) << endl;
  return 0;
}