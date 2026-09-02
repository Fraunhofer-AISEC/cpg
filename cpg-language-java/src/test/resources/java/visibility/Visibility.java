package visibility;

public class Visibility {

    public int publicField;
    protected int protectedField;
    private int privateField;
    int packageField;

    public static int publicStaticField;
    static int packageStaticField;

    public Visibility() {}

    Visibility(int i) {
        this.packageField = i;
    }

    public void publicMethod() {}

    protected void protectedMethod() {}

    private void privateMethod() {}

    void packageMethod() {}

    public static void publicStaticMethod() {}

    static void packageStaticMethod() {}

    public class PublicInner {}

    protected class ProtectedInner {}

    private class PrivateInner {}

    class PackageInner {}

    public enum PublicEnum {
        A,
        B
    }

    enum PackageEnum {
        C,
        D
    }
}

class PackagePrivateTopLevel {

    public int somePublicField;

    int somePackageField;
}
