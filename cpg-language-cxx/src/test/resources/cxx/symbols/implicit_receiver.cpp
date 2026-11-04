class MyClass {
public:
    int value;

    int helper() {
        return value;
    }

    int caller() {
        // calls helper() and reads value without an explicit "this->"; resolving both requires
        // knowing we are currently inside MyClass.
        return helper() + value;
    }
};
