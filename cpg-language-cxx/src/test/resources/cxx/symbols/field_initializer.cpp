int helper() {
    return 42;
}

class WithField {
public:
    int value = helper();
};
