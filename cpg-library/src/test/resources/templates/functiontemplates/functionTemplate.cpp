#include <iostream>
using namespace std;

template <class T=int, int N=5>
T fixed_multiply (T val)
{
  auto x = val * N;
  return x;
}

int main() {
  std::cout << fixed_multiply<int,2>(10) << '\n';
  std::cout << fixed_multiply<float,3>((float) 10.0) << '\n';
}