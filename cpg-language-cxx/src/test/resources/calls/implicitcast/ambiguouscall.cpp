#include <iostream>
using namespace std;

int ambiguous_multiply (int val)
{
  return val * 3;
}

int ambiguous_multiply (float val) {
    return val * 5;
}

int main() {
  std::cout << ambiguous_multiply(10.0) << '\n';
}