class BaseClass {
public:
    int size;
};

class SomeDataClass : BaseClass {
public:
    void doOperation();
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
        // should create a new SomeDataClass
        d = new Data();
    }

    int myFunction() {
        // should execute SomeDataClass::doOperation
        d->doOperation();

        // should return BaseClass::size
        return d->size;
    }
};
