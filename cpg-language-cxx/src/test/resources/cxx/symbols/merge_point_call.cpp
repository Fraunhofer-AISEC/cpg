void merge_point_call(bool cond) {
    if (cond) {
        int a = 1;
    } else {
        int b = 2;
    }
    // Neither branch above returns, so this call sits right at the EOG merge point: it is
    // reached by two distinct incoming edges (one from each branch).
    undeclared_function();
}
