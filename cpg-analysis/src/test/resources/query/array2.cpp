int main() {
    char* c = new char[4];
    int a = 0;
    for(int i = 0; i <= 4; i++) {
        a = a + c[i];
    }
    return a;
}
