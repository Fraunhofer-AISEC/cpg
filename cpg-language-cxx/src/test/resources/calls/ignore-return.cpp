int main() {
    Object o1;
    someFunction(o1); // intentionally ignore the return value
}

Object someFunction(Object x) {
    return x;
}
