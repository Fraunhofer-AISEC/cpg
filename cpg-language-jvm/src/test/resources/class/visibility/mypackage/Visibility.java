package mypackage;

/**
 * A fixture exercising every JVM access-flag combination the frontend maps onto the canonical
 * visibility model. The class itself is {@code public}. The separate top-level class {@link
 * PackagePrivate} (declared after this class' body, compiled to its own {@code
 * PackagePrivate.class}) has no access flag and is therefore package-private; a top-level class can
 * never be {@code private}/{@code protected}, so it exercises the "no ACC_* flag -> PACKAGE" path.
 *
 * <p>The genuinely nested {@link NestedPrivate}, {@link NestedProtected} and {@link NestedStatic}
 * classes exercise the InnerClasses path. Their real accessibility lives in this class' {@code
 * InnerClasses} attribute rather than in their own ClassFile {@code access_flags}; since SootUp only
 * reads the latter, the frontend reports them as package-private / static-less (pinned in
 * {@code JVMVisibilityTest}).
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

    private static class NestedPrivate {}

    protected static class NestedProtected {}

    public static class NestedStatic {}
}

class PackagePrivate {
    int value;
}
