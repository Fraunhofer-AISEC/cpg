#include <stdio.h>

int main() {
  cout << "bla";
  cout << "blubb";
  if(true) {
    cout << "zonk";
    return 1;
  } else {
    {
      cout << "zink";
      return 2;
    }
  }
  return 0;
}
