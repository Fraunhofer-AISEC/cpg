template<typename T>
class Foo {
public:
    T value;
};

class Bar {
public:
    int value;
};

void test() {
    // Brace-init with a template new expression (previously caused a ClassCastException).
    auto* f = new Foo<int>{42};
    // Brace-init with a non-template new expression.
    auto* g = new Bar{7};
}
