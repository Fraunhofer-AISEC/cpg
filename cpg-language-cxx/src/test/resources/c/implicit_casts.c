void f(int x) {}

// g simulates a compile error
void g(int x) {}
void g(float x) {}

int main() {
   char x = 'a';
   f(x);

   g(x);
}
