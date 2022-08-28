int main() {
    char* c;
    if(5 > 4)
        c = new char[4];
    else
        c = new char[5];
    int a = 0;
    for(int i = 0; i <= 4; i++) {
        a = a + c[i];
    }
    return a;
}
