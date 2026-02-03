#include <iostream>
using namespace std;

template<class T> void f(T x, T y) { cout << "Template" << endl; }

void f(int w, int z) { cout << "Non-template" << endl; }

int main() {
   f( 1 ,  2 ); // non-template
   f('a', 'b'); // template
   f( 1 , 'b'); // non-template
   f<>( 1 , 'b'); // error; we fall back to non-template for now
}