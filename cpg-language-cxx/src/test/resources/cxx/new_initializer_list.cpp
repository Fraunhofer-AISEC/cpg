template<typename T>
class Foo {
public:
    T value;
};

void test() {
    // This triggers an InitializerList (brace-init) for a template new expression.
    // Previously this caused a ClassCastException in handleNew.
    auto* f = new Foo<int>{42};
}
