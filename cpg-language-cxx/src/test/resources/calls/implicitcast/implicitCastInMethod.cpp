#include <iostream>
using namespace std;

class DemoClass {
  private:
  int calc(int a) {
    return a+3;
  }
  public:
    void doSmth(int x) {
      cout << x+calc(2.0) << "\n";
    }
};

int main() {
  DemoClass demoClass;
  demoClass.doSmth(10.0);
  return 0;
}