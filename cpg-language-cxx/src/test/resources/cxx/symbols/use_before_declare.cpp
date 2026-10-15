int x = 100;

int f() {
    // At this point in the function, the block-scoped "x" declared below is not yet in scope
    // (its scope begins at its own point of declaration, not at the start of the enclosing
    // block), so this must resolve to the global "x".
    int early = x;
    int x = 5;
    // Here, the local "x" is in scope and shadows the global one.
    int late = x;
    return early + late;
}
