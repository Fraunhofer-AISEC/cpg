#include <iostream>
using namespace std;

template <class T=int, int N=5>
T fixed_multiply ()
{
  return 8 * N;
}

int main() {
  std::cout << fixed_multiply<>() << '\n';
}