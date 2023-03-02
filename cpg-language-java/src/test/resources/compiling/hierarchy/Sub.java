public class Sub extends Super {

    public int subInt;

    public Sub() {
        this(123);
    }

    public Sub(int subInt) {
        super(42);
        this.subInt = subInt;
    }

    @Override
    public String toString() {
        return "Sub[superInt=" + superInt + ", subInt=" + subInt + "]";
    }
}