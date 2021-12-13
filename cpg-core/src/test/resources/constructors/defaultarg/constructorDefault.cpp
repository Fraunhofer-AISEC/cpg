class D {
public:
    D(int x=0, int y=1) {}
};

class E {
public:
    E(int x, int y=10) {}
};

int main() {
    D d1;
    D d2(2);
    D d3(3,4);

    E e1;
    E e2(5);
    E e3(6,7);
}