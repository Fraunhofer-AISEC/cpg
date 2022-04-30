int global = 1;

void foo();

class Test {
public:
    void foo() {
    }
};

int main() {
    int local = global;

    foo();

    Test t;
    t.foo();

    return local;
}