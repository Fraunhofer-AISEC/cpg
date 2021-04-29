void f(int, int);
void f(int, int = 7);
void h() {
    f(3); // OK calls f(3,7);
}
void m() {
    void f(int, int);
    f(8); // Error
    void f(int, int = 5);
    f(4); // OK calls f(4,5);
}
void n() {
    f(6); // OK call f(6,7);
}