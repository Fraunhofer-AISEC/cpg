void f(int x) {}

void g(char x) {}
void g(int x) {}
void g(float x) {}

class Base {};
class One : public Base {};
class Two : public One {};

void h(Base*) {};
void h(One*) {};

int main() {
   char x = 'a';
   f(x);

   // this should prefer g(char)
   g(x);

   // this should prefer h(One*)
   h(new Two());
}
