#include <stdio.h>

class Simple {

  public:
    void foo(int x) {
                        int j = 7;
                        if (x < 42) {
                          j = x;
                        }
                        printf(j);
                    }
};


/*int main () {
  Simple s;
  s.foo(1);
  return 0;
}*/
