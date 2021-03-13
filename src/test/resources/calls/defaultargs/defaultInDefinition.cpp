#include <iostream>
using namespace std;

// defining the default arguments
void display(char c = '*', int count = 3) {
    for(int i = 1; i <= count; ++i) {
        cout << c;
    }
    cout << endl;
}

int main() {
    int count = 5;

    // *, 3 will be parameters
    display();

    // #, 3 will be parameters
    display('#');

    // $, 5 will be parameters
    display('$', count);

    return 0;
}