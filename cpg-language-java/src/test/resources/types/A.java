public class A {
    // Test uniqueness
    private int x;
    private int z;

    // Test propagation of specifiers
    private final int y = 4;

    public A(int x) {
        this.x = x;
    }

    public int doSomething() {
        // Test propagation in variables
        final String s = "";
        int[] array = new int[5];
        C<D, E> map;
        return x;
    }
}
