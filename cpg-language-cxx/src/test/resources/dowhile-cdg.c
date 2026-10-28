
void FUN_123(int param_1,long *param_2) {
    int x;
    void *_stderr;
    char *pcVar9 = "error: %s\n";
    x = 7;
    int sum = param_1 + param_2;

    do {
        if(param_1 == 5) {
            if (sum % 2 == 0) {
                printf("The sum is even: %d\n", sum);
            } else {
                printf("The sum is odd: %d\n", sum);
            }
            __fprintf_chk(_stderr,2,pcVar9,param_2);
        } else {
            x = 9;
        }
    } while(true);
}