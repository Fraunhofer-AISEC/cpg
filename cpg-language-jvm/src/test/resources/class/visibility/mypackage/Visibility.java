package mypackage;

/**
 * A fixture exercising every JVM access-flag combination the frontend maps onto the canonical
 * visibility model. The class itself is {@code public}; the nested {@link PackagePrivate} class has
 * no access flag and is therefore package-private.
 */
public class Visibility {

    public int publicField;
    protected int protectedField;
    private int privateField;
    int packageField;
    public static int staticField;

    public void publicMethod() {}

    protected void protectedMethod() {}

    private void privateMethod() {}

    void packageMethod() {}

    public static void staticMethod() {}
}

class PackagePrivate {
    int value;
}
