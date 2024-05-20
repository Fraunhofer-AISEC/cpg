public class Issue1459 {
    public static final int i1 = 1;
    public static final int i2 = i1;

    public static void main(String[] args) {
        System.out.println(i2);
    }

    class Inner { // EOG predecessor: MemberExpression "i2"
        static final int i3 = i2;
    }

    class Inner2 {// EOG predecessor: FieldDeclaration "Inner$this"
        static final int i4 = FieldRecord.Inner.i3;
    }
}
