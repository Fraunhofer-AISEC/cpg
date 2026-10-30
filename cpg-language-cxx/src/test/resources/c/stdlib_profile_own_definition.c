// strlen is defined by the code itself (with a prototype, as a header would have it); memcpy is
// neither declared nor defined.
size_t strlen(const char *s);

size_t strlen(const char *s) {
    size_t n = 0;
    while (s[n]) n++;
    return n;
}

size_t copyAndCount(char *dst, char *src, size_t n) {
    memcpy(dst, src, n);
    return strlen(dst);
}
