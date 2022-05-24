int main() {
    char array[6] = "hello";
    memcpy(array, "Hello world", 11);
    printf(array);
    free(array);
    free(array);
}