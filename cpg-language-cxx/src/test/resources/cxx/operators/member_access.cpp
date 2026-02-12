class Data {
public:
    int size;
};

class Proxy {
public:
    Proxy() {
        this->data = new Data();
    }

    Data* operator->() {
        return data;
    }

    Data* data;
};

int main() {
    Proxy p;
    int size = p->size;

    // int another_size = p.operator->()->size;
}
