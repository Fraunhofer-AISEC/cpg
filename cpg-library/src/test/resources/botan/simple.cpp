#include <memory>
#include <string>
//#include "test"
//int bar(int i) {return i;}

namespace std {
    template <class T>
    class unique_ptr {
    private:
      T number;

     public:
       SomeClass(T number) : number() {

       }
       void do_sth() {}
    };
}
//
//class Test {
//    public void do_sth() {}
//}
//
//template <class T>
//class SomeClass {
//private:
//  T number;
//
// public:
//   SomeClass(T number) : number() {
//
//   }
//   void do_sth() {}
//};
//
//int main2() {
//  SomeClass<int> c(5);
//  UnknownClass<int> d(4);
//  Test<UnknownType> e(3);
//
//  c.do_sth();
//  d.do_sth();
//  e.do_sth();
//
//  {
//    int i = 12;
//  }
//  {
//    float i = 10;
//    i++;
//  }
//  {
//    i = 41;
//  }
//}
//
//int main() {
//    Test t();
//    t.do_sth()
//    foo(bar(1));
//    return 0;
//}
//
//
