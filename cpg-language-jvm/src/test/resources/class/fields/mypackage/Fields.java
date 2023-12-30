package mypackage;

public class Fields {

    private int a = 2;

    Fields() {
        resetA();
    }

    public void setA(int a) {
        this.a = a;
    }

    private void resetA() {
        setA(10);
    }
}