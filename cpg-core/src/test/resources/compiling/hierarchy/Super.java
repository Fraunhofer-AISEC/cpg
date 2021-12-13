public class Super {

    public int superInt;

    public Super(int superInt) {
        this.superInt = superInt;
    }

    public int getSuperInt() {
        return superInt;
    }

    @Override
    public String toString() {
        return "Super[superInt=" + superInt + "]";
    }
}