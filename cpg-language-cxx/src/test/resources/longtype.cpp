void foo(long **dataPtr);

int main() {
    long value = 42;
    long *ptr = &value;
    foo(&ptr);
    return 0;
}