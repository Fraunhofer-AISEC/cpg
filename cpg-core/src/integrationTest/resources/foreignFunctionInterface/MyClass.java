public class MyClass {
    // Declared in native.c via JNI naming convention.
    public static native int nativeAdd(int left, int right);

    public static int useNativeAdd() {
        return nativeAdd(4, 7);
    }

    public static void main(String[] args) {
        int result = useNativeAdd();
        System.out.println("JNI result: " + result);
    }
}

