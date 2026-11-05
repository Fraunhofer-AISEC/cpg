// Neither strcpy nor strcat are declared, as the standard headers are not available
void copy(char *dst) {
    char b = 'b';
    char *p = strcpy(dst, &b);
    char viaReturn = *p;
}

void append(char *dst) {
    char b = 'b';
    *dst = 'a';
    strcat(dst, &b);
    char afterAppend = *dst;
}
