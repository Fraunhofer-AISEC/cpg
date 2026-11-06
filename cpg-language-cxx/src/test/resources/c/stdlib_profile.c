// Neither the types nor memcpy are declared, as the standard headers are not available
void copy(char *dst, char *src, size_t n) {
    memcpy(dst, src, n);
}

long next(size_t size) {
    return size + 1;
}
