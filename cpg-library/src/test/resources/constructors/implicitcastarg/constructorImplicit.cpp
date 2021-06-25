class I {
public:
    I(int x) {}
};

class H {
public:
    H(int x, int y=10) {}
};

int main() {
    I i1(1.0);
    H h1 (2.0);
}