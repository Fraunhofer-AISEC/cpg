void takesVoidPtr(void* ptr) {}

int main() {
    int x;
    int* intPtr = &x;
    takesVoidPtr(intPtr);

    double d;
    double* doublePtr = &d;
    takesVoidPtr(doublePtr);

    int** intPtrPtr = &intPtr;
    takesVoidPtr(intPtrPtr);

    double** doublePtrPtr = &doublePtr;
    takesVoidPtr(doublePtrPtr);
}