class SomeDataClass {
public:
    int doOperation();
};

class MyClass {
private:
    // This typedef is FQN'd to MyClass::Data, but it cannot be accessed
    // outside of the class because it is declared as private (however we
    // are not yet using visibility information).
    typedef SomeDataClass Data;

    Data *d;

public:
    MyClass() {
        d = new Data();
    }

    int myFunction() {
        return d->doOperation();
    }
};