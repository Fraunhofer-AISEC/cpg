// MyTemplateClass is a template class with a copy and a move constructor
template <typename T>
class MyTemplateClass {
public:
    MyTemplateClass(const T &t) {
        // do something here, probably copy from T
    }
    MyTemplateClass(T &&t) {
        // do something here, probably move from T
    }
};

// MyClass is a regular class with a copy and a move constructor
class MyClass {
public:
    MyClass(const MyClass &m) {}
    MyClass(MyClass &&m) {}
};
