// fill and infer_fill have registered summaries that write to their second parameter, but the second
// call of each only passes one argument. In C, the name alone determines the function, so the calls
// still go to fill and to the infer_fill inferred for the first call.
void fill(int value, int *out);

int caller(void) {
    int x;
    fill(1, &x);
    fill(2);
    infer_fill(1, &x);
    infer_fill(2);
    return x;
}
