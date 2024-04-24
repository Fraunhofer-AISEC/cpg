class MyClass {
    int i;
    int j;

public:
    MyClass(int i) {
        this->i = i;
        this->j = 5;
    }

    MyClass(int i, int j) {
        this->i = i;
        this->j = j;
    }
};

typedef long long myint64;

int function(float f) {
    // it seems that functional style calls with primitives are also parsed as simple type constructors (not sure exactly why)
    int i = int(f + 0.5f);

    // this is a functional style call with a "custom" type, this is recognized as a call expression and will be replaced in a pass later
    int j = i == 1 ? myint64(i) : 0;

    // this on the other hand is a regular constructor initializer as part of the variable declaration
    int k(5);

    // now things get interesting, since this is also a simple type constructor, but in this case this would actually construct an object
    MyClass c = MyClass{1, 2};
    c = MyClass{1};

    return k;
}
