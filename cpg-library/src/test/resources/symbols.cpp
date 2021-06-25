#include <iostream>

// just to explain the macros, they will not be defined here but in the configuration of the translation manager
//#define HELLO_WORLD "Hello World"
//#define INCREASE(X) X+1

int main() {
    std::cout << HELLO_WORLD;
    std::cout << INCREASE(2);
}