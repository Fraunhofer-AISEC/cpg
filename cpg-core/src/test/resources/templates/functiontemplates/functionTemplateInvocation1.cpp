#include <iostream>
using namespace std;

template <class T=int, int N=5>
T fixed_multiply (T val)
{
  auto x = val * N;
  return x;
}

double fixed_multiply(double val) {
	return val*100;
}

int main() {
  std::cout << fixed_multiply(10.0) << '\n';
}