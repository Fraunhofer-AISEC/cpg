// global declarations - only semi supported
int a = 1;

// function declarations
int only_variables() {
    // b, c are visible in function
    int b = 2;
    int c = 3;

    if(a > b) {
        // d is visible in then-branch
        int d = 4;
        return c + d;
    } else {
        // d is not visible in else-branch
        return c;
    }
}
