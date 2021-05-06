class A {
public:
    A() {}
    A(int x) {}
    A(int x, int y) {}
};

int main() {
   A a1;
   A a2(5);
   A a3(5,6);
   A a4 = A();
   A a5 = A(5);
   A a6 = A(5, 6);
   A* a7 = new A;
   A* a8 = new A();
   A* a9 = new A(5);
   A* a10 = new A(5, 6);
   return 0;
}