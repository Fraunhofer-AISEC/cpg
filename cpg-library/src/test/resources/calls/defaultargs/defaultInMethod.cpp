#include <iostream>
using namespace std;

class DemoClass {
  private:
  int calc(int a, int b=5) {
    return a+b;
  }
  public:
    void doSmth(int x=1, int y=2) {
      cout << calc(x)+y << "\n";
    }
};

int main() {
  DemoClass demoClass;
  demoClass.doSmth();
  return 0;
}