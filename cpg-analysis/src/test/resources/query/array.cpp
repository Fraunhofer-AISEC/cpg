int main() {
    char* c = new char[4];
    int a = 4;
    int b = a + 1;

    char d = c[b];
}

void some_other_function() {
    char* c = new char[100];

    return c[0];
}