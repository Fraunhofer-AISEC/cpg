// global declarations - not supported yet
int a = 1;

// function declarations
int main() {
    // a, b are visible in main
    int b = 2;
    int a = 3;

    if(a > b) {
        // c is visible in then-branch
        int c = 4;
        return a + c;
    } else {
        // c is not visible in else-branch
        return b;
    }
}
