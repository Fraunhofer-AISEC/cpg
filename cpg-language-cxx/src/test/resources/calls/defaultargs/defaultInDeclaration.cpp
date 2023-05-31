#include <iostream>
using namespace std;

// defining the default arguments
void display(int = 3, char = '*');

int main() {
    int count = 5;

    // 3, * will be parameters
    display();

    // 1, * will be parameters
    display(1);

    // 10, * will be parameters (implicit cast)
    display(10.0);

    // 5, $ will be parameters
    display(count, '$');

    return 0;
}

void display(int count, char c) {
    for(int i = 1; i <= count; ++i)
    {
        cout << c;
    }
}