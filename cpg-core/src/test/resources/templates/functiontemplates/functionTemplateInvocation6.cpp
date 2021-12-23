#include <iostream>
using namespace std;

template <class T=int, int N=5>
T fixed_multiply (T val)
{
  auto x = val * N;
  return x;
}

int main() {
  std::cout << fixed_multiply<int>(20.3) << '\n';
}