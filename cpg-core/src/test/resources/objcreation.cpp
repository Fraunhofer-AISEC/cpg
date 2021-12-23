#include <iostream>

int method();

class Integer {
private:
  int i;

public:
  Integer(int i) {
    this->i = i;
  }

  int method();

  int getI() {
    return i;
  }

};

int method() {
  return 2;
}

int main() {
  Integer i(4);
  i.getI();

  std::string s("hallo");
  s.c_str();

  Integer j(method());

  int k = 4;

  auto l = new Integer(k);
}
