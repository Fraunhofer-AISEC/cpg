// first forward declaration of A
class A;

void someFunction(
        // elaborated type specifier and forward declaration of B as part of a parameter,
        // only valid in block
        class B* b1,
        // elaborated type specifier without any further declaration
        class B* b2
        ) {
    // elaborated type specifier and forward declaration of C as part of a declaration statement,
    // only valid in block
    class C* c1;
    // elaborated type specifier without any further declaration
    class C* c2;

    // usage of B
    B* b3;
}

// a redeclaration of A
class A;

// redeclaration and definition of A
class A {};

// fresh forward definition of C on global scope
class C;
