template<class K>
struct A {
    int foo(int i);
};

template<class K>
int A<K>::foo(int i) {
    return foo(i + 1);
}