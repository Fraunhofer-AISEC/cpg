struct Data {
    int foo() {
        return 1;
    }
};

struct Proxy {
    Data *data;
    Proxy() {
        data = new Data;
    }
    Data* operator->() {
        return data;
    }
    int bar() {
        return 1;
    }
};

int main() {
    Proxy p;

    int i = p->foo();
    int j = p.bar();
    return 1;
}

