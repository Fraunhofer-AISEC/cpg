void foo(int i) {
}

struct S {
    int a;
} typedef s_t;

typedef s_t* s_t_p;

int main() {
    void (*ptr)(int) = &foo;

    // this is a function call
    (*ptr)(1);
    (ptr)(2);

    // this is a type case
    (int)(3);
    (s_t_p)(4);

    return 0;
}
