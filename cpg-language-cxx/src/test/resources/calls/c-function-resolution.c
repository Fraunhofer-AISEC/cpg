//#define MY_CONST_INT 1

void foo(int i) {
}
void bar(int i) {
}

int main() {
    foo(MY_CONST_INT);
    foo(1);
    bar(1,2);
    return 0;
}

