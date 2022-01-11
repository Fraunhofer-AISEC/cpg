#include <iostream>
using namespace std;

// defining the default arguments
void add(int a, int b, int c=3, int d = 4) {
    for(int i = 1; i <= count; ++i) {
        cout << c;
    }
    cout << endl;
}

int main() {
    // Unresolved
    add();

    // OK a=1, b=2, c=3, d=4
    add(1,2);

    // OK a=1, b=2, c=5,d=6
    add(1,2,5,6);

    return 0;
}