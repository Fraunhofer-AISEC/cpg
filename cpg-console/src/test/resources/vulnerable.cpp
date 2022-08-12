int main() {
    char array[6] = "hello";
    memcpy(array, "Hello world", 11);
    printf(array);
    free(array);
    free(array);

    int a = 2;
    if(array == "hello") {
        a = 0;
    }
    double x = 5/a;

    int b = 2147483648;
    b = 2147483648;
}